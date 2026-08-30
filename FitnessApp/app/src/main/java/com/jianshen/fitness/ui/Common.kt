package com.jianshen.fitness.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/** 无水波纹点击,用于 Notion 风的图标/文字型按钮(✕、设置入口、行点击)。 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.combinedClickableNoRipple(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = this.combinedClickable(
    enabled = enabled,
    indication = null,
    interactionSource = MutableInteractionSource(),
    onLongClick = onLongClick,
    onClick = onClick,
)

/** 在 Composable 中记住一个稳定的 MutableInteractionSource。 */
@Composable
fun rememberInteractionSource() = remember { MutableInteractionSource() }
