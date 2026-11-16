package gestaodeprojetos.alunos

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import gestaodeprojetos.alunos.db.AppDatabase
import gestaodeprojetos.alunos.model.Project
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var containerProjetos: LinearLayout
    private lateinit var emptyProjects: LinearLayout
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var todosProjetos = emptyList<Project>()
    private var mostrarConcluidos = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        containerProjetos = findViewById(R.id.containerProjetos)
        emptyProjects = findViewById(R.id.emptyProjects)

        findViewById<Button>(R.id.btnNovoProjeto).setOnClickListener {
            startActivity(Intent(this, FormActivity::class.java))
        }
        findViewById<Button>(R.id.btnEmAndamento).setOnClickListener {
            mostrarConcluidos = false
            atualizarInterface()
        }
        findViewById<Button>(R.id.btnFinalizados).setOnClickListener {
            mostrarConcluidos = true
            atualizarInterface()
        }

        // A tela e os indicadores acompanham as mudanças reais no Room.
        lifecycleScope.launch {
            database.projectDao().getAll().collect { projetos ->
                todosProjetos = projetos
                atualizarInterface()
            }
        }
    }

    private fun atualizarInterface() {
        val ativos = todosProjetos.filterNot { it.isCompleted }
        val concluidos = todosProjetos.filter { it.isCompleted }
        val total = todosProjetos.size
        val urgentes = ativos.count { projeto ->
            val dias = diasAteEntrega(projeto.dataEntrega)
            dias != null && dias <= 3L
        }

        findViewById<TextView>(R.id.tvTotalProjetos).text = total.toString()
        findViewById<TextView>(R.id.tvAtivosDashboard).text = ativos.size.toString()
        findViewById<TextView>(R.id.tvConcluidosDashboard).text = concluidos.size.toString()
        findViewById<TextView>(R.id.tvPrazosDashboard).text = urgentes.toString()
        findViewById<ProgressBar>(R.id.progressoProjetos).progress =
            if (total == 0) 0 else (concluidos.size * 100) / total

        val buttonActive = findViewById<MaterialButton>(R.id.btnEmAndamento)
        val buttonDone = findViewById<MaterialButton>(R.id.btnFinalizados)
        estilizarFiltro(buttonActive, !mostrarConcluidos)
        estilizarFiltro(buttonDone, mostrarConcluidos)

        val projetos = if (mostrarConcluidos) {
            concluidos.sortedByDescending { it.id }
        } else {
            ativos.sortedWith(compareBy<Project> { millisEntrega(it.dataEntrega) ?: Long.MAX_VALUE }.thenByDescending { it.id })
        }

        findViewById<TextView>(R.id.tvSectionHint).text = if (mostrarConcluidos) {
            "Confira tudo o que você já conquistou."
        } else {
            "${ativos.size} ${if (ativos.size == 1) "projeto ativo" else "projetos ativos"} para acompanhar."
        }
        emptyProjects.visibility = if (projetos.isEmpty()) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tvEmptyTitle).text = if (mostrarConcluidos) {
            getString(R.string.empty_finished)
        } else {
            getString(R.string.empty_active)
        }
        findViewById<TextView>(R.id.tvEmptyDescription).text = if (mostrarConcluidos) {
            "Quando você concluir um projeto, ele aparecerá aqui."
        } else {
            "Seu próximo grande projeto começa no botão abaixo."
        }

        containerProjetos.removeAllViews()
        projetos.forEach(::adicionarProjetoAoLayout)
    }

    private fun estilizarFiltro(button: MaterialButton, selecionado: Boolean) {
        button.backgroundTintList = ColorStateList.valueOf(
            getColor(if (selecionado) R.color.brand else R.color.white)
        )
        button.setTextColor(getColor(if (selecionado) R.color.white else R.color.ink_secondary))
        button.strokeWidth = if (selecionado) 0 else (resources.displayMetrics.density + 0.5f).toInt()
        button.strokeColor = ColorStateList.valueOf(getColor(R.color.card_border))
    }

    private fun adicionarProjetoAoLayout(projeto: Project) {
        val itemView = LayoutInflater.from(this).inflate(R.layout.item_project, containerProjetos, false)
        itemView.findViewById<TextView>(R.id.tvNomeProjeto).text = projeto.nome
        itemView.findViewById<TextView>(R.id.tvDisciplina).text =
            projeto.disciplina.ifBlank { "Sem disciplina informada" }
        itemView.findViewById<TextView>(R.id.tvProjectMonogram).text =
            projeto.nome.trim().take(1).uppercase(Locale.getDefault()).ifEmpty { "P" }
        itemView.findViewById<TextView>(R.id.tvEntrega).text =
            if (projeto.dataEntrega.isBlank()) "Sem prazo definido" else "Entrega: ${projeto.dataEntrega}"

        val status = itemView.findViewById<TextView>(R.id.tvStatusProjeto)
        val dias = diasAteEntrega(projeto.dataEntrega)
        when {
            projeto.isCompleted -> {
                status.text = getString(R.string.done)
                status.setBackgroundResource(R.drawable.bg_status_done)
                status.setTextColor(getColor(R.color.mint))
            }
            dias != null && dias < 0 -> {
                status.text = getString(R.string.late)
                status.setBackgroundResource(R.drawable.bg_status_late)
                status.setTextColor(getColor(R.color.red))
            }
            dias != null && dias <= 3 -> {
                status.text = getString(R.string.soon)
                status.setBackgroundResource(R.drawable.bg_status_soon)
                status.setTextColor(getColor(R.color.orange))
            }
            else -> {
                status.text = getString(R.string.ongoing)
                status.setBackgroundResource(R.drawable.bg_status_active)
                status.setTextColor(getColor(R.color.brand_dark))
            }
        }

        val detalhes = itemView.findViewById<Button>(R.id.btnVerDetalhes)
        detalhes.setOnClickListener {
            startActivity(Intent(this, DetailActivity::class.java).putExtra("PROJECT", projeto))
        }
        // O cartão inteiro abre detalhes; os botões mantêm ações próprias.
        itemView.setOnClickListener { detalhes.performClick() }

        itemView.findViewById<MaterialButton>(R.id.btnConcluido).apply {
            text = if (projeto.isCompleted) getString(R.string.reopen) else getString(R.string.concluido)
            setOnClickListener {
                lifecycleScope.launch {
                    database.projectDao().update(projeto.copy(isCompleted = !projeto.isCompleted))
                }
            }
        }
        containerProjetos.addView(itemView)
    }

    private fun millisEntrega(data: String): Long? = try {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).apply { isLenient = false }
        if (data.isBlank()) null else format.parse(data)?.time
    } catch (_: Exception) {
        null
    }

    private fun diasAteEntrega(data: String): Long? {
        val entrega = millisEntrega(data) ?: return null
        val hoje = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(entrega - hoje)
    }
}
