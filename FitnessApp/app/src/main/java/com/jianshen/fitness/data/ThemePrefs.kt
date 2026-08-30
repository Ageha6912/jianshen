package com.jianshen.fitness.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// 外观模式:system 跟随系统 / light 浅色 / dark 深色。
// 用 mutableStateOf 包一层,设置页切换后整个 Compose 树即时重组换主题。
object ThemePrefs {
    var mode by mutableStateOf("system")
        private set

    fun load(context: Context) {
        mode = context.getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
            .getString("theme_mode", "system") ?: "system"
    }

    fun set(context: Context, value: String) {
        mode = value
        context.getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
            .edit().putString("theme_mode", value).apply()
    }
}
