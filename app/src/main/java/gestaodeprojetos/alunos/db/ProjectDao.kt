package gestaodeprojetos.alunos.db

import androidx.room.*
import gestaodeprojetos.alunos.model.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY id DESC")
    fun getAll(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE isCompleted = 0")
    fun getAllActive(): Flow<List<Project>>

    @Insert
    suspend fun insert(project: Project)

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)
}
