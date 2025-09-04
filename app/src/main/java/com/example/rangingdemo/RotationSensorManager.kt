package com.example.rangingdemo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RotationSensorManager() : SensorEventListener {
    // 角度数据Flow
    private val _rotationAngle = MutableStateFlow(0f)
    val rotationAngle: StateFlow<Float> = _rotationAngle

    // 用于平滑处理的滑动窗口
    private val angleWindow = mutableListOf<Float>()
    private val windowSize = 5

    // 校准基准角度
    private var initialAngle: Float = 0f
    // 当前原始角度（未校准）
    private var currentRawAngle = 0f

    // 校准当前角度为0度
    fun calibrate() {
        // 使用当前原始角度作为新的基准
        initialAngle = currentRawAngle
        angleWindow.clear() // 清空滑动窗口
    }

    override fun onSensorChanged(event: SensorEvent) {
        when(event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR -> handleSensorEventGrv(event)
        }
    }

    private fun handleSensorEventGrv(event: SensorEvent) {
        // 从旋转矢量获取旋转矩阵
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        // 将旋转矩阵转换为方位角（azimuth, pitch, roll）
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)

        // orientation[0] 是方位角（弧度），绕Z轴旋转
        val azimuthRadians = orientation[0]
        var azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()
        Log.d("azimuthDegrees", "$azimuthDegrees")
        // 将角度调整到0-360度范围
        if (azimuthDegrees < 0) {
            azimuthDegrees += 360f
        }

        // 保存原始角度（用于校准）
        currentRawAngle = azimuthDegrees

        // 应用校准
        val calibratedAngle = initialAngle.let { initial ->
            var angle = azimuthDegrees - initial
            // 处理角度环绕（例如350° - 10° 应该是-20°而不是340°）
            if (angle > 180) angle -= 360
            if (angle < -180) angle += 360
            angle
        }

        // 应用滑动窗口滤波减少噪声
        angleWindow.add(calibratedAngle)
        if (angleWindow.size > windowSize) {
            angleWindow.removeAt(0)
        }

        // 计算窗口平均值并更新Flow
        val smoothedAngle = angleWindow.average().toFloat()
        _rotationAngle.value = smoothedAngle
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // 精度变化时可以在这里处理，通常不需要特别操作
    }
}