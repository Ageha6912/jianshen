package com.jianshen.fitness.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 预置模板:推 / 拉 / 腿。仅当模板表为空时注入一次(用户删光后不再自动补)。 */
private data class PresetTemplate(val name: String, val items: List<PresetItem>)

private data class PresetItem(val exerciseId: String, val sets: Int, val repsMin: Int, val repsMax: Int)

private val PRESETS = listOf(
    PresetTemplate(
        "推日(胸肩三头)",
        listOf(
            PresetItem("0025", 4, 8, 12),
            PresetItem("0047", 3, 8, 12),
            PresetItem("0405", 3, 8, 12),
            PresetItem("0201", 3, 10, 15),
            PresetItem("0662", 3, 10, 20),
        ),
    ),
    PresetTemplate(
        "拉日(背二头)",
        listOf(
            PresetItem("0032", 4, 5, 8),
            PresetItem("0652", 4, 6, 12),
            PresetItem("0027", 3, 8, 12),
            PresetItem("0150", 3, 10, 12),
            PresetItem("0031", 3, 10, 15),
        ),
    ),
    PresetTemplate(
        "腿日(全腿)",
        listOf(
            PresetItem("0043", 4, 6, 10),
            PresetItem("0739", 3, 8, 12),
            PresetItem("0085", 3, 8, 12),
            PresetItem("0585", 3, 12, 15),
            PresetItem("1372", 4, 12, 20),
        ),
    ),
)

fun seedTemplatesIfNeeded(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    scope.launch {
        try {
            val db = FitnessDatabase.get(context)
            db.withTransaction {
                val dao = db.templateDao()
                if (dao.count() > 0) return@withTransaction
                val exercises = ExerciseRepository.load(context).associateBy { it.id }
                PRESETS.forEach { (name, items) ->
                    val templateId = dao.insertTemplate(
                        WorkoutTemplate(name = name, createdAt = System.currentTimeMillis())
                    )
                    dao.insertTemplateExercises(
                        items.mapIndexed { i, p ->
                            TemplateExercise(
                                templateId = templateId,
                                exerciseId = p.exerciseId,
                                exerciseNameZh = exercises[p.exerciseId]?.nameZh ?: p.exerciseId,
                                sortOrder = i,
                                targetSets = p.sets,
                                targetRepsMin = p.repsMin,
                                targetRepsMax = p.repsMax,
                                targetWeightKg = null,
                            )
                        }
                    )
                }
            }
        } catch (_: Exception) {
            // 种子失败不阻塞启动,用户可手动建模板
        }
    }
}
