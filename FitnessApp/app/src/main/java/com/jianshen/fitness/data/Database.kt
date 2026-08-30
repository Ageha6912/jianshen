package com.jianshen.fitness.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sessions")
data class TrainingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val finishedAt: Long? = null,
)

@Entity(tableName = "session_exercises", primaryKeys = ["sessionId", "exerciseId"])
data class SessionExercise(
    val sessionId: Long,
    val exerciseId: String,
    val exerciseNameZh: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "set_entries",
    foreignKeys = [
        ForeignKey(
            entity = TrainingSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val exerciseNameZh: String,
    val weightKg: Float?,
    val reps: Int,
    val completedAt: Long,
)

data class PrRow(
    val exerciseId: String,
    val exerciseNameZh: String,
    val weightKg: Float,
    val reps: Int,
    val e1rm: Double,
)

data class ExportRow(
    val startedAt: Long,
    val exerciseNameZh: String,
    val weightKg: Float?,
    val reps: Int,
)

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: TrainingSession): Long

    @Query("UPDATE sessions SET finishedAt = :at WHERE id = :id")
    suspend fun finish(id: Long, at: Long)

    @Query("SELECT * FROM sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<TrainingSession?>

    @Query("SELECT * FROM sessions WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinished(): Flow<List<TrainingSession>>

    @Query("SELECT * FROM sessions ORDER BY startedAt")
    suspend fun getAll(): List<TrainingSession>
}

@Dao
interface SessionExerciseDao {
    @Insert
    suspend fun insert(exercise: SessionExercise)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY sortOrder")
    fun observeForSession(sessionId: Long): Flow<List<SessionExercise>>

    @Query("DELETE FROM session_exercises WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun delete(sessionId: Long, exerciseId: String)

    @Query("DELETE FROM set_entries WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun deleteSets(sessionId: Long, exerciseId: String)

    @Query("SELECT * FROM session_exercises ORDER BY sessionId")
    suspend fun getAll(): List<SessionExercise>
}

@Dao
interface SetEntryDao {
    @Insert
    suspend fun insert(entry: SetEntry)

    @Query("SELECT * FROM set_entries WHERE sessionId = :sessionId ORDER BY completedAt")
    fun observeForSession(sessionId: Long): Flow<List<SetEntry>>

    @Query("SELECT * FROM set_entries ORDER BY completedAt")
    fun observeAll(): Flow<List<SetEntry>>

    @Query("DELETE FROM set_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        "SELECT * FROM set_entries WHERE exerciseId = :exerciseId " +
            "AND sessionId IN (SELECT id FROM sessions WHERE finishedAt IS NOT NULL) " +
            "ORDER BY completedAt DESC LIMIT 12"
    )
    suspend fun getRecentFinishedForExercise(exerciseId: String): List<SetEntry>

    @Query(
        "SELECT exerciseId, exerciseNameZh, weightKg, reps, " +
            "MAX(weightKg * (1.0 + reps / 30.0)) AS e1rm " +
            "FROM set_entries WHERE weightKg IS NOT NULL " +
            "GROUP BY exerciseId ORDER BY e1rm DESC"
    )
    fun observePrs(): Flow<List<PrRow>>

    @Query(
        "SELECT s.startedAt AS startedAt, e.exerciseNameZh AS exerciseNameZh, " +
            "e.weightKg AS weightKg, e.reps AS reps " +
            "FROM set_entries e INNER JOIN sessions s ON s.id = e.sessionId " +
            "ORDER BY s.startedAt, e.completedAt"
    )
    suspend fun exportRows(): List<ExportRow>

    @Query("SELECT * FROM set_entries ORDER BY completedAt")
    suspend fun getAll(): List<SetEntry>
}

@Database(
    entities = [TrainingSession::class, SessionExercise::class, SetEntry::class],
    version = 3,
    exportSchema = false,
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun sessionExerciseDao(): SessionExerciseDao
    abstract fun setEntryDao(): SetEntryDao

    companion object {
        @Volatile
        private var instance: FitnessDatabase? = null

        fun get(context: Context): FitnessDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context, FitnessDatabase::class.java, "fitness.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
