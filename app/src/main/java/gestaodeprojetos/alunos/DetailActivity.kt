package gestaodeprojetos.alunos

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import gestaodeprojetos.alunos.calendar.CalendarIntentHelper
import gestaodeprojetos.alunos.cloud.CloudSyncOutcome
import gestaodeprojetos.alunos.cloud.SupabaseBackupRepository
import gestaodeprojetos.alunos.db.AppDatabase
import gestaodeprojetos.alunos.model.Project
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val backupRepository by lazy { SupabaseBackupRepository(this) }
    private lateinit var tvDetailCloudStatus: TextView
    private var projetoAtual: Project? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        tvDetailCloudStatus = findViewById(R.id.tvDetailCloudStatus)

        val projetoIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("PROJECT", Project::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("PROJECT") as? Project
        }

        findViewById<View>(R.id.tvBackDetail).setOnClickListener { finish() }

        projetoIntent?.let { inicial ->
            projetoAtual = inicial
            preencherDetalhes(inicial)

            lifecycleScope.launch {
                val atualizado = database.projectDao().getById(inicial.id)
                if (atualizado != null) {
                    projetoAtual = atualizado
                    preencherDetalhes(atualizado)
                }
            }
        }

        findViewById<View>(R.id.rowCloudStatus).setOnClickListener {
            val projeto = projetoAtual ?: return@setOnClickListener
            if (!projeto.isSynced) {
                sincronizarProjetoNaNuvem(projeto)
            } else {
                Toast.makeText(this, "Este projeto já está salvo na nuvem (Supabase).", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnAdicionarCalendario).setOnClickListener {
            val projeto = projetoAtual ?: return@setOnClickListener
            val abriu = CalendarIntentHelper.openCalendarInsert(this, projeto)
            if (!abriu) {
                Toast.makeText(this, getString(R.string.no_calendar_app), Toast.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.btnVoltar).setOnClickListener {
            finish()
        }
    }

    private fun preencherDetalhes(projeto: Project) {
        findViewById<TextView>(R.id.tvDetailStatus).text =
            if (projeto.isCompleted) "FINALIZADO" else "EM ANDAMENTO"
        findViewById<TextView>(R.id.tvDetailNomeProjeto).text = projeto.nome
        findViewById<TextView>(R.id.tvDetailDisciplinaTitle).text =
            getString(R.string.label_disciplina, projeto.disciplina.ifBlank { "Sem disciplina" })

        findViewById<TextView>(R.id.tvDetailProjetoValue).text = projeto.nome
        findViewById<TextView>(R.id.tvDetailDisciplinaValue).text =
            projeto.disciplina.ifBlank { "Não informada" }
        findViewById<TextView>(R.id.tvDetailInicioValue).text =
            projeto.dataInicio.ifBlank { "Não informada" }
        findViewById<TextView>(R.id.tvDetailEntregaValue).text =
            projeto.dataEntrega.ifBlank { "Não informada" }
        findViewById<TextView>(R.id.tvDetailDiaEstudoValue).text = projeto.diaEstudo
        findViewById<TextView>(R.id.tvDetailHorarioValue).text =
            if (projeto.horarioInicio.isBlank() && projeto.horarioTermino.isBlank()) {
                "Não informado"
            } else {
                "${projeto.horarioInicio} - ${projeto.horarioTermino}"
            }

        findViewById<TextView>(R.id.tvDetailObservacoes).text =
            projeto.observacoes.ifEmpty { getString(R.string.no_observations) }

        if (projeto.isSynced) {
            tvDetailCloudStatus.text = getString(R.string.cloud_synced)
            tvDetailCloudStatus.setTextColor(getColor(R.color.mint))
        } else {
            tvDetailCloudStatus.text = getString(R.string.cloud_not_synced)
            tvDetailCloudStatus.setTextColor(getColor(R.color.brand))
        }
    }

    private fun sincronizarProjetoNaNuvem(projeto: Project) {
        tvDetailCloudStatus.text = getString(R.string.cloud_syncing)
        tvDetailCloudStatus.setTextColor(getColor(R.color.ink_secondary))

        lifecycleScope.launch {
            when (val outcome = backupRepository.updateAndBackup(projeto)) {
                is CloudSyncOutcome.Synced -> {
                    projetoAtual = outcome.project
                    preencherDetalhes(outcome.project)
                    Toast.makeText(
                        this@DetailActivity,
                        "Projeto sincronizado no Supabase!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is CloudSyncOutcome.NotConfigured -> {
                    preencherDetalhes(outcome.project)
                    Toast.makeText(
                        this@DetailActivity,
                        "Configure SUPABASE_URL e SUPABASE_ANON_KEY em SupabaseConfig.kt.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                is CloudSyncOutcome.Error -> {
                    preencherDetalhes(outcome.project)
                    Toast.makeText(this@DetailActivity, outcome.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
