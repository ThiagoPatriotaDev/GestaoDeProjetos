package gestaodeprojetos.alunos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import gestaodeprojetos.alunos.calendar.CalendarIntentHelper
import gestaodeprojetos.alunos.cloud.CloudSyncOutcome
import gestaodeprojetos.alunos.cloud.SupabaseBackupRepository
import gestaodeprojetos.alunos.model.Project
import kotlinx.coroutines.launch

class FormActivity : AppCompatActivity() {

    private val backupRepository by lazy { SupabaseBackupRepository(this) }
    private lateinit var btnSalvar: Button
    private lateinit var btnCancelar: Button
    private lateinit var swAbrirCalendario: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)

        btnSalvar = findViewById(R.id.btnSalvar)
        btnCancelar = findViewById(R.id.btnCancelar)
        swAbrirCalendario = findViewById(R.id.swAbrirCalendario)

        setupSpinners()
        setupMasks()
        findViewById<android.view.View>(R.id.tvBackForm).setOnClickListener { finish() }

        btnSalvar.setOnClickListener {
            salvarProjeto()
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    private fun salvarProjeto() {
        val projeto = coletarDados()
        if (projeto == null) {
            Toast.makeText(this, getString(R.string.fill_project_name), Toast.LENGTH_SHORT).show()
            return
        }

        definirEstadoCarregamento(carregando = true)

        lifecycleScope.launch {
            val outcome = backupRepository.createAndBackup(projeto)
            val projetoSalvo = when (outcome) {
                is CloudSyncOutcome.Synced -> {
                    Toast.makeText(
                        this@FormActivity,
                        getString(R.string.project_saved_cloud),
                        Toast.LENGTH_SHORT
                    ).show()
                    outcome.project
                }

                is CloudSyncOutcome.NotConfigured -> {
                    Toast.makeText(
                        this@FormActivity,
                        getString(R.string.project_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                    outcome.project
                }

                is CloudSyncOutcome.Error -> {
                    Toast.makeText(
                        this@FormActivity,
                        getString(R.string.project_saved_cloud_pending),
                        Toast.LENGTH_LONG
                    ).show()
                    outcome.project
                }
            }

            if (swAbrirCalendario.isChecked) {
                val abriu = CalendarIntentHelper.openCalendarInsert(this@FormActivity, projetoSalvo)
                if (!abriu) {
                    Toast.makeText(
                        this@FormActivity,
                        getString(R.string.no_calendar_app),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            finish()
        }
    }

    private fun definirEstadoCarregamento(carregando: Boolean) {
        btnSalvar.isEnabled = !carregando
        btnCancelar.isEnabled = !carregando
        swAbrirCalendario.isEnabled = !carregando
        btnSalvar.text = if (carregando) {
            getString(R.string.saving_to_cloud)
        } else {
            getString(R.string.salvar)
        }
    }

    private fun setupMasks() {
        val etDataInicio = findViewById<EditText>(R.id.etDataInicio)
        val etDataEntrega = findViewById<EditText>(R.id.etDataEntrega)
        val etHorarioInicio = findViewById<EditText>(R.id.etHorarioInicio)
        val etHorarioTermino = findViewById<EditText>(R.id.etHorarioTermino)

        etDataInicio.addDateMaskWatcher()
        etDataEntrega.addDateMaskWatcher()
        etHorarioInicio.addTimeMaskWatcher()
        etHorarioTermino.addTimeMaskWatcher()
    }

    private fun coletarDados(): Project? {
        val nome = findViewById<EditText>(R.id.etNomeProjeto).text.toString().trim()
        if (nome.isEmpty()) return null

        val disciplina = findViewById<EditText>(R.id.etDisciplina).text.toString().trim()
        val dataInicio = findViewById<EditText>(R.id.etDataInicio).text.toString().trim()
        val dataEntrega = findViewById<EditText>(R.id.etDataEntrega).text.toString().trim()
        val diaEstudo = findViewById<Spinner>(R.id.spDiaEstudo).selectedItem.toString()
        val horarioInicio = findViewById<EditText>(R.id.etHorarioInicio).text.toString().trim()
        val horarioTermino = findViewById<EditText>(R.id.etHorarioTermino).text.toString().trim()
        val observacoes = findViewById<EditText>(R.id.etObservacoes).text.toString().trim()

        return Project(
            nome = nome,
            disciplina = disciplina,
            dataInicio = dataInicio,
            dataEntrega = dataEntrega,
            diaEstudo = diaEstudo,
            horarioInicio = horarioInicio,
            horarioTermino = horarioTermino,
            observacoes = observacoes,
            isCompleted = false
        )
    }

    private fun setupSpinners() {
        val dias = arrayOf(
            "Segunda-feira",
            "Terça-feira",
            "Quarta-feira",
            "Quinta-feira",
            "Sexta-feira",
            "Sábado",
            "Domingo"
        )
        val adapterDias = ArrayAdapter(this, android.R.layout.simple_spinner_item, dias)
        adapterDias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        findViewById<Spinner>(R.id.spDiaEstudo).adapter = adapterDias
    }

    private fun EditText.addDateMaskWatcher() {
        this.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private var oldText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                oldText = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) return

                val clean = s.toString().filter { it.isDigit() }.take(8)
                val oldClean = oldText.filter { it.isDigit() }

                if (clean.length == oldClean.length) return

                val sb = StringBuilder()
                var index = 0
                for (i in clean.indices) {
                    if (index == 2 || index == 5) {
                        sb.append('/')
                        index++
                    }
                    sb.append(clean[i])
                    index++
                }

                isUpdating = true
                val currentFormatted = sb.toString()
                this@addDateMaskWatcher.setText(currentFormatted)
                this@addDateMaskWatcher.setSelection(currentFormatted.length)
                isUpdating = false
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun EditText.addTimeMaskWatcher() {
        this.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private var oldText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                oldText = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) return

                val clean = s.toString().filter { it.isDigit() }.take(4)
                val oldClean = oldText.filter { it.isDigit() }

                if (clean.length == oldClean.length) return

                val sb = StringBuilder()
                var index = 0
                for (i in clean.indices) {
                    if (index == 2) {
                        sb.append(':')
                        index++
                    }
                    sb.append(clean[i])
                    index++
                }

                isUpdating = true
                val currentFormatted = sb.toString()
                this@addTimeMaskWatcher.setText(currentFormatted)
                this@addTimeMaskWatcher.setSelection(currentFormatted.length)
                isUpdating = false
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }
}
