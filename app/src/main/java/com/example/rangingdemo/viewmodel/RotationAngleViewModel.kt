package com.example.rangingdemo.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.rangingdemo.RotationSensorManager
import kotlinx.coroutines.flow.StateFlow

class RotationAngleViewModel(application: Application): AndroidViewModel(application) {
    private val appContext: Context
        get() = getApplication<Application>().applicationContext

    private val sensorManager: SensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var rotationSensor: Sensor? = null
    private val rotationSensorManager = RotationSensorManager()

    val rotationAngle: StateFlow<Float> = rotationSensorManager.rotationAngle

    /**
     * 初始化并立刻开始监听
     */
    init {
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        if (rotationSensor == null) {
            Log.e("rotationSensor", "rotationSensor = null!")
        }
        startListening()
    }

    // 校准当前角度为0度
    fun calibrate() {
        rotationSensorManager.calibrate()
    }

    // 注册传感器监听
    private fun startListening() {
        sensorManager.registerListener(
            rotationSensorManager,
            rotationSensor,
            SensorManager.SENSOR_DELAY_GAME // 适合游戏和实时应用的延迟
        )
    }

    // 取消传感器监听
    private fun stopListening() {
        sensorManager.unregisterListener(rotationSensorManager)
    }

    override fun onCleared() {
        super.onCleared()

        stopListening()
    }
}