package com.jianshen.fitness.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.nativeCanvas
import com.jianshen.fitness.FitnessApplication
import com.jianshen.fitness.data.SetEntry
import com.jianshen.fitness.data.TrainingSession
import com.jianshen.fitness.data.fmtKg
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val DayKeyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
private val DayTitleFmt = SimpleDateFormat("M月d日", Locale.CHINA)
private val SessionDateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

private fun dayKey(millis: Long): String = DayKeyFmt.format(Date(millis))

private fun mondayOf(cal: Calendar): Long {
    val c = cal.clone() as Calendar
    c.firstDayOfWeek = Calendar.MONDAY
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    val dow = c.get(Calendar.DAY_OF_WEEK)
    val diff = (dow + 5) % 7 // MONDAY=2 → 0
    c.add(Calendar.DAY_OF_MONTH, -diff)
    return c.timeInMillis
}

/** 统计 tab:概览指标 + 6 个月热力图 + 每动作走势入口。 */
@Composable
fun StatsScreen(onOpenExercise: (String, String) -> Unit) {
    val app = LocalContext.current.applicationContext as FitnessApplication
    val db = app.database
    val sessions by db.sessionDao().observeFinished().collectAsState(initial = emptyList())
    val allSets by db.setEntryDao().observeAll().collectAsState(initial = emptyList())
    var selectedDay by rememberSaveable { mutableStateOf<String?>(null) }

    val finishedSessions = remember(sessions) { sessions }
    val setsBySession = remember(allSets) { allSets.groupBy { it.sessionId } }

    // 每日组数(仅已完成训练)
    val daySetCounts = remember(finishedSessions, allSets) {
        val result = mutableMapOf<String, Int>()
        finishedSessions.forEach { s ->
            setsBySession[s.id].orEmpty().forEach { _ ->
                result[dayKey(s.startedAt)] = (result[dayKey(s.startedAt)] ?: 0) + 1
            }
        }
        result
    }
    // 每天 summary:日期 → (组数, 动作名集合)
    val daySummary = remember(finishedSessions, allSets) {
        val result = mutableMapOf<String, MutableList<SetEntry>>()
        finishedSessions.forEach { s ->
            setsBySession[s.id].orEmpty().forEach { set ->
                result.getOrPut(dayKey(s.startedAt)) { mutableListOf() }.add(set)
            }
        }
        result
    }

    val now = System.currentTimeMillis()
    val totalVolume = remember(allSets) {
        allSets.filter { it.durationMin == null }.sumOf { (it.weightKg ?: 0f).toDouble() * it.reps }
    }
    // 本周(周一起)训练次数
    val weekStart = remember { mondayOf(Calendar.getInstance().apply { timeInMillis = now }) }
    val weekCount = remember(finishedSessions, weekStart) {
        finishedSessions.count { it.startedAt >= weekStart }
    }
    // 连续不空训练周
    val weekStreak = remember(finishedSessions) {
        if (finishedSessions.isEmpty()) 0
        else {
            val weeks = finishedSessions.map { mondayOf(Calendar.getInstance().apply { timeInMillis = it.startedAt }) }
                .distinct().sortedDescending()
            var streak = 0
            var cursor = Calendar.getInstance().apply { timeInMillis = weeks.first() }.timeInMillis
            val currentWeekStart = mondayOf(Calendar.getInstance())
            if (cursor != currentWeekStart) {
                // 本周还没练:从上周开始数,但当前周算中断
                val c = Calendar.getInstance().apply { timeInMillis = currentWeekStart }
                c.add(Calendar.WEEK_OF_YEAR, -1)
                if (weeks.contains(c.timeInMillis)) {
                    cursor = c.timeInMillis
                    streak = 1
                } else return@remember 0
            } else {
                streak = 1
            }
            while (true) {
                val c = Calendar.getInstance().apply { timeInMillis = cursor }
                c.add(Calendar.WEEK_OF_YEAR, -1)
                if (weeks.contains(c.timeInMillis)) {
                    streak++
                    cursor = c.timeInMillis
                } else break
            }
            streak
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            text = "统计",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("本周训练", "$weekCount 次", Modifier.weight(1f))
            MetricCard("连续周", "$weekStreak 周", Modifier.weight(1f))
            MetricCard(
                "总容量",
                if (totalVolume >= 1000) "%.1f t".format(totalVolume / 1000) else "${totalVolume.roundToInt()} kg",
                Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "训练热力图(近 6 个月)", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Heatmap(
            daySetCounts = daySetCounts,
            selectedDay = selectedDay,
            onSelect = { selectedDay = if (selectedDay == it) null else it },
        )
        selectedDay?.let { key ->
            val entries = daySummary[key].orEmpty()
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (entries.isEmpty()) {
                        Text(
                            text = "当天没有训练记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = "共 ${entries.size} 组:" + entries.joinToString("、") { it.exerciseNameZh }
                                .split("、").distinct().joinToString("、"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "动作走势", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        val withHistory = remember(allSets) {
            allSets.groupBy { it.exerciseId }.map { (id, list) -> id to (list.firstOrNull()?.exerciseNameZh ?: id) }
        }
        if (withHistory.isEmpty()) {
            Text(
                text = "完成训练后,这里可以看到每个动作的走势",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        withHistory.forEach { (id, name) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenExercise(id, name) }
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "→",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun Heatmap(
    daySetCounts: Map<String, Int>,
    selectedDay: String?,
    onSelect: (String) -> Unit,
) {
    val weeks = remember { 25 downTo 0 }.map { offset ->
        val c = Calendar.getInstance()
        c.timeInMillis = mondayOf(c)
        c.add(Calendar.WEEK_OF_YEAR, -offset)
        c.timeInMillis
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 当前周是最右一列:进入页面自动滚到末尾,保证"今天"可见
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(Unit) { listState.scrollToItem(25) }
        LazyRow(
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            reverseLayout = false,
        ) {
            items(weeks) { weekStart ->
                Column {
                    (0..6).forEach { dayOffset ->
                        val c = Calendar.getInstance().apply {
                            timeInMillis = weekStart
                            add(Calendar.DAY_OF_MONTH, dayOffset)
                        }
                        val key = dayKey(c.timeInMillis)
                        val future = c.timeInMillis > System.currentTimeMillis()
                        val count = daySetCounts[key] ?: 0
                        val color = when {
                            future -> Color.Transparent
                            count == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
                            count <= 2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            count <= 4 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .padding(1.dp)
                                .size(15.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                                .then(
                                    if (future) Modifier else Modifier.clickable { onSelect(key) }
                                )
                                .then(
                                    if (selectedDay == key) Modifier.background(
                                        Color.Transparent,
                                        RoundedCornerShape(3.dp)
                                    ) else Modifier
                                ),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
}

/** 每动作历史:走势折线图(力量)或最佳摘要(计时)+ 全部记录列表。 */
@Composable
fun ExerciseHistoryScreen(exerciseId: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as FitnessApplication
    val db = app.database
    BackHandler(onBack = onBack)

    val asset = remember { app.exercises.find { it.id == exerciseId } }
    val title = asset?.nameZh ?: exerciseId
    val timed = asset?.isTimed == true

    val setsRaw = produceState<List<SetEntry>?>(initialValue = null, exerciseId) {
        value = db.setEntryDao().getAllFinishedForExercise(exerciseId)
    }
    val sessionsRaw = produceState<List<TrainingSession>?>(initialValue = null) {
        value = db.sessionDao().getAll()
    }
    val sets = setsRaw.value
    val sessions = sessionsRaw.value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "←",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .combinedClickableNoRipple(onClick = onBack)
                    .padding(end = 12.dp),
            )
            Text(text = "$title · 历史", style = MaterialTheme.typography.headlineSmall)
        }

        when {
            sets == null || sessions == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {}
            }
            sets.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "该动作还没有已完成的历史记录",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                val sessionStartById = sessions.associate { it.id to it.startedAt }
                val bySession = sets.groupBy { it.sessionId }.toList()
                    .sortedBy { sessionStartById[it.first] ?: 0L }

                if (!timed) {
                    var mode by rememberSaveable { mutableStateOf(0) } // 0 = 重量, 1 = 1RM
                    val points = bySession.mapNotNull { (sid, list) ->
                        val start = sessionStartById[sid] ?: return@mapNotNull null
                        val weighted = list.filter { it.weightKg != null }
                        if (weighted.isEmpty()) return@mapNotNull null
                        val value = if (mode == 0) {
                            (weighted.maxOf { it.weightKg ?: 0f }).toDouble()
                        } else {
                            weighted.maxOf { (it.weightKg ?: 0f) * (1.0 + it.reps / 30.0) }
                        }
                        start to value
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        HistoryPillChip("最高重量", selected = mode == 0) { mode = 0 }
                        HistoryPillChip("估算1RM", selected = mode == 1) { mode = 1 }
                    }
                    if (points.size >= 2) {
                        TrendChart(
                            points = points,
                            valueLabel = if (mode == 0) "kg" else "kg(1RM)",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    } else if (points.size == 1) {
                        Text(
                            text = "只有一次训练记录(共 ${sets.size} 组),再练一次就能看到走势",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    val bestDuration = sets.maxOfOrNull { it.durationMin ?: 0 } ?: 0
                    val bestDistance = sets.mapNotNull { it.distanceKm }.maxOrNull()
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        MetricCard("最长单次", "$bestDuration 分", Modifier.weight(1f))
                        if (bestDistance != null) {
                            MetricCard("最远单次", "${bestDistance.fmtKg()} km", Modifier.weight(1f))
                        }
                    }
                }

                Text(
                    text = "全部记录",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bySession.sortedByDescending { sessionStartById[it.first] ?: 0L }.forEach { (sid, list) ->
                        val dateStr = SessionDateFmt.format(Date(sessionStartById[sid] ?: 0L))
                        item(key = sid) {
                            Column {
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                list.forEach { set ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = if (set.durationMin != null) {
                                                "${set.durationMin} 分" + (set.distanceKm?.let { " · ${it.fmtKg()} km" } ?: "")
                                            } else {
                                                (set.weightKg?.let { "${it.fmtKg()}kg × " } ?: "") + "${set.reps}次"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                                HorizontalDividerThin()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryPillChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun HorizontalDividerThin() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/** 轻量自绘折线图:点位均匀分布,白金主色,末值标注。 */
@Composable
private fun TrendChart(points: List<Pair<Long, Double>>, valueLabel: String) {
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = androidx.compose.ui.platform.LocalDensity.current
    val textSizeSp = 11

    val values = points.map { it.second }
    val minV = values.min()
    val maxV = values.max()
    val span = (maxV - minV).takeIf { it > 1e-6 } ?: 1.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padL = 12.dp.toPx()
            val padR = 12.dp.toPx()
            val padT = 24.dp.toPx()
            val padB = 20.dp.toPx()
            val w = size.width - padL - padR
            val h = size.height - padT - padB

            // 网格:3 条横线
            (0..2).forEach { i ->
                val y = padT + h * i / 2f
                drawLine(gridColor, Offset(padL, y), Offset(padL + w, y), 1f)
            }

            fun xy(i: Int, v: Double): Offset {
                val x = if (points.size == 1) padL + w / 2 else padL + w * i / (points.size - 1f)
                val y = (padT + h * (1f - ((v - minV) / span).toFloat())).toFloat()
                return Offset(x, y)
            }

            val path = Path()
            points.indices.forEach { i ->
                val p = xy(i, points[i].second)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            points.indices.forEach { i ->
                drawCircle(dotColor, radius = 3.dp.toPx(), center = xy(i, points[i].second))
            }

            // 末值标注
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(255, 160, 150, 135)
                textSize = with(density) { textSizeSp.sp.toPx() }
                isAntiAlias = true
            }
            val last = points.last()
            val label = "%.1f %s".format(last.second, valueLabel)
            val pos = xy(points.size - 1, last.second)
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            drawContext.canvas.nativeCanvas.drawText(
                label,
                size.width - padR,
                (pos.y - 8.dp.toPx()).coerceAtLeast(textSizeSp.sp.toPx()),
                paint,
            )
        }
    }
}
