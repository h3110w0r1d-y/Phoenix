package com.h3110w0r1d.phoenix.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.h3110w0r1d.phoenix.R

@Composable
fun MaxAdjDialog(onDismissRequest: () -> Unit) {
    val maxAdjHelpScroll = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.adj_help_dialog_title))
        },
        text = {
            Text(
                text = stringResource(R.string.adj_help_description),
                modifier =
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalFadingEdge(maxAdjHelpScroll, length = 32.dp)
                        .verticalScroll(maxAdjHelpScroll),
                style = typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.confirm))
            }
        },
    )
}

fun Modifier.verticalFadingEdge(
    scrollState: ScrollState,
    length: Dp = 32.dp,
): Modifier =
    this.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent() // 先绘制文本内容内容
        val fadeLengthPx = length.toPx()
        // 状态判断
        val showTopGradient = scrollState.value > 0
        val showBottomGradient = scrollState.value < scrollState.maxValue

        if (showTopGradient || showBottomGradient) {
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colorStops =
                            arrayOf(
                                // 0% 位置：如果上方有内容，则透明；否则黑色（不透明）
                                0f to if (showTopGradient) Color.Transparent else Color.Black,
                                // 渐变结束位置：从这里往后的中间区域全是黑色
                                (fadeLengthPx / size.height) to Color.Black,
                                // 底部渐变开始位置
                                ((size.height - fadeLengthPx) / size.height) to Color.Black,
                                // 100% 位置：如果下方有内容，则透明；否则黑色
                                1f to if (showBottomGradient) Color.Transparent else Color.Black,
                            ),
                    ),
                blendMode = BlendMode.DstIn,
            )
        }
    }
