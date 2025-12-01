package com.example.rangingdemo

import android.util.Log
import com.example.rangingdemo.complex.Complex32
import com.example.rangingdemo.complex.Complex32Array
import com.example.rangingdemo.lib.LibRustFFT
import java.text.DecimalFormat
import kotlin.math.sin

/**
 * 生成指定频率(left和right)的立体声音频
 */
fun generateSimpleStereoAudio(
    durationMs: Int,
    leftFreq: Int,
    rightFreq: Int,
    sampleRate: Int
): FloatArray {
    assert(leftFreq * 2 <= sampleRate)
    assert(rightFreq * 2 <= sampleRate)

    // 计算总帧数（每帧包含左右声道各一个采样）
    val frameCount = (sampleRate * durationMs / 1000.0).toInt()
    // 总采样数 = 帧数 × 2（立体声道）
    val totalSamples = frameCount * 2
    val pcmData = FloatArray(totalSamples)

    var time = 0.0
    val timeStep = 1.0 / sampleRate

    for (i in 0 until frameCount) {
        // 左声道采样（偶数索引）
        pcmData[i * 2] = sin(2 * Math.PI * leftFreq * time).toFloat()
        // 右声道采样（奇数索引）
        pcmData[i * 2 + 1] = sin(2 * Math.PI * rightFreq * time).toFloat()

        time += timeStep
    }
    Log.d("simpleStereoAudio", getMaxIndexedValue(pcmData).toString())
    return pcmData
}

/**
 * 消耗Complex32Array，并将复数数组转成双声道音频数组
 * 复用内部数组,拷贝实数部分（左声道）到右声道
 * @param array 左声道复数数组，将被消耗，无法再使用
 * @param scale 归一化倍率
 */
fun consumeComplexArray2StereoFloatArray(array: Complex32Array, scale: Float = 0.7f): FloatArray {
    val stereoAudioData = array.inner
    array.clear()

    normalizeAudioHighPerformance(stereoAudioData, scale)
    return stereoAudioData
}

fun floatArray2ComplexArray(array: FloatArray): Complex32Array {
    val result = Complex32Array(array.size)
    for (i in array.indices) {
        result[i] = Complex32(array[i], 0f)
    }
    return result
}

/**
 * 返回复数数组的复数共轭数组
 */
fun conjugate(array: Complex32Array): Complex32Array {
    val result = Complex32Array(array.size)
    for (i in 0 until result.size) {
        result[i] = Complex32(array[i].real, -array[i].imag)
    }
    return result
}

/**
 * 求复数数组每个元素的模所组成的数组
 */
fun magnitude(array: Complex32Array): FloatArray {
    val result = FloatArray(array.size)
    LibRustFFT.INSTANCE.magnitude32(array.inner, array.size, result)
    return result
}

/**
 * 获取一个数组中最大值那一对
 * @return (index, value)
 */
fun getMaxIndexedValue(array: FloatArray): Pair<Int, Float> {
    assert(array.isNotEmpty())
    var result = Pair(0, array[0])
    for (i in array.indices) {
        if (result.second < array[i]) {
            result = Pair(i, array[i])
        }
    }
    return result
}


fun ns2ms(ns: Long) = (ns / 1_000_000.0f)

fun formatNumber(number: Number): String = DecimalFormat("#.00").format(number)


fun shiftLeft(x: FloatArray, shift: Int): FloatArray {
    val n = x.size
    var actualShift = shift % n
    while (actualShift < 0) {
        actualShift += n
    }
    val newArray = x.clone()
    newArray.reverse(0, actualShift)
    newArray.reverse(actualShift, n)
    newArray.reverse()

    return newArray
}


fun squareMatrix(n: Int): Array<Array<Int>> {
    return Array(n) {
        Array(n) { 0 }
    }
}



/**
 * 音频归一化到[-scale, scale]
 *
 * 循环 + 单遍遍历计算最大值 + 直接修改原数组
 */
fun normalizeAudioHighPerformance(audioData: FloatArray, scale: Float = 1.0f) {
    val length = audioData.size
    if (length == 0) return

    // 计算绝对值最大值
    var maxAbs = 0f
    for (i in 0 until length) {
        val absVal = kotlin.math.abs(audioData[i])
        if (absVal > maxAbs) {
            maxAbs = absVal
        }
    }

    // 静音判断（低于阈值直接置0）
    if (maxAbs < 1e-6f) {
        for (i in 0 until length) {
            audioData[i] = 0f
        }
        return
    }

    // 归一化到 [-scale, scale]
    val scale1 = scale / maxAbs
    for (i in 0 until length) {
        audioData[i] *= scale1
    }
}