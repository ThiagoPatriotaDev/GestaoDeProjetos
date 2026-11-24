package gestaodeprojetos.alunos.cloud

import android.content.Context
import android.util.Log
import gestaodeprojetos.alunos.db.AppDatabase
import gestaodeprojetos.alunos.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

sealed class CloudSyncOutcome {
    data class Synced(val project: Project) : CloudSyncOutcome()
    data class NotConfigured(val project: Project) : CloudSyncOutcome()
    data class Error(val project: Project, val message: String) : CloudSyncOutcome()
}

data class FullSyncResult(
    val uploaded: Int = 0,
    val restored: Int = 0,
    val configured: Boolean = true,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = configured && errorMessage == null
}

class SupabaseBackupRepository(private val context: Context) {

    companion object {
        private const val TAG = "SupabaseBackup"
    }

    private val projectDao by lazy { AppDatabase.getDatabase(context).projectDao() }

    fun getSyncCode(): String = SyncCodeManager.getSyncCode(context)

    fun setSyncCode(newCode: String): String = SyncCodeManager.setSyncCode(context, newCode)

    suspend fun createAndBackup(project: Project): CloudSyncOutcome = withContext(Dispatchers.IO) {
        val localId = projectDao.insert(project.copy(isSynced = false))
        val savedLocal = project.copy(id = localId, isSynced = false)

        if (!SupabaseConfig.isConfigured()) {
            return@withContext CloudSyncOutcome.NotConfigured(savedLocal)
        }

        pushProjectToCloud(savedLocal)
    }

    suspend fun updateAndBackup(project: Project): CloudSyncOutcome = withContext(Dispatchers.IO) {
        val pendingLocal = project.copy(isSynced = false)
        projectDao.update(pendingLocal)

        if (!SupabaseConfig.isConfigured()) {
            return@withContext CloudSyncOutcome.NotConfigured(pendingLocal)
        }

        pushProjectToCloud(pendingLocal)
    }

    suspend fun syncAll(): FullSyncResult = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext FullSyncResult(
                configured = false,
                errorMessage = "Configure SUPABASE_URL e SUPABASE_ANON_KEY em SupabaseConfig.kt."
            )
        }

        var uploaded = 0
        var restored = 0
        val syncCode = getSyncCode()
        val apiKey = SupabaseConfig.SUPABASE_ANON_KEY.trim()
        val bearer = SupabaseConfig.authorizationHeaderOrNull()

        try {
            // 1. Envia projetos locais ainda não sincronizados
            val localProjects = projectDao.getAllSnapshot()
            for (local in localProjects.filter { !it.isSynced }) {
                when (val outcome = pushProjectToCloud(local)) {
                    is CloudSyncOutcome.Synced -> uploaded++
                    is CloudSyncOutcome.Error -> {
                        return@withContext FullSyncResult(
                            uploaded = uploaded,
                            restored = restored,
                            errorMessage = outcome.message
                        )
                    }
                    is CloudSyncOutcome.NotConfigured -> {
                        return@withContext FullSyncResult(configured = false)
                    }
                }
            }

            // 2. Baixa e restaura projetos da nuvem para este código de sincronização
            val response = SupabaseApiClient.service.getProjects(
                apiKey = apiKey,
                bearerToken = bearer,
                syncCodeHeader = syncCode,
                syncCodeFilter = "eq.$syncCode"
            )

            if (!response.isSuccessful) {
                val errBody = response.errorBody()?.string().orEmpty()
                Log.e(TAG, "Erro ao buscar backup no Supabase (HTTP ${response.code()}): $errBody")
                return@withContext FullSyncResult(
                    uploaded = uploaded,
                    restored = restored,
                    errorMessage = mapHttpError(response.code())
                )
            }

            val remoteProjects = response.body().orEmpty()
            for (dto in remoteProjects) {
                val existing = projectDao.getByRemoteId(dto.id)
                if (existing == null) {
                    projectDao.insert(dto.toLocalProject())
                    restored++
                } else if (existing.isSynced) {
                    val refreshed = dto.toLocalProject(existingLocalId = existing.id)
                    if (refreshed != existing) {
                        projectDao.update(refreshed)
                    }
                }
            }

            FullSyncResult(uploaded = uploaded, restored = restored)
        } catch (e: IOException) {
            Log.e(TAG, "Sem conexão durante sincronização com Supabase", e)
            FullSyncResult(
                uploaded = uploaded,
                restored = restored,
                errorMessage = "Sem conexão com a internet. Seus projetos continuam salvos localmente."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado na sincronização com Supabase", e)
            FullSyncResult(
                uploaded = uploaded,
                restored = restored,
                errorMessage = e.localizedMessage ?: "Erro inesperado ao sincronizar com o Supabase."
            )
        }
    }

    private suspend fun pushProjectToCloud(project: Project): CloudSyncOutcome {
        val syncCode = getSyncCode()
        val apiKey = SupabaseConfig.SUPABASE_ANON_KEY.trim()
        val bearer = SupabaseConfig.authorizationHeaderOrNull()
        val payload = project.toSupabasePayload(syncCode)

        return try {
            val existingRemoteId = project.remoteId
            val response = if (!existingRemoteId.isNullOrBlank()) {
                val patchResponse = SupabaseApiClient.service.updateProject(
                    apiKey = apiKey,
                    bearerToken = bearer,
                    syncCodeHeader = syncCode,
                    idFilter = "eq.$existingRemoteId",
                    payload = payload
                )
                if (patchResponse.isSuccessful && patchResponse.body().isNullOrEmpty()) {
                    // Se o registro não existir mais na nuvem (ou mudou de código), cria novamente
                    SupabaseApiClient.service.createProject(
                        apiKey = apiKey,
                        bearerToken = bearer,
                        syncCodeHeader = syncCode,
                        payload = payload
                    )
                } else {
                    patchResponse
                }
            } else {
                SupabaseApiClient.service.createProject(
                    apiKey = apiKey,
                    bearerToken = bearer,
                    syncCodeHeader = syncCode,
                    payload = payload
                )
            }

            if (response.isSuccessful) {
                val remoteDto = response.body()?.firstOrNull()
                val syncedProject = project.copy(
                    remoteId = remoteDto?.id ?: project.remoteId,
                    isSynced = true
                )
                projectDao.update(syncedProject)
                Log.i(TAG, "Backup sincronizado no Supabase! remoteId=${syncedProject.remoteId}")
                CloudSyncOutcome.Synced(syncedProject)
            } else {
                val errBody = response.errorBody()?.string().orEmpty()
                Log.e(TAG, "Erro HTTP ${response.code()} ao enviar para Supabase: $errBody")
                CloudSyncOutcome.Error(project, mapHttpError(response.code()))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Falha de rede ao enviar projeto ao Supabase", e)
            CloudSyncOutcome.Error(
                project,
                "Sem conexão com a internet. Projeto salvo localmente para sincronizar depois."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado ao enviar projeto ao Supabase", e)
            CloudSyncOutcome.Error(
                project,
                e.localizedMessage ?: "Erro ao enviar backup para a nuvem."
            )
        }
    }

    private fun mapHttpError(code: Int): String {
        return when (code) {
            401 -> "Chave do Supabase inválida (HTTP 401). Verifique SUPABASE_ANON_KEY."
            403 -> "Permissão negada pelo Supabase (HTTP 403). Verifique as políticas RLS da tabela projects."
            404 -> "Tabela 'projects' não encontrada no Supabase (HTTP 404). Execute o SQL de criação no painel."
            else -> "Erro no Web Service Supabase (HTTP $code). Consulte o Logcat (tag SupabaseApi)."
        }
    }
}
