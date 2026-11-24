package gestaodeprojetos.alunos.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val disciplina: String,
    val dataInicio: String,
    val dataEntrega: String,
    val diaEstudo: String,
    val horarioInicio: String,
    val horarioTermino: String,
    val observacoes: String = "",
    val isCompleted: Boolean = false,
    val remoteId: String? = null,
    val isSynced: Boolean = false
) : Serializable
