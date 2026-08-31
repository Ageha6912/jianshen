package com.jianshen.fitness.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 应用内备份:JSON 全量,存应用私有目录(免权限),轮换保留最近 KEEP 份。 */
object BackupManager {
    private val stampFmt = SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA)
    private const val PREF = "fitness_prefs"
    private const val KEY_LAST_BACKUP = "last_backup_at"
    private const val KEEP = 4
    private const val AUTO_INTERVAL = 7L * 24 * 3600 * 1000

    fun backupDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, "backups").apply { mkdirs() }
    }

    fun listBackups(context: Context): List<File> =
        backupDir(context).listFiles { f -> f.name.endsWith(".json") }
            ?.sortedByDescending { it.name } ?: emptyList()

    fun lastBackupAt(context: Context): Long =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(KEY_LAST_BACKUP, 0L)

    suspend fun backupNow(context: Context): File = withContext(Dispatchers.IO) {
        val text = buildJsonBackup(FitnessDatabase.get(context))
        val file = File(backupDir(context), "backup_${stampFmt.format(Date())}.json")
        file.writeText(text)
        touchLastBackup(context)
        prune(backupDir(context))
        file
    }

    /** 每周自动备份一次;空库跳过;失败静默(不打扰启动)。 */
    suspend fun autoBackupIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_BACKUP, 0L)
        if (System.currentTimeMillis() - last < AUTO_INTERVAL) return@withContext
        try {
            if (FitnessDatabase.get(context).sessionDao().count() > 0) backupNow(context)
        } catch (_: Exception) {
        }
    }

    /** 覆盖式恢复:整库替换为备份内容。调用方须先做红色确认。 */
    suspend fun restore(context: Context, file: File) = withContext(Dispatchers.IO) {
        importFromJson(FitnessDatabase.get(context), file.readText())
        touchLastBackup(context)
    }

    suspend fun importFromJson(db: FitnessDatabase, text: String) = db.withTransaction {
        val root = JSONObject(text)
        if (root.optString("app") != "健身打卡" || !root.has("sessions")) {
            error("不是本应用的备份文件")
        }
        val sessionsArr = root.getJSONArray("sessions")
        db.setEntryDao().deleteAll()
        db.sessionExerciseDao().deleteAll()
        db.sessionDao().deleteAll()
        for (i in 0 until sessionsArr.length()) {
            val s = sessionsArr.getJSONObject(i)
            val sessionId = s.getLong("id")
            db.sessionDao().insertAll(
                listOf(
                    TrainingSession(
                        id = sessionId,
                        startedAt = s.getLong("startedAt"),
                        finishedAt = if (s.isNull("finishedAt")) null else s.getLong("finishedAt"),
                    )
                )
            )
            val exArr = s.getJSONArray("exercises")
            db.sessionExerciseDao().insertAll(
                (0 until exArr.length()).map { j ->
                    val e = exArr.getJSONObject(j)
                    SessionExercise(
                        sessionId = sessionId,
                        exerciseId = e.getString("exerciseId"),
                        exerciseNameZh = e.getString("name"),
                        sortOrder = j,
                    )
                }
            )
            val setArr = s.getJSONArray("sets")
            db.setEntryDao().insertAll(
                (0 until setArr.length()).map { j ->
                    val e = setArr.getJSONObject(j)
                    SetEntry(
                        sessionId = sessionId,
                        exerciseId = e.getString("exerciseId"),
                        exerciseNameZh = e.getString("name"),
                        weightKg = if (e.isNull("weightKg")) null else e.getDouble("weightKg").toFloat(),
                        reps = e.optInt("reps", 0),
                        durationMin = if (e.isNull("durationMin")) null else e.getInt("durationMin"),
                        distanceKm = if (e.isNull("distanceKm")) null else e.getDouble("distanceKm").toFloat(),
                        completedAt = e.getLong("completedAt"),
                    )
                }
            )
        }
    }

    private fun touchLastBackup(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply()
    }

    private fun prune(dir: File) {
        dir.listFiles { f -> f.name.endsWith(".json") }
            ?.sortedByDescending { it.name }
            ?.drop(KEEP)
            ?.forEach { it.delete() }
    }
}

private val BackupStampFmt = SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA)

/** JSON 全量备份文本(SAF 导出与应用内备份共用)。 */
suspend fun buildJsonBackup(db: FitnessDatabase): String {
    val sessions = db.sessionDao().getAll()
    val exercises = db.sessionExerciseDao().getAll()
    val sets = db.setEntryDao().getAll()
    val root = JSONObject()
    root.put("app", "健身打卡")
    root.put("schemaVersion", 2)
    root.put("exportedAt", System.currentTimeMillis())
    val sessionArray = JSONArray()
    sessions.forEach { session ->
        val obj = JSONObject()
        obj.put("id", session.id)
        obj.put("startedAt", session.startedAt)
        obj.put("finishedAt", session.finishedAt ?: JSONObject.NULL)
        obj.put(
            "exercises",
            JSONArray().apply {
                exercises.filter { it.sessionId == session.id }.forEach {
                    put(
                        JSONObject()
                            .put("exerciseId", it.exerciseId)
                            .put("name", it.exerciseNameZh)
                    )
                }
            },
        )
        obj.put(
            "sets",
            JSONArray().apply {
                sets.filter { it.sessionId == session.id }.forEach {
                    put(
                        JSONObject()
                            .put("exerciseId", it.exerciseId)
                            .put("name", it.exerciseNameZh)
                            .put("weightKg", it.weightKg?.toDouble() ?: JSONObject.NULL)
                            .put("reps", it.reps)
                            .put("durationMin", it.durationMin ?: JSONObject.NULL)
                            .put("distanceKm", it.distanceKm?.toDouble() ?: JSONObject.NULL)
                            .put("completedAt", it.completedAt)
                    )
                }
            },
        )
        sessionArray.put(obj)
    }
    root.put("sessions", sessionArray)
    return root.toString(2)
}
