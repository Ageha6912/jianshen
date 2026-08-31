package com.jianshen.fitness.data

import android.content.Context
import org.json.JSONArray

data class Exercise(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val category: String,
    val categoryZh: String,
    val equipmentZh: String,
    val target: String,
    val muscleGroup: String,
    val instructionsZh: String,
    val stepsZh: List<String>,
    val image: String,
    val gif: String,
    val attribution: String,
    // "strength"(默认) | "timed"(有氧/计时:记时长+可选距离)
    val type: String = "strength",
) {
    val isTimed: Boolean get() = type == "timed"
    val hasImage: Boolean get() = image.isNotBlank()
    val imageUri: String get() = "file:///android_asset/media/$image"
    val gifUri: String get() = "file:///android_asset/media/$gif"
    val targetZh: String get() = TARGET_ZH[target] ?: target
}

private val TARGET_ZH = mapOf(
    "abs" to "腹肌",
    "obliques" to "腹斜肌",
    "pectorals" to "胸大肌",
    "lats" to "背阔肌",
    "traps" to "斜方肌",
    "lower back" to "下背部",
    "quads" to "股四头肌",
    "hamstrings" to "腘绳肌",
    "glutes" to "臀肌",
    "calves" to "小腿",
    "biceps" to "肱二头肌",
    "triceps" to "肱三头肌",
    "forearms" to "前臂",
    "shoulders" to "肩部",
    "deltoids" to "三角肌",
    "hip flexors" to "髋屈肌",
    "adductors" to "内收肌",
    "abductors" to "外展肌",
    "full body" to "全身",
    "cardio" to "心肺",
)

fun Float.fmtKg(): String = if (this % 1f == 0f) this.toInt().toString() else this.toString()

fun Float?.fmtKgOrNull(): String = this?.fmtKg() ?: ""

object ExerciseRepository {
    @Volatile
    private var cache: List<Exercise>? = null

    fun load(context: Context): List<Exercise> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            val steps = mutableListOf<String>()
            val result = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                steps.clear()
                o.optJSONArray("stepsZh")?.let { sa ->
                    for (j in 0 until sa.length()) steps.add(sa.getString(j))
                }
                Exercise(
                    id = o.getString("id"),
                    nameZh = o.getString("nameZh"),
                    nameEn = o.getString("nameEn"),
                    category = o.getString("category"),
                    categoryZh = o.getString("categoryZh"),
                    equipmentZh = o.getString("equipmentZh"),
                    target = o.optString("target"),
                    muscleGroup = o.optString("muscleGroup"),
                    instructionsZh = o.optString("instructionsZh"),
                    stepsZh = steps.toList(),
                    image = o.getString("image"),
                    gif = o.getString("gif"),
                    attribution = o.getString("attribution"),
                    type = o.optString("type", "strength"),
                )
            }
            cache = result
            return result
        }
    }
}
