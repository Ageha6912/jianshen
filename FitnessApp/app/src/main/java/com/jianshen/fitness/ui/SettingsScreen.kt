package com.jianshen.fitness.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jianshen.fitness.FitnessApplication
import com.jianshen.fitness.R
import com.jianshen.fitness.data.ThemePrefs
import com.jianshen.fitness.data.fmtKg
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val DateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
private val StampFmt = SimpleDateFormat("yyyyMMdd", Locale.CHINA)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FitnessApplication
    val db = app.database
    val scope = rememberCoroutineScope()

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val rows = db.setEntryDao().exportRows()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(buildCsv(rows).toByteArray(Charsets.UTF_8))
                } ?: error("无法写入文件")
            }.onSuccess { toast("CSV 已导出") }
                .onFailure { toast("导出失败:${it.message}") }
        }
    }

    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val text = buildJsonBackup(db)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(text.toByteArray(Charsets.UTF_8))
                } ?: error("无法写入文件")
            }.onSuccess { toast("备份已导出") }
                .onFailure { toast("导出失败:${it.message}") }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "返回",
                modifier = Modifier
                    .combinedClickableNoRipple(onClick = onBack)
                    .padding(end = 16.dp),
            )
            Text(text = "设置", style = MaterialTheme.typography.headlineMedium)
        }

        SectionTitle("外观")
        ThemeOptionRow("跟随系统", "system", ThemePrefs.mode) {
            ThemePrefs.set(context, "system")
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ThemeOptionRow("浅色", "light", ThemePrefs.mode) {
            ThemePrefs.set(context, "light")
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ThemeOptionRow("深色", "dark", ThemePrefs.mode) {
            ThemePrefs.set(context, "dark")
        }

        SectionTitle("导出")
        SettingsItem(
            title = "导出 CSV",
            subtitle = "全部训练记录的表格文件,可用 Excel 打开",
            iconRes = R.drawable.ic_description,
        ) {
            csvLauncher.launch("训练记录_${StampFmt.format(Date())}.csv")
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingsItem(
            title = "导出 JSON 备份",
            subtitle = "包含全部训练数据,可用于恢复或迁移",
            iconRes = R.drawable.ic_backup,
        ) {
            jsonLauncher.launch("健身打卡备份_${StampFmt.format(Date())}.json")
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SectionTitle("关于")
        Text(
            text = "健身打卡 v2.0 · 本地存储,不联网",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Text(
            text = "动作数据基于开源项目 exercises-dataset(MIT License)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Text(
            text = "动作图示 © Gym visual — https://gymvisual.com/",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    iconRes: Int? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickableNoRipple(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        iconRes?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeOptionRow(title: String, mode: String, current: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickableNoRipple(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        if (mode == current) {
            Text(
                text = "✓",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun buildCsv(rows: List<com.jianshen.fitness.data.ExportRow>): String {
    val sb = StringBuilder()
    sb.append('\uFEFF') // UTF-8 BOM,避免 Excel 打开中文乱码
    sb.append("日期,动作,组序,重量(kg),次数,时长(分),距离(km)\n")
    val df = DateFmt
    var lastSession = -1L
    var lastExercise = ""
    var setNo = 0
    rows.forEach { row ->
        if (row.startedAt != lastSession || row.exerciseNameZh != lastExercise) {
            lastSession = row.startedAt
            lastExercise = row.exerciseNameZh
            setNo = 1
        } else {
            setNo += 1
        }
        val timed = row.durationMin != null
        sb.append(df.format(Date(row.startedAt)))
            .append(',').append(row.exerciseNameZh)
            .append(',').append(setNo)
            .append(',').append(if (timed) "" else row.weightKg?.fmtKg() ?: "")
            .append(',').append(if (timed) "" else row.reps.toString())
            .append(',').append(row.durationMin?.toString() ?: "")
            .append(',').append(row.distanceKm?.fmtKg() ?: "")
            .append('\n')
    }
    return sb.toString()
}

private suspend fun buildJsonBackup(db: com.jianshen.fitness.data.FitnessDatabase): String {
    val sessions = db.sessionDao().getAll()
    val exercises = db.sessionExerciseDao().getAll()
    val sets = db.setEntryDao().getAll()
    val root = JSONObject()
    root.put("app", "健身打卡")
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
