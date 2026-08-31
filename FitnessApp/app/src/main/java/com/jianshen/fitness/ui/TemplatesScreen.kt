package com.jianshen.fitness.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.withTransaction
import com.jianshen.fitness.FitnessApplication
import com.jianshen.fitness.R
import com.jianshen.fitness.data.Exercise
import com.jianshen.fitness.data.SessionExercise
import com.jianshen.fitness.data.TemplateExercise
import com.jianshen.fitness.data.TemplateWithExercises
import com.jianshen.fitness.data.TrainingSession
import com.jianshen.fitness.data.WorkoutTemplate
import com.jianshen.fitness.data.fmtKg
import kotlinx.coroutines.launch

/** 计划 tab:模板列表 + 新建/编辑/删除 + 从模板开始训练。 */
@Composable
fun TemplatesScreen(onSessionStarted: () -> Unit) {
    val app = LocalContext.current.applicationContext as FitnessApplication
    val db = app.database
    val scope = rememberCoroutineScope()
    val templates by db.templateDao().observeAll().collectAsState(initial = emptyList())
    val active by db.sessionDao().observeActive().collectAsState(initial = null)

    var editing by remember { mutableStateOf<TemplateWithExercises?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<TemplateWithExercises?>(null) }

    when {
        creating || editing != null -> {
            TemplateEditorScreen(
                existing = editing,
                onDone = {
                    creating = false
                    editing = null
                },
            )
            return
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "计划",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )

        if (templates.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "还没有训练计划\n点下方按钮,从零创建你的第一个模板",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(templates, key = { it.template.id }) { template ->
                    TemplateCard(
                        template = template,
                        activeExists = active != null,
                        onStart = {
                            scope.launch {
                                val tpl = db.templateDao().getById(template.template.id) ?: return@launch
                                val sessionId = db.sessionDao().insert(
                                    TrainingSession(startedAt = System.currentTimeMillis())
                                )
                                db.sessionExerciseDao().insertAll(
                                    tpl.exercises.map { te ->
                                        SessionExercise(
                                            sessionId = sessionId,
                                            exerciseId = te.exerciseId,
                                            exerciseNameZh = te.exerciseNameZh,
                                            sortOrder = te.sortOrder,
                                            targetSets = te.targetSets,
                                            targetRepsMin = te.targetRepsMin,
                                            targetRepsMax = te.targetRepsMax,
                                            targetWeightKg = te.targetWeightKg,
                                        )
                                    }
                                )
                                onSessionStarted()
                            }
                        },
                        onEdit = { editing = template },
                        onDelete = { confirmDelete = template },
                    )
                }
            }
        }

        Button(
            onClick = { creating = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = PillShape,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("+ 新建模板", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    confirmDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除模板") },
            text = { Text("确定删除「${template.template.name}」?该操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { db.templateDao().delete(template.template.id) }
                    confirmDelete = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TemplateCard(
    template: TemplateWithExercises,
    activeExists: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = template.template.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_delete),
                    contentDescription = "删除模板",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .combinedClickableNoRipple(onClick = onDelete)
                        .padding(4.dp)
                        .width(20.dp),
                )
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_mode_edit),
                    contentDescription = "编辑模板",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .combinedClickableNoRipple(onClick = onEdit)
                        .padding(4.dp)
                        .width(20.dp),
                )
            }
            template.exercises.forEach { te ->
                val goal = if (te.targetWeightKg != null) {
                    "${te.targetSets}×${te.targetRepsMin}-${te.targetRepsMax}次 · ${te.targetWeightKg.fmtKg()}kg"
                } else {
                    "${te.targetSets}×${te.targetRepsMin}-${te.targetRepsMax}次"
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = te.exerciseNameZh,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = goal,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onStart,
                enabled = !activeExists,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = PillShape,
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Text(if (activeExists) "有进行中的训练,先结束再开始" else "按此计划开始训练")
            }
        }
    }
}

