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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianshen.fitness.FitnessApplication
import com.jianshen.fitness.R
import com.jianshen.fitness.data.BackupManager
import com.jianshen.fitness.data.ThemePrefs
import java.io.File
import com.jianshen.fitness.data.fmtKg
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val DateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
private val StampFmt = SimpleDateFormat("yyyyMMdd", Locale.CHINA)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FitnessApplication
    val db = app.database
    val scope = rememberCoroutineScope()

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    var showRestoreList by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<File?>(null) }

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
                val text = com.jianshen.fitness.data.buildJsonBackup(db)
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

        SectionTitle("备份")
        val lastBackup = BackupManager.lastBackupAt(context)
        Text(
            text = if (lastBackup == 0L) "还没有应用内备份(每 7 天自动备份一次)"
            else "上次备份:" + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(lastBackup)) + " · 每 7 天自动备份",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsItem(
            title = "立即备份",
            subtitle = "备份到应用私有目录(保留最近 4 份)",
            iconRes = R.drawable.ic_backup,
        ) {
            scope.launch {
                runCatching { BackupManager.backupNow(context) }
                    .onSuccess { toast("备份完成") }
                    .onFailure { toast("备份失败:${it.message}") }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingsItem(
            title = "从备份恢复",
            subtitle = "选择应用内备份,覆盖当前全部数据",
            iconRes = R.drawable.ic_description,
        ) {
            showRestoreList = true
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SectionTitle("关于")
        Text(
            text = "健身打卡 v4.0 · 本地存储,不联网",
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

    if (showRestoreList) {
        val backups = BackupManager.listBackups(context)
        AlertDialog(
            onDismissRequest = { showRestoreList = false },
            title = { Text("从备份恢复") },
            text = {
                if (backups.isEmpty()) {
                    Text("应用内还没有备份文件。先用「立即备份」创建一份")
                } else {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        backups.forEach { file ->
                            TextButton(
                                onClick = {
                                    pendingRestore = file
                                    showRestoreList = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(file.name, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showRestoreList = false }) { Text("取消") } },
        )
    }
    pendingRestore?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("覆盖当前数据?") },
            text = {
                Text(
                    "将清空现有全部训练记录,并替换为「${file.name}」的内容。替换前会自动对当前数据再做一次备份,作为后悔药。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching {
                            BackupManager.backupNow(context)
                            BackupManager.restore(context, file)
                        }
                            .onSuccess { toast("恢复完成") }
                            .onFailure { toast("恢复失败:${it.message}") }
                        pendingRestore = null
                    }
                }) {
                    Text("覆盖", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("取消") } },
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


