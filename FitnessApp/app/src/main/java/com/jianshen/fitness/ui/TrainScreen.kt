package com.jianshen.fitness.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jianshen.fitness.FitnessApplication
import com.jianshen.fitness.R
import com.jianshen.fitness.data.Exercise
import com.jianshen.fitness.data.RestTimer
import com.jianshen.fitness.data.SessionExercise
import com.jianshen.fitness.data.SetEntry
import com.jianshen.fitness.data.TrainingSession
import com.jianshen.fitness.data.cancelRestNotification
import com.jianshen.fitness.data.canPostNotifications
import com.jianshen.fitness.data.fmtKg
import com.jianshen.fitness.data.fmtKgOrNull
import com.jianshen.fitness.data.playRestAlarm
import com.jianshen.fitness.data.postRestNotification
import com.jianshen.fitness.data.restSecondsPref
import com.jianshen.fitness.data.saveRestSecondsPref
import kotlinx.coroutines.launch

/** 一次勾组录入:力量 = 重量+次数;计时 = 时长+可选距离。 */
data class LoggedSet(
    val weightKg: Float?,
    val reps: Int,
    val durationMin: Int?,
    val distanceKm: Float?,
)

@Composable
fun TrainScreen(onOpenSettings: () -> Unit) {
    val app = LocalContext.current.applicationContext as FitnessApplication
    val db = app.database
    val scope = rememberCoroutineScope()
    val active by db.sessionDao().observeActive().collectAsState(initial = null)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "训练",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "设置",
                modifier = Modifier
                    .combinedClickableNoRipple(onClick = onOpenSettings)
                    .padding(8.dp),
            )
        }

        val session = active
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "没有进行中的训练",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                db.sessionDao().insert(TrainingSession(startedAt = System.currentTimeMillis()))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = PillShape,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text("+ 开始训练", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            SessionEditor(session = session)
        }
    }
}

