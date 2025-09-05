package com.example.rangingdemo

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

    return pcmData
}

/**
 * 将复数数组转成双声道音频
 * 只会记录实数部分，忽略虚数部分
 * @param leftArray 左声道复数数组
 * @param rightArray 右声道复数数组，只传入左声道默认左右声道数据相同
 * @param leftRate 左声道倍率
 * @param rightRate 右声道倍率
 */
fun complexArray2StereoFloatArray(leftArray: Complex32Array, rightArray: Complex32Array = leftArray,leftRate: Float = 1.0f, rightRate: Float = 1.0f): FloatArray {
    assert(leftArray.size == rightArray.size)
    val stereoAudioData = FloatArray(leftArray.size * 2)
    for (i in 0 until leftArray.size) {
        stereoAudioData[2 * i] = leftRate * leftArray[i].real
        stereoAudioData[2 * i + 1] = rightRate * rightArray[i].real
    }
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
