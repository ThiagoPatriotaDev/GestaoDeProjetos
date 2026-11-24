package gestaodeprojetos.alunos.calendar

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import gestaodeprojetos.alunos.model.Project
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class CalendarEventData(
    val title: String,
    val description: String,
    val beginTimeMillis: Long,
    val endTimeMillis: Long,
    val rrule: String?,
    val timeZoneId: String
)

object CalendarIntentHelper {

    private data class DayMapping(val calendarDay: Int, val rruleCode: String)

    private val dayMappings = mapOf(
        "segunda-feira" to DayMapping(Calendar.MONDAY, "MO"),
        "terça-feira" to DayMapping(Calendar.TUESDAY, "TU"),
        "terca-feira" to DayMapping(Calendar.TUESDAY, "TU"),
        "quarta-feira" to DayMapping(Calendar.WEDNESDAY, "WE"),
        "quinta-feira" to DayMapping(Calendar.THURSDAY, "TH"),
        "sexta-feira" to DayMapping(Calendar.FRIDAY, "FR"),
        "sábado" to DayMapping(Calendar.SATURDAY, "SA"),
        "sabado" to DayMapping(Calendar.SATURDAY, "SA"),
        "domingo" to DayMapping(Calendar.SUNDAY, "SU")
    )

    fun buildEventData(
        project: Project,
        timeZone: TimeZone = TimeZone.getDefault(),
        now: Calendar = Calendar.getInstance(timeZone)
    ): CalendarEventData {
        val baseStartDate = parseDate(project.dataInicio, timeZone)
            ?: parseDate(project.dataEntrega, timeZone)
            ?: cloneDateOnly(now, timeZone)

        val deliveryDate = parseDate(project.dataEntrega, timeZone)
        val ptBrLocale = Locale.forLanguageTag("pt-BR")
        val dayMapping = dayMappings[project.diaEstudo.trim().lowercase(ptBrLocale)]

        val firstSessionDate = cloneDateOnly(baseStartDate, timeZone)
        var canRecurWeekly = false

        if (dayMapping != null) {
            val diffDays = (dayMapping.calendarDay - firstSessionDate.get(Calendar.DAY_OF_WEEK) + 7) % 7
            firstSessionDate.add(Calendar.DAY_OF_YEAR, diffDays)

            if (deliveryDate != null && isDateAfter(firstSessionDate, deliveryDate)) {
                firstSessionDate.timeInMillis = baseStartDate.timeInMillis
            } else {
                canRecurWeekly = deliveryDate != null && isDateAfter(deliveryDate, firstSessionDate)
            }
        }

        val (startHour, startMinute) = parseTime(project.horarioInicio) ?: (9 to 0)
        val startCal = (firstSessionDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val parsedEndTime = parseTime(project.horarioTermino)
        val endCal = (firstSessionDate.clone() as Calendar).apply {
            if (parsedEndTime != null) {
                set(Calendar.HOUR_OF_DAY, parsedEndTime.first)
                set(Calendar.MINUTE, parsedEndTime.second)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            } else {
                timeInMillis = startCal.timeInMillis + 60 * 60 * 1000L
            }
        }

        if (endCal.timeInMillis <= startCal.timeInMillis) {
            endCal.timeInMillis = startCal.timeInMillis + 60 * 60 * 1000L
        }

        val rrule = if (canRecurWeekly && dayMapping != null && deliveryDate != null) {
            val untilLocal = (deliveryDate.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 0)
            }
            val untilUtc = formatRRuleUntilUtc(untilLocal)
            "FREQ=WEEKLY;BYDAY=${dayMapping.rruleCode};UNTIL=$untilUtc"
        } else {
            null
        }

        val title = if (project.disciplina.isNotBlank()) {
            "${project.nome} — ${project.disciplina}"
        } else {
            project.nome
        }

        return CalendarEventData(
            title = title,
            description = buildDescription(project),
            beginTimeMillis = startCal.timeInMillis,
            endTimeMillis = endCal.timeInMillis,
            rrule = rrule,
            timeZoneId = timeZone.id
        )
    }

    fun buildInsertIntent(project: Project): Intent {
        val data = buildEventData(project)
        return Intent(Intent.ACTION_INSERT).apply {
            this.data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, data.title)
            putExtra(CalendarContract.Events.DESCRIPTION, data.description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, data.beginTimeMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, data.endTimeMillis)
            putExtra(CalendarContract.Events.EVENT_TIMEZONE, data.timeZoneId)
            if (!data.rrule.isNullOrBlank()) {
                putExtra(CalendarContract.Events.RRULE, data.rrule)
            }
        }
    }

    fun openCalendarInsert(context: Context, project: Project): Boolean {
        return try {
            context.startActivity(buildInsertIntent(project))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun buildDescription(project: Project): String {
        val lines = mutableListOf<String>()
        if (project.disciplina.isNotBlank()) {
            lines.add("Disciplina: ${project.disciplina.trim()}")
        }
        if (project.dataInicio.isNotBlank()) {
            lines.add("Data de início: ${project.dataInicio.trim()}")
        }
        if (project.dataEntrega.isNotBlank()) {
            lines.add("Data de entrega: ${project.dataEntrega.trim()}")
        }
        if (project.diaEstudo.isNotBlank()) {
            lines.add("Dia de estudo: ${project.diaEstudo.trim()}")
        }
        if (project.horarioInicio.isNotBlank() || project.horarioTermino.isNotBlank()) {
            val horario = listOf(project.horarioInicio.trim(), project.horarioTermino.trim())
                .filter { it.isNotEmpty() }
                .joinToString(" às ")
            lines.add("Horário reservado: $horario")
        }
        if (project.observacoes.isNotBlank()) {
            lines.add("")
            lines.add("Observações:")
            lines.add(project.observacoes.trim())
        }
        lines.add("")
        lines.add("Agendado pelo app UniFocus.")
        return lines.joinToString("\n")
    }

    private fun parseDate(raw: String, timeZone: TimeZone): Calendar? {
        val trimmed = raw.trim()
        if (trimmed.length != 10) return null
        return try {
            val parser = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")).apply {
                isLenient = false
                this.timeZone = timeZone
            }
            val parsed = parser.parse(trimmed) ?: return null
            Calendar.getInstance(timeZone).apply {
                time = parsed
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTime(raw: String): Pair<Int, Int>? {
        val parts = raw.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    private fun cloneDateOnly(source: Calendar, timeZone: TimeZone): Calendar {
        return Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, source.get(Calendar.YEAR))
            set(Calendar.MONTH, source.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun isDateAfter(first: Calendar, second: Calendar): Boolean {
        val y1 = first.get(Calendar.YEAR)
        val y2 = second.get(Calendar.YEAR)
        if (y1 != y2) return y1 > y2
        return first.get(Calendar.DAY_OF_YEAR) > second.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatRRuleUntilUtc(calendar: Calendar): String {
        return SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(calendar.time)
    }
}
