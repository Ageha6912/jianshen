package com.jianshen.fitness.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.jianshen.fitness.R

private data class TabSpec(val label: String, val iconRes: Int)

@Composable
fun AppRoot() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    // 每动作历史页覆盖层:(exerciseId, 名称)
    var historyTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    val tabs = listOf(
        TabSpec("训练", R.drawable.ic_fitness_center),
        TabSpec("历史", R.drawable.ic_calendar_month),
        TabSpec("统计", R.drawable.ic_trending_up),
        TabSpec("计划", R.drawable.ic_assignment),
        TabSpec("动作库", R.drawable.ic_menu_book),
    )

    when {
        showSettings -> {
            SettingsScreen(onBack = { showSettings = false })
            return
        }
        historyTarget != null -> {
            val target = historyTarget!!
            ExerciseHistoryScreen(
                exerciseId = target.first,
                onBack = { historyTarget = null },
            )
            return
        }
    }

    Scaffold(
        bottomBar = {
            // M3 导航条:选中项图标自带 pill 指示器(LibreFit 式),容器与底同色,无分隔线。
            NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                val itemColors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                tabs.forEachIndexed { index, spec ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(painterResource(spec.iconRes), contentDescription = spec.label) },
                        label = { Text(spec.label) },
                        colors = itemColors,
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                0 -> TrainScreen(onOpenSettings = { showSettings = true })
                1 -> HistoryScreen(onOpenExerciseHistory = { id, name -> historyTarget = id to name })
                2 -> StatsScreen(onOpenExercise = { id, name -> historyTarget = id to name })
                3 -> TemplatesScreen(onSessionStarted = { tab = 0 })
                4 -> LibraryScreen()
            }
        }
    }
}
