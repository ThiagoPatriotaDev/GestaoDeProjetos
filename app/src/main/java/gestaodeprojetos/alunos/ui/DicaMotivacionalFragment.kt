package gestaodeprojetos.alunos.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import gestaodeprojetos.alunos.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class DicaMotivacionalFragment : Fragment() {

    private lateinit var tvDicaTexto: TextView
    private lateinit var btnRefreshDica: ImageView

    private val dicasDeEstudoFallback = listOf(
        "Mantenha o foco em blocos de 25 minutos de estudo seguidos por 5 minutos de descanso (Técnica Pomodoro).",
        "Revise seus apontamentos no mesmo dia para fixar o aprendizado na memória de longo prazo.",
        "Divida grandes projetos acadêmicos em pequenas tarefas diárias para evitar a procrastinação.",
        "Estudar em um ambiente organizado reduz a fadiga mental e melhora a concentração.",
        "Explique o conteúdo para si mesmo ou para um colega para testar seu real domínio sobre a matéria."
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dica_motivacional, container, false)

        tvDicaTexto = view.findViewById(R.id.tvDicaTexto)
        btnRefreshDica = view.findViewById(R.id.btnRefreshDica)

        btnRefreshDica.setOnClickListener {
            carregarDicaWeb()
        }

        carregarDicaWeb()

        return view
    }

    private fun carregarDicaWeb() {
        tvDicaTexto.text = "Buscando dica do dia via Web Service..."
        lifecycleScope.launch(Dispatchers.IO) {
            val dica = buscarDicaDaApi()
            withContext(Dispatchers.Main) {
                if (_bindingIsAvailable()) {
                    tvDicaTexto.text = "\"$dica\""
                }
            }
        }
    }

    private fun _bindingIsAvailable(): Boolean {
        return isAdded && view != null
    }

    private fun buscarDicaDaApi(): String {
        return try {
            val url = URL("https://api.adviceslip.com/advice")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(jsonString)
                val slip = jsonObject.getJSONObject("slip")
                slip.getString("advice")
            } else {
                dicasDeEstudoFallback.random()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            dicasDeEstudoFallback.random()
        }
    }
}