/** 模板编辑器:新建与编辑共用。 */
@Composable
private fun TemplateEditorScreen(existing: TemplateWithExercises?, onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as FitnessApplication
    val db = app.database
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onDone)

    var name by rememberSaveable { mutableStateOf(existing?.template?.name ?: "") }
    val initialItems: List<EditorItem> = existing?.exercises?.map { te ->
        val asset = app.exercises.find { it.id == te.exerciseId }
        EditorItem(
            asset ?: Exercise(
                id = te.exerciseId, nameZh = te.exerciseNameZh, nameEn = "", category = "",
                categoryZh = "", equipmentZh = "", target = "", muscleGroup = "",
                instructionsZh = "", stepsZh = emptyList(), image = "", gif = "", attribution = "",
            ),
            te.targetSets, te.targetRepsMin, te.targetRepsMax, te.targetWeightKg,
        )
    } ?: emptyList()
    var editorItems by remember { mutableStateOf(initialItems) }
    var showPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = if (existing == null) "新建模板" else "编辑模板",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(20) },
            placeholder = { Text("模板名称(如:推日)") },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        editorItems.forEachIndexed { index, item ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.exercise.nameZh,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_close),
                            contentDescription = "移除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .combinedClickableNoRipple(
                                    onClick = { editorItems = editorItems.filterIndexed { i, _ -> i != index } }
                                )
                                .padding(4.dp)
                                .width(18.dp),
                        )
                    }
                    val timed = item.exercise.isTimed
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = item.targetSets.takeIf { it > 0 }?.toString() ?: "",
                            onValueChange = { v ->
                                editorItems = editorItems.update(index) { it.copy(targetSets = v.filter(Char::isDigit).take(2).toIntOrNull() ?: 0) }
                            },
                            label = { Text("组数") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = item.targetRepsMin.takeIf { it > 0 }?.toString() ?: "",
                            onValueChange = { v ->
                                editorItems = editorItems.update(index) { it.copy(targetRepsMin = v.filter(Char::isDigit).take(3).toIntOrNull() ?: 0) }
                            },
                            label = { Text(if (timed) "时长起(分)" else "次数起") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = item.targetRepsMax.takeIf { it > 0 }?.toString() ?: "",
                            onValueChange = { v ->
                                editorItems = editorItems.update(index) { it.copy(targetRepsMax = v.filter(Char::isDigit).take(3).toIntOrNull() ?: 0) }
                            },
                            label = { Text(if (timed) "时长止(分)" else "次数止") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (!timed) {
                        OutlinedTextField(
                            value = item.targetWeightKg?.fmtKg() ?: "",
                            onValueChange = { v ->
                                editorItems = editorItems.update(index) { it.copy(targetWeightKg = v.filter { ch -> ch.isDigit() || ch == '.' }.take(6).toFloatOrNull()) }
                            },
                            label = { Text("目标重量 kg(可选)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = { showPicker = true },
            shape = PillShape,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.width(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加动作")
        }
        Spacer(modifier = Modifier.height(16.dp))

        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val valid = editorItems.filter { it.targetSets > 0 && it.targetRepsMin > 0 && it.targetRepsMax >= it.targetRepsMin }
                if (name.isBlank()) {
                    error = "请填写模板名称"
                    return@Button
                }
                if (valid.isEmpty()) {
                    error = "至少一个动作,且组数/次数有效(次数止 ≥ 次数起)"
                    return@Button
                }
                scope.launch {
                    db.withTransaction {
                        val templateId = existing?.template?.id
                            ?: db.templateDao().insertTemplate(WorkoutTemplate(name = name.trim(), createdAt = System.currentTimeMillis()))
                        if (existing != null) {
                            db.templateDao().updateTemplate(WorkoutTemplate(id = templateId, name = name.trim(), createdAt = existing.template.createdAt))
                            db.templateDao().clearExercises(templateId)
                        }
                        db.templateDao().insertTemplateExercises(
                            valid.mapIndexed { i, item ->
                                TemplateExercise(
                                    templateId = templateId,
                                    exerciseId = item.exercise.id,
                                    exerciseNameZh = item.exercise.nameZh,
                                    sortOrder = i,
                                    targetSets = item.targetSets,
                                    targetRepsMin = item.targetRepsMin,
                                    targetRepsMax = item.targetRepsMax,
                                    targetWeightKg = item.targetWeightKg,
                                )
                            }
                        )
                    }
                    onDone()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = PillShape,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("保存模板", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showPicker) {
        ExercisePickerSheet(
            onPick = { exercise ->
                if (editorItems.none { it.exercise.id == exercise.id }) {
                    editorItems = editorItems + EditorItem(exercise, 3, 8, 12, null)
                }
            },
            alreadyPicked = editorItems.map { it.exercise.id }.toSet(),
            onDismiss = { showPicker = false },
        )
    }
}

/** 编辑器行状态。 */
private data class EditorItem(
    val exercise: Exercise,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val targetWeightKg: Float?,
)

private inline fun List<EditorItem>.update(index: Int, transform: (EditorItem) -> EditorItem): List<EditorItem> =
    mapIndexed { i, item -> if (i == index) transform(item) else item }
