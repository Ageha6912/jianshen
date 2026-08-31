package com.jianshen.fitness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jianshen.fitness.FitnessApplication
import com.jianshen.fitness.data.Exercise

@Composable
fun LibraryScreen(onOpenExerciseHistory: (String, String) -> Unit = { _, _ -> }) {
    val app = LocalContext.current.applicationContext as FitnessApplication
    val exercises = app.exercises
    var category by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Exercise?>(null) }

    selected?.let { exercise ->
        ExerciseDetail(
            exercise = exercise,
            onBack = { selected = null },
            onViewHistory = { onOpenExerciseHistory(exercise.id, exercise.nameZh) },
        )
        return
    }

    val categories = exercises.map { it.categoryZh }.distinct()
    val filtered = if (category == null) exercises else exercises.filter { it.categoryZh == category }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "动作库",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                CategoryChip(
                    label = "全部 ${exercises.size}",
                    selected = category == null,
                    onClick = { category = null },
                )
            }
            items(categories) { c ->
                CategoryChip(
                    label = "$c ${exercises.count { it.categoryZh == c }}",
                    selected = category == c,
                    onClick = { category = c },
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp),
        ) {
            items(filtered, key = { it.id }) { exercise ->
                ExerciseRow(exercise) { selected = exercise }
                HorizontalDividerRow()
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = exercise.imageUri,
            contentDescription = exercise.nameZh,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = exercise.nameZh, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = exercise.nameEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = exercise.equipmentZh,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HorizontalDividerRow() {
    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
