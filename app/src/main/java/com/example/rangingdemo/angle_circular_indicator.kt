package com.example.rangingdemo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


/**
 * 角度指示器
 * 负数角度向左延伸，正数角度向右延伸
 * @param angle 当前角度，范围在 -180°~180° 之间
 * @param radius 指示器半径
 * @param strokeWidth 线条宽度
 * @param color 指示器颜色
 * @param backgroundColor 背景圆环颜色
 */
@Composable
fun AngleCircularIndicator(
    angle: Float,
    modifier: Modifier = Modifier,
    radius: Dp = 70.dp,
    strokeWidth: Dp = 8.dp,
    color: Color = Color(0xFF2196F3),
    backgroundColor: Color = Color(0xFFEEEEEE)
) {
    // 限制角度在 [-180, 180] 范围内
    val clampedAngle = angle.coerceIn(-180f, 180f)

    Canvas(modifier = modifier.size(radius * 2)) {
        // 绘制背景圆环
        drawBackgroundCircle(
            radius = radius.toPx(),
            strokeWidth = strokeWidth.toPx(),
            color = backgroundColor
        )

        // 绘制角度指示器
        drawAngleIndicator(
            angle = clampedAngle,
            radius = radius.toPx(),
            strokeWidth = strokeWidth.toPx(),
            color = color
        )
    }
}

/**
 * 绘制背景圆环
 */
private fun DrawScope.drawBackgroundCircle(
    radius: Float,
    strokeWidth: Float,
    color: Color
) {
    drawCircle(
        color = color,
        radius = radius - strokeWidth / 2,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}

/**
 * 绘制角度指示器
 * 0度在顶部，负数向左延伸，正数向右延伸
 */
private fun DrawScope.drawAngleIndicator(
    angle: Float,
    radius: Float,
    strokeWidth: Float,
    color: Color
) {
    // Canvas角度系统：0°在右侧，顺时针为正
    // 我们需要将顶部设为0°，所以起始角度为270°（顶部）
    val startAngle = 270f

    // 计算扫过的角度
    // 正数角度向右（顺时针）延伸，负数角度向左（逆时针）延伸
    val sweepAngle = angle

    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
        size = Size(
            width = (radius * 2) - strokeWidth,
            height = (radius * 2) - strokeWidth
        ),
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}
