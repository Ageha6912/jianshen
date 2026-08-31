package com.jianshen.fitness.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    // 来自模板的目标(v4,可空 = 普通添加没有目标)
    val targetSets: Int? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetWeightKg: Float? = null,
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
    // 有氧/计时动作(v4,可空;非空即计时类记录,此时 weightKg/reps 无意义 reps=0)
    val durationMin: Int? = null,
    val distanceKm: Float? = null,
)

@Entity(tableName = "templates")
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "template_exercises",
    primaryKeys = ["templateId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("templateId")],
)
data class TemplateExercise(
    val templateId: Long,
    val exerciseId: String,
    val exerciseNameZh: String,
    val sortOrder: Int,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val targetWeightKg: Float? = null,
)

data class TemplateWithExercises(
    @Embedded val template: WorkoutTemplate,
    @Relation(parentColumn = "id", entityColumn = "templateId")
    val exercises: List<TemplateExercise>,
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
    val durationMin: Int?,
    val distanceKm: Float?,
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

    @Insert
    suspend fun insertAll(sessions: List<TrainingSession>)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun count(): Int
}

@Dao
interface SessionExerciseDao {
    @Insert
    suspend fun insert(exercise: SessionExercise)

    @Insert
    suspend fun insertAll(exercises: List<SessionExercise>)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY sortOrder")
    fun observeForSession(sessionId: Long): Flow<List<SessionExercise>>

    @Query("DELETE FROM session_exercises WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun delete(sessionId: Long, exerciseId: String)

    @Query("DELETE FROM set_entries WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun deleteSets(sessionId: Long, exerciseId: String)

    @Query("SELECT * FROM session_exercises ORDER BY sessionId")
    suspend fun getAll(): List<SessionExercise>

    @Query("DELETE FROM session_exercises")
    suspend fun deleteAll()
}

@Dao
interface SetEntryDao {
    @Insert
    suspend fun insert(entry: SetEntry)

    @Insert
    suspend fun insertAll(entries: List<SetEntry>)

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
        "SELECT * FROM set_entries WHERE exerciseId = :exerciseId " +
            "AND sessionId IN (SELECT id FROM sessions WHERE finishedAt IS NOT NULL) " +
            "ORDER BY completedAt"
    )
    suspend fun getAllFinishedForExercise(exerciseId: String): List<SetEntry>

    @Query(
        "SELECT exerciseId, exerciseNameZh, weightKg, reps, " +
            "MAX(weightKg * (1.0 + reps / 30.0)) AS e1rm " +
            "FROM set_entries WHERE weightKg IS NOT NULL AND durationMin IS NULL " +
            "GROUP BY exerciseId ORDER BY e1rm DESC"
    )
    fun observePrs(): Flow<List<PrRow>>

    @Query(
        "SELECT s.startedAt AS startedAt, e.exerciseNameZh AS exerciseNameZh, " +
            "e.weightKg AS weightKg, e.reps AS reps, e.durationMin AS durationMin, e.distanceKm AS distanceKm " +
            "FROM set_entries e INNER JOIN sessions s ON s.id = e.sessionId " +
            "ORDER BY s.startedAt, e.completedAt"
    )
    suspend fun exportRows(): List<ExportRow>

    @Query("SELECT * FROM set_entries ORDER BY completedAt")
    suspend fun getAll(): List<SetEntry>

    @Query("DELETE FROM set_entries")
    suspend fun deleteAll()
}

@Dao
interface TemplateDao {
    @Insert
    suspend fun insertTemplate(template: WorkoutTemplate): Long

    @Insert
    suspend fun insertTemplateExercises(exercises: List<TemplateExercise>)

    @Update
    suspend fun updateTemplate(template: WorkoutTemplate)

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun count(): Int

    @Transaction
    @Query("SELECT * FROM templates ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TemplateWithExercises>>

    @Transaction
    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getById(id: Long): TemplateWithExercises?

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun clearExercises(templateId: Long)
}

@Database(
    entities = [
        TrainingSession::class,
        SessionExercise::class,
        SetEntry::class,
        WorkoutTemplate::class,
        TemplateExercise::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun sessionExerciseDao(): SessionExerciseDao
    abstract fun setEntryDao(): SetEntryDao
    abstract fun templateDao(): TemplateDao

    companion object {
        /**
         * v3→v4:有氧/计时列 + 模板两表 + session_exercises 目标列。
         * 全部可空/新表,旧数据原样保留;自本版起不再允许摧毁式迁移。
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_entries ADD COLUMN durationMin INTEGER")
                db.execSQL("ALTER TABLE set_entries ADD COLUMN distanceKm REAL")
                db.execSQL("ALTER TABLE session_exercises ADD COLUMN targetSets INTEGER")
                db.execSQL("ALTER TABLE session_exercises ADD COLUMN targetRepsMin INTEGER")
                db.execSQL("ALTER TABLE session_exercises ADD COLUMN targetRepsMax INTEGER")
                db.execSQL("ALTER TABLE session_exercises ADD COLUMN targetWeightKg REAL")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `templates` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `template_exercises` (" +
                        "`templateId` INTEGER NOT NULL, `exerciseId` TEXT NOT NULL, " +
                        "`exerciseNameZh` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, " +
                        "`targetSets` INTEGER NOT NULL, `targetRepsMin` INTEGER NOT NULL, " +
                        "`targetRepsMax` INTEGER NOT NULL, `targetWeightKg` REAL, " +
                        "PRIMARY KEY(`templateId`, `exerciseId`), " +
                        "FOREIGN KEY(`templateId`) REFERENCES `templates`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_template_exercises_templateId` " +
                        "ON `template_exercises` (`templateId`)"
                )
            }
        }

        @Volatile
        private var instance: FitnessDatabase? = null

        fun get(context: Context): FitnessDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context, FitnessDatabase::class.java, "fitness.db")
                    .addMigrations(MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
