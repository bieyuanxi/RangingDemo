package com.example.rangingdemo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun genZCSequence(u: Int, q: Int, N: Int = q): Complex32Array {
    assert(N <= q)
    val sequence = Complex32Array(N)
    for (n in 0 until N) {
        // 计算 ZC 序列元素的公式 exp(-j * pi * u * n * (n + 1) / q)
        val phase = -PI * u * n * (n + 1) / q
        val real = cos(phase).toFloat()
        val imag = sin(phase).toFloat()
        sequence[n] = Complex32(real, imag)
    }
    return sequence
}

fun genZCSequence64(u: Int, q: Int, N: Int = q): Complex64Array {
    assert(N <= q)
    val sequence = Complex64Array(N)
    for (n in 0 until N) {
        // 计算 ZC 序列元素的公式 exp(-j * pi * u * n * (n + 1) / q)
        val phase = -PI * u * n * (n + 1) / q
        sequence.real[n] = cos(phase)
        sequence.imag[n] = sin(phase)
    }
    return sequence
}



// 自相关运算
fun autoCorrelation(sequence: Complex32Array, k: Int): Complex32 {
    val n = sequence.size
    var result = Complex32(0.0f, 0.0f)
    for (i in 0 until n) {
        val index = (i + k) % n
        result += sequence[i] * sequence[index].conjugate()
    }
    return result
}

// 自相关运算
fun autoCorrelation(sequence: Complex64Array, k: Int): Complex64 {
    return crossCorrelation(sequence, sequence, k)
}

// 互相关运算
fun crossCorrelation(sequence1: Complex32Array, sequence2: Complex32Array, k: Int): Complex32 {
    val N = sequence1.size
    var result = Complex32(0.0f, 0.0f)
    for (n in 0 until N) {
        val index = (n + k) % N
        result += sequence1[n] * sequence2[index].conjugate()
    }
    return result
}

// 互相关运算
fun crossCorrelation(s1: Complex64Array, s2: Complex64Array, k: Int): Complex64 {
    val N = s1.size
    val result = Complex64(0.0, 0.0)
    for (i in 0 until N) {
        val index = (i + k) % N
        result.real += s1.real[i] * s2.real[index] + s1.imag[i] * s2.imag[index]
        result.imag += s1.imag[i] * s2.real[index] - s1.real[i] * s2.imag[index]
    }
    return result
}