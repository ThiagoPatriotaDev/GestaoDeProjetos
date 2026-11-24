package gestaodeprojetos.alunos

import gestaodeprojetos.alunos.calendar.CalendarIntentHelper
import gestaodeprojetos.alunos.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CalendarIntentHelperTest {

    private val saoPauloTz: TimeZone = TimeZone.getTimeZone("GMT-03:00")

    @Test
    fun buildsWeeklyRecurringEventWithObservations() {
        // 23/11/2026 é segunda-feira -> próxima quarta-feira é 25/11/2026
        val project = Project(
            nome = "App Gestão de Projetos",
            disciplina = "Programação Mobile",
            dataInicio = "23/11/2026",
            dataEntrega = "16/12/2026",
            diaEstudo = "Quarta-feira",
            horarioInicio = "19:00",
            horarioTermino = "21:30",
            observacoes = "Revisar backup na nuvem com Supabase"
        )

        val event = CalendarIntentHelper.buildEventData(project, timeZone = saoPauloTz)

        assertEquals("App Gestão de Projetos — Programação Mobile", event.title)
        assertTrue(event.description.contains("Programação Mobile"))
        assertTrue(event.description.contains("Revisar backup na nuvem com Supabase"))
        assertEquals("FREQ=WEEKLY;BYDAY=WE;UNTIL=20261217T025959Z", event.rrule)

        val beginCal = Calendar.getInstance(saoPauloTz).apply { timeInMillis = event.beginTimeMillis }
        val endCal = Calendar.getInstance(saoPauloTz).apply { timeInMillis = event.endTimeMillis }
        assertEquals(2026, beginCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, beginCal.get(Calendar.MONTH))
        assertEquals(25, beginCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(19, beginCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, beginCal.get(Calendar.MINUTE))
        assertEquals(21, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, endCal.get(Calendar.MINUTE))
    }

    @Test
    fun fallsBackToOneHourWhenEndTimeIsInvalid() {
        val project = Project(
            nome = "Seminário",
            disciplina = "",
            dataInicio = "25/11/2026",
            dataEntrega = "25/11/2026",
            diaEstudo = "Quarta-feira",
            horarioInicio = "14:00",
            horarioTermino = "13:00",
            observacoes = ""
        )

        val event = CalendarIntentHelper.buildEventData(project, timeZone = saoPauloTz)

        assertEquals("Seminário", event.title)
        assertEquals(60 * 60 * 1000L, event.endTimeMillis - event.beginTimeMillis)
        assertNull(event.rrule)
    }
}
