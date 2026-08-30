package com.jianshen.fitness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jianshen.fitness.FitnessApplication
import com.jianshen.fitness.data.SetEntry
import com.jianshen.fitness.data.TrainingSession
import com.jianshen.fitness.data.fmtKg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

@Composable
fun HistoryScreen() {
    var subTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "历史",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryPill(label = "日志", selected = subTab == 0, onClick = { subTab = 0 })
            HistoryPill(label = "个人纪录", selected = subTab == 1, onClick = { subTab = 1 })
        }
        Spacer(modifier = Modifier.padding(top = 12.dp))
        when (subTab) {
            0 -> LogList()
            1 -> PrList()
        }
    }
}

/** LibreFit 式 pill 页签:选中用 secondaryContainer 胶囊。 */
@Composable
private fun HistoryPill(label: String, selected: Boolean, onClick: () -> Unit) {
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
            .combinedClickableNoRipple(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun LogList() {
    val app = LocalContext.current.applicationContext as FitnessApplication
    val sessions by app.database.sessionDao().observeFinished().collectAsState(initial = emptyList())
    val allSets by app.database.setEntryDao().observeAll().collectAsState(initial = emptyList())
    val setsBySession = allSets.groupBy { it.sessionId }

    if (sessions.isEmpty()) {
        EmptyHint("还没有完成的训练")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(sessions, key = { it.id }) { session ->
            SessionBlock(session, setsBySession[session.id].orEmpty())
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun SessionBlock(session: TrainingSession, sets: List<SetEntry>) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = DateTimeFmt.format(Date(session.startedAt)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${sets.size} 组",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.padding(top = 4.dp))
        sets.forEach { set ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = set.exerciseNameZh,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = (set.weightKg?.let { "${it.fmtKg()}kg × " } ?: "") + "${set.reps}次",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PrList() {
    val app = LocalContext.current.applicationContext as FitnessApplication
    val prs by app.database.setEntryDao().observePrs().collectAsState(initial = emptyList())

    if (prs.isEmpty()) {
        EmptyHint("完成第一组训练后,这里会记录你的个人纪录")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
    ) {
        items(prs, key = { it.exerciseId }) { pr ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = pr.exerciseNameZh,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = "最佳 ${pr.weightKg.fmtKg()}kg × ${pr.reps}次",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "估算1RM",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp, bottom = 4.dp),
                        )
                        Text(
                            text = "≈ ${Math.round(pr.e1rm)}kg",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp),
        )
    }
}
