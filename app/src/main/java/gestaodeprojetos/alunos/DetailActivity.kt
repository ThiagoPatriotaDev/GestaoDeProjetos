package gestaodeprojetos.alunos

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import gestaodeprojetos.alunos.R
import gestaodeprojetos.alunos.model.Project
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val projeto = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("PROJECT", Project::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("PROJECT") as? Project
        }

        findViewById<android.view.View>(R.id.tvBackDetail).setOnClickListener { finish() }

        projeto?.let {
            findViewById<TextView>(R.id.tvDetailStatus).text =
                if (it.isCompleted) "FINALIZADO" else "EM ANDAMENTO"
            findViewById<TextView>(R.id.tvDetailNomeProjeto).text = it.nome
            findViewById<TextView>(R.id.tvDetailDisciplinaTitle).text = getString(R.string.label_disciplina, it.disciplina)
            
            findViewById<TextView>(R.id.tvDetailProjetoValue).text = it.nome
            findViewById<TextView>(R.id.tvDetailDisciplinaValue).text = it.disciplina
            findViewById<TextView>(R.id.tvDetailInicioValue).text = it.dataInicio
            findViewById<TextView>(R.id.tvDetailEntregaValue).text = it.dataEntrega
            findViewById<TextView>(R.id.tvDetailDiaEstudoValue).text = it.diaEstudo
            findViewById<TextView>(R.id.tvDetailHorarioValue).text = "${it.horarioInicio} - ${it.horarioTermino}"
            
            findViewById<TextView>(R.id.tvDetailObservacoes).text = it.observacoes.ifEmpty { getString(R.string.no_observations) }

            findViewById<Button>(R.id.btnAdicionarCalendario).setOnClickListener { _ ->
                adicionarAoCalendario(it)
            }
        }

        findViewById<Button>(R.id.btnVoltar).setOnClickListener {
            finish()
        }
    }

    private fun adicionarAoCalendario(projeto: Project) {
        try {
            val dateFormater = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            
            // Parse da Data de Início do Projeto
            val dateInicio = if (projeto.dataInicio.isNotEmpty()) {
                dateFormater.parse(projeto.dataInicio)
            } else null

            // Parse da Data de Entrega/Término do Projeto
            val dateEntrega = if (projeto.dataEntrega.isNotEmpty()) {
                dateFormater.parse(projeto.dataEntrega)
            } else null

            val calendarInicio = Calendar.getInstance()
            if (dateInicio != null) {
                calendarInicio.time = dateInicio
            } else if (dateEntrega != null) {
                calendarInicio.time = dateEntrega
            }

            // Configura o horário de início se fornecido (formato hh:mm)
            if (projeto.horarioInicio.isNotEmpty() && projeto.horarioInicio.contains(":")) {
                val partesHora = projeto.horarioInicio.split(":")
                val hora = partesHora[0].toIntOrNull() ?: 0
                val minuto = partesHora[1].toIntOrNull() ?: 0
                calendarInicio.set(Calendar.HOUR_OF_DAY, hora)
                calendarInicio.set(Calendar.MINUTE, minuto)
            } else {
                calendarInicio.set(Calendar.HOUR_OF_DAY, 9) // Valor padrão: 9h da manhã
                calendarInicio.set(Calendar.MINUTE, 0)
            }

            val calendarTermino = Calendar.getInstance()
            if (dateEntrega != null) {
                calendarTermino.time = dateEntrega
            } else if (dateInicio != null) {
                calendarTermino.time = dateInicio
            }

            // Configura o horário de término se fornecido (formato hh:mm)
            if (projeto.horarioTermino.isNotEmpty() && projeto.horarioTermino.contains(":")) {
                val partesHora = projeto.horarioTermino.split(":")
                val hora = partesHora[0].toIntOrNull() ?: 0
                val minuto = partesHora[1].toIntOrNull() ?: 0
                calendarTermino.set(Calendar.HOUR_OF_DAY, hora)
                calendarTermino.set(Calendar.MINUTE, minuto)
            } else {
                if (dateEntrega == null || projeto.dataInicio == projeto.dataEntrega) {
                    // Se for no mesmo dia e não tiver hora de término, coloca 1 hora após o início
                    calendarTermino.timeInMillis = calendarInicio.timeInMillis + (60 * 60 * 1000)
                } else {
                    // Se for em dias diferentes, coloca no final do dia de entrega (ex: 18h)
                    calendarTermino.set(Calendar.HOUR_OF_DAY, 18)
                    calendarTermino.set(Calendar.MINUTE, 0)
                }
            }

            // Cria a Intent nativa do sistema para inserção de eventos no Calendário
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "Desenvolvimento e Entrega: ${projeto.nome}")
                putExtra(CalendarContract.Events.DESCRIPTION, "Disciplina: ${projeto.disciplina}\nDia de estudo definido: ${projeto.diaEstudo}")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendarInicio.timeInMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, calendarTermino.timeInMillis)
                putExtra(CalendarContract.Events.STATUS, 1) // Confirmado
            }

            startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao abrir o calendário. Verifique os formatos de data/hora.", Toast.LENGTH_LONG).show()
        }
    }
}