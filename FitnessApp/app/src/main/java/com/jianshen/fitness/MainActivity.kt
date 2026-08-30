package com.jianshen.fitness

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import com.jianshen.fitness.data.ThemePrefs
import com.jianshen.fitness.ui.AppRoot
import com.jianshen.fitness.ui.FitnessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ThemePrefs.load(this)
        setContent {
            val mode = ThemePrefs.mode
            val systemDark = isSystemInDarkTheme()
            val dark = when (mode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }
            // 手动切换外观时,系统栏图标明暗也要跟着内容走(系统 uiMode 并未变化)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
                onDispose { }
            }
            FitnessTheme(darkTheme = dark) {
                // 根部 Surface 统一设定 LocalContentColor,否则设置页等绕过
                // Scaffold 的路径默认黑字,深色模式下不可读。
                androidx.compose.material3.Surface(
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                ) {
                    AppRoot()
                }
            }
        }
    }
}
