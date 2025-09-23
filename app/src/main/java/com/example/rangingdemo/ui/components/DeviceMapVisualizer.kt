package com.example.rangingdemo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.absoluteValue

// 数据类：存储其他手机的「距离」和「角度」（角度单位：度）
data class DeviceInfo(
    val distance: Float,   // 与自身的距离
    val angle: Float,      // 与自身的角度（0度→右，90度→上，180度→左，270度→下）
    val deviceName: String  // 设备标识（用于标签显示）
)

/**
 * 设备"地图"可视化组件
 * @param deviceInfos 其他手机列表：[(距离1, 角度1), (距离2, 角度2), ...]
 * @param currentAngle 当前手机角度: (-180, 180)
 * @param selfRadius 自身手机圆的半径（默认24dp）
 * @param otherRadius 其他手机圆的半径（默认18dp）
 */
@Composable
fun DeviceMapVisualizer(
    deviceInfos: List<DeviceInfo>,
    currentAngle: Float,
    modifier: Modifier = Modifier,
    selfRadius: Dp = 24.dp,
    otherRadius: Dp = 18.dp
) {
    val textMeasurer = rememberTextMeasurer()
    // 获取当前的密度信息，用于dp转px
    val density = LocalDensity.current

    // 将dp转换为像素值
    val selfRadiusPx = with(density) { selfRadius.toPx() }
    val otherRadiusPx = with(density) { otherRadius.toPx() }
    val lineWidthPx = with(density) { 2.dp.toPx() }

    Canvas(modifier) {
        // 计算屏幕中心（自身手机的位置）
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // 计算缩放因子（确保所有其他手机都能在屏幕内显示）
        val maxDistance = if (deviceInfos.isNotEmpty()) deviceInfos.maxOf { it.distance } else 100f
        val maxAvailableDistance = max(centerX, centerY) * 0.4f // 屏幕最大可用距离（避免超出屏幕）
        val scale = if (maxDistance > 0) maxAvailableDistance / maxDistance else 1f

        // 遍历绘制「其他手机」和「连接线」
        deviceInfos.forEach { device ->
            val angleOffset = device.angle - currentAngle
            // 角度转弧度
            val angleRadians = angleOffset * PI / 180f - PI / 2

            // 计算其他手机的屏幕坐标（极坐标→笛卡尔坐标）
            val otherX = centerX + (device.distance * cos(angleRadians) * scale).toFloat()
            val otherY = centerY + (device.distance * sin(angleRadians) * scale).toFloat()

            // 绘制「连接线」（自身 → 其他手机）
            drawLine(
                color = if (angleOffset.absoluteValue < 1f) Color.Green else Color(0xFF9E9E9E), // 灰色：连接线
                start = Offset(centerX, centerY),
                end = Offset(otherX, otherY),
                strokeWidth = lineWidthPx
            )

            // 绘制「其他手机」（小圆）
            drawCircle(
                color = if (angleOffset.absoluteValue < 1f) Color.Green else Color(0xFFFF5722), // 橙色：其他设备
                radius = otherRadiusPx,
                center = Offset(otherX, otherY)
            )

            // 绘制其他手机标签
            val deviceText = textMeasurer.measure(device.deviceName)
            drawText(
                textLayoutResult = deviceText,
                color = Color.White,
                topLeft = Offset(
                    x = otherX - deviceText.size.width / 2f,
                    y = otherY - deviceText.size.height / 2f
                )
            )
        }

        // 绘制自身手机（中心圆）
        drawCircle(
            color = Color(0xFF2196F3), // 蓝色：自身
            radius = selfRadiusPx,
            center = Offset(centerX, centerY)
        )
        // 绘制自身标签
        val selfText = textMeasurer.measure("Self")
        drawText(
            textLayoutResult = selfText,
            color = Color.White,
            topLeft = Offset(
                x = centerX - selfText.size.width / 2f,
                y = centerY - selfText.size.height / 2f
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PhoneNetworkExample() {
    // 输入数据：[(距离, 角度), ...]
    val otherPhones = listOf(
        DeviceInfo(distance = 150f, angle = 0f, deviceName = "A"),
        DeviceInfo(distance = 150f, angle = -90f, deviceName = "B"),
        DeviceInfo(distance = 120f, angle = 90f, deviceName = "C"),
        DeviceInfo(distance = 80f, angle = 315f, deviceName = "D")
    )

    DeviceMapVisualizer(
        deviceInfos = otherPhones,
        currentAngle = 0f,
        modifier = Modifier.fillMaxSize()
    )
}
