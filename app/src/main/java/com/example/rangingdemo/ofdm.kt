package com.example.rangingdemo

import com.example.rangingdemo.complex.Complex32Array
import com.example.rangingdemo.lib.RustFFTWrapper

/**
 * Frequency domain rearrange by half
 *
 * @param ZC ZC = dft(zc)
 * @return a new array of ZC_hat
 */
fun frequencyRearrange(ZC: Complex32Array): Complex32Array {
    assert(ZC.size % 2 == 1)
    return ZC.clone().shiftRight(ZC.size / 2)
}

/**
 * 调制算法
 *
 * @param ZC_hat ZC_hat = frequency_rearrange(ZC), ZC = dft(zc)
 * @param N 帧长，以采样率为基本单位，如帧长20ms，则 N = 20 * 0.001 * f_s
 * @param f_c carrier frequency 载波频率
 * @param f_s 采样频率
 * @return 浮点数组，调制好的声波序列（应该是一个实数序列）
 */
fun modulate(ZC_hat: Complex32Array, N: Int = 960, f_c: Int = 19000, f_s: Int = 48000, ): Complex32Array {
    val Nzc = ZC_hat.size
    val h_zc = Nzc / 2
    val n_c = N * f_c / f_s
    val X = Complex32Array(N)
    for(i in 0 until Nzc) {
        X[i + n_c - h_zc] = ZC_hat[i]
    }

    for(i in (N / 2 + 1) until N) {
        X[i] = X[N - i].conjugate()
    }

    return RustFFTWrapper.ifft(X)
}


/**
 * 解调算法
 * @param y Received signal sequence
 * @param ZC_hat_prime ZC_hat_prime = ZC_hat.conjugate()
 * @param N_prime
 * @return cir
 */
fun demodulate(y: Complex32Array, ZC_hat_prime: Complex32Array, N_prime: Int, f_c: Int = 19000, f_s: Int = 48000): Complex32Array {
    val N = y.size
    val n_c = N * f_c / f_s
    val N_zc = ZC_hat_prime.size
    val h_zc = N_zc / 2

    // perform N-point DFT
    val Y = RustFFTWrapper.fft(y)

    // conjugate multiplication
    val CFR_hat = Complex32Array(N_zc)
    for (i in 0 .. 2 * h_zc) {
        CFR_hat[i] = ZC_hat_prime[i] * Y[i + n_c - h_zc]
    }

    // Zero padding
    val CFR = Complex32Array(N_prime)
    for (i in 0 .. h_zc) {
        CFR[i] = CFR_hat[i + h_zc]
    }
    for (i in 0 until h_zc) {
//        CFR[N_prime - 1 - i] = CFR_hat[i] // 论文算法2中的代码，可能有误
        CFR[N_prime - h_zc + i] = CFR_hat[i]    // 根据论文Proof部分推断，应该是做循环位移
    }
    // perform N'-point IDFT
    return RustFFTWrapper.ifft(CFR)
}

/**
 * 根据四个下标获取距离
 * TODO: find better algorithm
 * @return 返回的距离为估计值，结果为=(dAB + dBA - dAA - dBB) / 2，结果应该比实际值偏小
 */
fun get_distance(
    m_aa: Int, m_ab: Int, m_bb: Int, m_ba: Int,
    N_prime: Int, c: Float = 343.0f, N: Int, f_s: Int = 48000
): Float {
    val m = m_aa + m_bb - m_ab - m_ba
    val range = c * N_prime / f_s

    for (i in -2..2) {
        val d = -(m + (i * N_prime)) * c * N / f_s / N_prime
        if (d in 0.0f..range) {
            return d / 2
        }
    }
    return -1.0f
}