@Composable
private fun SessionEditor(session: TrainingSession) {
    val app = LocalContext.current.applicationContext as FitnessApplication
    val db = app.database
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val exercises by db.sessionExerciseDao().observeForSession(session.id)
        .collectAsState(initial = emptyList())
    val sets by db.setEntryDao().observeForSession(session.id)
        .collectAsState(initial = emptyList())
    val restRemaining by RestTimer.remaining.collectAsState()
    val restFinished by RestTimer.finishedNaturally.collectAsState()
    var restSeconds by remember { mutableStateOf(restSecondsPref(context)) }
    var showDurationPicker by remember { mutableStateOf(false) }
    var showAddExercise by remember { mutableStateOf(false) }

    // 通知权限:进入训练后请求一次,拒绝则只保留页内横幅。
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !canPostNotifications(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // 计时结束时撤掉常驻通知。
    LaunchedEffect(restRemaining) {
        if (restRemaining == null) cancelRestNotification(context)
    }
    // 倒计时自然走完:响铃 + 震动。
    LaunchedEffect(restFinished) {
        if (restFinished) {
            playRestAlarm(context)
            RestTimer.clearFinished()
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        restRemaining?.let { seconds ->
            RestBanner(
                seconds = seconds,
                onSkip = { RestTimer.skip() },
                onAdd15 = { RestTimer.add15() },
                onLongPress = { showDurationPicker = true },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (exercises.isEmpty()) {
            Text(
                text = "先添加今天的第一个动作",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
        exercises.forEach { ex ->
            ExerciseSetCard(
                session = session,
                exercise = ex,
                sets = sets.filter { it.exerciseId == ex.exerciseId },
                onRemove = {
                    scope.launch {
                        db.sessionExerciseDao().deleteSets(session.id, ex.exerciseId)
                        db.sessionExerciseDao().delete(session.id, ex.exerciseId)
                    }
                },
                onSetChecked = { logged ->
                    scope.launch {
                        db.setEntryDao().insert(
                            SetEntry(
                                sessionId = session.id,
                                exerciseId = ex.exerciseId,
                                exerciseNameZh = ex.exerciseNameZh,
                                weightKg = logged.weightKg,
                                reps = logged.reps,
                                completedAt = System.currentTimeMillis(),
                                durationMin = logged.durationMin,
                                distanceKm = logged.distanceKm,
                            )
                        )
                        RestTimer.start(restSeconds)
                        postRestNotification(context, restSeconds)
                    }
                },
                onSetDeleted = { set ->
                    scope.launch { db.setEntryDao().delete(set.id) }
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = { showAddExercise = true },
            shape = PillShape,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("+ 添加动作")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch { db.sessionDao().finish(session.id, System.currentTimeMillis()) }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = PillShape,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("结束训练", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAddExercise) {
        ExercisePickerSheet(
            onPick = { exercise ->
                scope.launch {
                    db.sessionExerciseDao().insert(
                        SessionExercise(
                            sessionId = session.id,
                            exerciseId = exercise.id,
                            exerciseNameZh = exercise.nameZh,
                            sortOrder = exercises.size,
                        )
                    )
                }
            },
            alreadyPicked = exercises.map { it.exerciseId }.toSet(),
            onDismiss = { showAddExercise = false },
        )
    }
    if (showDurationPicker) {
        RestDurationDialog(
            current = restSeconds,
            onPick = { seconds ->
                restSeconds = seconds
                saveRestSecondsPref(context, seconds)
                if (RestTimer.remaining.value != null) RestTimer.start(seconds)
                showDurationPicker = false
            },
            onDismiss = { showDurationPicker = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RestBanner(
    seconds: Int,
    onSkip: () -> Unit,
    onAdd15: () -> Unit,
    onLongPress: () -> Unit,
) {
    val total by RestTimer.total.collectAsState()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "休息",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$seconds",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onAdd15) {
                    Text("+15s", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onSkip) {
                    Text("跳过", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            LinearProgressIndicator(
                progress = {
                    val t = total ?: seconds
                    if (t <= 0) 0f else seconds.toFloat() / t
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun RestDurationDialog(
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("休息时长") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120).forEach { seconds ->
                    OutlinedButton(
                        onClick = { onPick(seconds) },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (seconds == current) "$seconds s ✓" else "$seconds s",
                            fontWeight = if (seconds == current) FontWeight.Bold else FontWeight.Normal,
                            color = if (seconds == current) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 逐组表格卡:力量(重量/次数)与计时(时长/距离)双模式,含模板目标与缩略图。 */
@Composable
private fun ExerciseSetCard(
    session: TrainingSession,
    exercise: SessionExercise,
    sets: List<SetEntry>,
    onRemove: () -> Unit,
    onSetChecked: (LoggedSet) -> Unit,
    onSetDeleted: (SetEntry) -> Unit,
) {
    val app = LocalContext.current.applicationContext as FitnessApplication
    val db = app.database
    val assetById = remember { app.exercises.associateBy { it.id } }
    val asset = assetById[exercise.exerciseId]
    val timed = asset?.isTimed == true
    val hasDistance = asset?.categoryZh == "有氧"
    var lastSummary by remember(exercise.exerciseId) { mutableStateOf<String?>(null) }
    var weightText by remember(exercise.exerciseId) { mutableStateOf("") }
    var repsText by remember(exercise.exerciseId) { mutableStateOf("") }
    var durationText by remember(exercise.exerciseId) { mutableStateOf("") }
    var distanceText by remember(exercise.exerciseId) { mutableStateOf("") }

    // 上次成绩:最近一次已完成训练中该动作的组数据,汇总成一行。
    LaunchedEffect(exercise.exerciseId, session.finishedAt == null) {
        val recent = db.setEntryDao().getRecentFinishedForExercise(exercise.exerciseId)
        lastSummary = recent
            .groupBy { it.sessionId }
            .maxByOrNull { it.key }
            ?.value
            ?.let { lastSets ->
                val top = lastSets.maxByOrNull { it.weightKg ?: -1f } ?: lastSets.first()
                when {
                    top.durationMin != null ->
                        "上次:" + "${top.durationMin}分" +
                            (top.distanceKm?.let { " · ${it.fmtKg()}km" } ?: "") +
                            " ×${lastSets.size}组"
                    else ->
                        "上次:" + (top.weightKg?.let { "${it.fmtKg()}kg×" } ?: "") + "${top.reps}次×${lastSets.size}组"
                }
            }
    }
    // 勾选一组后,草稿行自动带入刚完成的数值,方便连续记录同重量。
    LaunchedEffect(sets.size) {
        sets.lastOrNull()?.let {
            if (it.durationMin != null) {
                durationText = it.durationMin.toString()
                distanceText = it.distanceKm?.fmtKgOrNull() ?: ""
            } else {
                weightText = it.weightKg.fmtKgOrNull()
                repsText = it.reps.toString()
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumb(asset, exercise.exerciseNameZh, size = 44)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.exerciseNameZh,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    // 模板目标(可空 = 普通添加)
                    exercise.targetSets?.let { target ->
                        val goal = if (timed) {
                            "目标 ${target} 组 × ${exercise.targetRepsMin ?: 20}-${exercise.targetRepsMax ?: 30} 分"
                        } else {
                            "目标 ${target} 组 × ${exercise.targetRepsMin ?: 8}-${exercise.targetRepsMax ?: 12} 次" +
                                (exercise.targetWeightKg?.let { " · ${it.fmtKg()}kg" } ?: "")
                        }
                        val done = sets.count { it.durationMin != null || it.weightKg != null || it.reps > 0 }
                        Text(
                            text = "$goal · 已完成 $done/$target",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "移除动作",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .combinedClickableNoRipple(onClick = onRemove)
                        .padding(6.dp)
                        .size(20.dp),
                )
            }
            lastSummary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 表头
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(28.dp),
                )
                Text(
                    text = if (timed) "时长(分)" else "重量(kg)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (timed) "距离(km)" else "次数",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(44.dp))
            }
            // 已完成组
            sets.forEachIndexed { index, set ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(28.dp),
                    )
                    if (timed) {
                        Text(
                            text = "${set.durationMin ?: 0} 分",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = set.distanceKm?.let { "${it.fmtKg()} km" } ?: "—",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            text = set.weightKg?.let { "${it.fmtKg()}" } ?: "—",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${set.reps}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .combinedClickableNoRipple(onClick = { onSetDeleted(set) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = "删除该组",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            // 草稿录入行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Spacer(modifier = Modifier.width(20.dp))
                if (timed) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter(Char::isDigit).take(3) },
                        placeholder = { Text("分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f),
                    )
                    if (hasDistance) {
                        OutlinedTextField(
                            value = distanceText,
                            onValueChange = { distanceText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(6) },
                            placeholder = { Text("km") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    val duration = durationText.toIntOrNull()
                    val distance = distanceText.toFloatOrNull()
                    Button(
                        onClick = {
                            onSetChecked(
                                LoggedSet(
                                    weightKg = null,
                                    reps = 0,
                                    durationMin = duration,
                                    distanceKm = if (hasDistance) distance else null,
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        enabled = duration in 1..600,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = "记一组",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(6) },
                        placeholder = { Text("kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it.filter(Char::isDigit).take(3) },
                        placeholder = { Text("次数") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            val w = weightText.toFloatOrNull()
                            val r = repsText.toIntOrNull() ?: return@Button
                            if (r !in 1..999) return@Button
                            onSetChecked(LoggedSet(weightKg = w, reps = r, durationMin = null, distanceKm = null))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        enabled = repsText.toIntOrNull() in 1..999,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = "记一组",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 动作缩略图:有图加载图片,无图(计时动作)用图标占位。 */
@Composable
fun ExerciseThumb(asset: Exercise?, contentDescription: String, size: Int) {
    if (asset != null && asset.hasImage) {
        AsyncImage(
            model = asset.imageUri,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape((size / 4).dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape((size / 4).dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_fitness_center),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size((size / 2).dp),
            )
        }
    }
}

/** 添加动作:底部弹层(列表/表单型交互),按部位分组。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    onPick: (Exercise) -> Unit,
    alreadyPicked: Set<String>,
    onDismiss: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as FitnessApplication
    val assetById = remember { app.exercises.associateBy { it.id } }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                text = "添加动作",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            val grouped = app.exercises.groupBy { it.categoryZh }
            // 固定高度:弹层从第一帧起就是最终尺寸,避免拖动时"半开→全高"吸附跳变(抖动根源)
            LazyColumn(modifier = Modifier.height(440.dp).padding(bottom = 24.dp)) {
                grouped.forEach { (category, list) ->
                    item(key = "cat_$category") {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(list.size, key = { list[it].id }) { index ->
                        val exercise = list[index]
                        val picked = exercise.id in alreadyPicked
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickableNoRipple(enabled = !picked) { onPick(exercise) }
                                .padding(vertical = 6.dp),
                        ) {
                            ExerciseThumb(assetById[exercise.id], exercise.nameZh, size = 36)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = exercise.nameZh,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (picked) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (picked) {
                                Text(
                                    "已添加",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
