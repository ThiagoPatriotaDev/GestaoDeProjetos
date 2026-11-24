package gestaodeprojetos.alunos

import gestaodeprojetos.alunos.cloud.SupabaseProjectDto
import gestaodeprojetos.alunos.cloud.toLocalProject
import gestaodeprojetos.alunos.cloud.toSupabasePayload
import gestaodeprojetos.alunos.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseMapperTest {

    @Test
    fun mapsLocalProjectToSupabasePayloadAndBack() {
        val project = Project(
            id = 7L,
            nome = "TCC",
            disciplina = "Engenharia de Software",
            dataInicio = "01/12/2026",
            dataEntrega = "20/12/2026",
            diaEstudo = "Terça-feira",
            horarioInicio = "18:00",
            horarioTermino = "20:00",
            observacoes = "Entregar versão final",
            isCompleted = true
        )

        val payload = project.toSupabasePayload(syncCode = "UF-ABCD-1234-WXYZ")
        assertEquals("UF-ABCD-1234-WXYZ", payload.syncCode)
        assertEquals("TCC", payload.nome)
        assertEquals("Entregar versão final", payload.observacoes)
        assertTrue(payload.isCompleted)

        val dto = SupabaseProjectDto(
            id = "550e8400-e29b-41d4-a716-446655440000",
            syncCode = payload.syncCode,
            nome = payload.nome,
            disciplina = payload.disciplina,
            dataInicio = payload.dataInicio,
            dataEntrega = payload.dataEntrega,
            diaEstudo = payload.diaEstudo,
            horarioInicio = payload.horarioInicio,
            horarioTermino = payload.horarioTermino,
            observacoes = payload.observacoes,
            isCompleted = payload.isCompleted
        )

        val restored = dto.toLocalProject(existingLocalId = 7L)
        assertEquals(7L, restored.id)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", restored.remoteId)
        assertTrue(restored.isSynced)
        assertEquals("TCC", restored.nome)
        assertEquals("Entregar versão final", restored.observacoes)
    }
}
