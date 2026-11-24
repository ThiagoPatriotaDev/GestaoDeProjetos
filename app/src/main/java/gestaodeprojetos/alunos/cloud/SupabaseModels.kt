package gestaodeprojetos.alunos.cloud

import com.google.gson.annotations.SerializedName
import gestaodeprojetos.alunos.model.Project

data class SupabaseProjectPayload(
    @SerializedName("sync_code")
    val syncCode: String,
    @SerializedName("nome")
    val nome: String,
    @SerializedName("disciplina")
    val disciplina: String,
    @SerializedName("data_inicio")
    val dataInicio: String,
    @SerializedName("data_entrega")
    val dataEntrega: String,
    @SerializedName("dia_estudo")
    val diaEstudo: String,
    @SerializedName("horario_inicio")
    val horarioInicio: String,
    @SerializedName("horario_termino")
    val horarioTermino: String,
    @SerializedName("observacoes")
    val observacoes: String,
    @SerializedName("is_completed")
    val isCompleted: Boolean
)

data class SupabaseProjectDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("sync_code")
    val syncCode: String? = null,
    @SerializedName("nome")
    val nome: String = "",
    @SerializedName("disciplina")
    val disciplina: String = "",
    @SerializedName("data_inicio")
    val dataInicio: String = "",
    @SerializedName("data_entrega")
    val dataEntrega: String = "",
    @SerializedName("dia_estudo")
    val diaEstudo: String = "",
    @SerializedName("horario_inicio")
    val horarioInicio: String = "",
    @SerializedName("horario_termino")
    val horarioTermino: String = "",
    @SerializedName("observacoes")
    val observacoes: String = "",
    @SerializedName("is_completed")
    val isCompleted: Boolean = false
)

fun Project.toSupabasePayload(syncCode: String): SupabaseProjectPayload {
    return SupabaseProjectPayload(
        syncCode = syncCode,
        nome = nome,
        disciplina = disciplina,
        dataInicio = dataInicio,
        dataEntrega = dataEntrega,
        diaEstudo = diaEstudo,
        horarioInicio = horarioInicio,
        horarioTermino = horarioTermino,
        observacoes = observacoes,
        isCompleted = isCompleted
    )
}

fun SupabaseProjectDto.toLocalProject(existingLocalId: Long = 0L): Project {
    return Project(
        id = existingLocalId,
        nome = nome,
        disciplina = disciplina,
        dataInicio = dataInicio,
        dataEntrega = dataEntrega,
        diaEstudo = diaEstudo,
        horarioInicio = horarioInicio,
        horarioTermino = horarioTermino,
        observacoes = observacoes,
        isCompleted = isCompleted,
        remoteId = id,
        isSynced = true
    )
}
