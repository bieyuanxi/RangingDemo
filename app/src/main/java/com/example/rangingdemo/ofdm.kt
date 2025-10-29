package com.example.rangingdemo

import com.example.rangingdemo.complex.Complex32Array
import com.example.rangingdemo.lib.RustFFTWrapper
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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

    RustFFTWrapper.ifftInPlace(X)
    return X
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
    RustFFTWrapper.ifftInPlace(CFR) // cir = ifft(CFR)
    return CFR  // cir
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

/**
 * @param m 第1次测距长度，单位：m
 * @param n 第2次测距长度，单位：m
 * @param radians 旋转弧度
 * @param lenOfPhone 手机长度（若按中心旋转则为手机长度的一半），单位：m
 * @return 外部设备相对于手机第一次测距时的弧度
 */
fun getRadians(m: Double, n: Double, radians: Double, lenOfPhone: Double = 0.08): Double {
    // A(x, y)
    // (0, r) + CA = (x, y) => CA = (x, y - r)
    // (r*sinφ, r*cosφ) + DA = (x, y)   => DA = (x - r*sinφ, y - r*cosφ)
    // |CA|^2 = x^2 + (y - r)^2 = m^2                   => x^2 + y^2 = m^2 + 2yr - r^2                  @1
    // |DA|^2 = (x - r*sinφ)^2 + (y - r*cosφ)^2 = n^2   => x^2 + y^2 = n^2 + 2yr*cosφ + 2xr*sinφ - r^2  @2
    // @2 - @1 = 2yr*cosφ + 2xr*sinφ - 2yr + n^2 - m^2 = 0    => 2yr(1 - cosφ) - 2xr*sinφ = n^2 - m^2   (φ != 0)

    // x = y(1-cosφ)/sinφ + (m^2 - n^2)/(2r*sinφ)
    // y = xsinφ / (1 - cosφ) + (n^2 - m^2) / (2r(1 - cosφ))
    // x^2 = y^2*(1-cosφ)^2/(sinφ^2) + 2y(1-cosφ)(m^2 - n^2)/sinφ/(2r*sinφ) + (m^2 - n^2)^2/(2r*sinφ)^2
    //     = y^2*(1-cosφ)^2/(sinφ^2) + y(1-cosφ)(m^2 - n^2)/(rsinφ^2) + (m^2 - n^2)^2/(2r*sinφ)^2
    // @1: x^2 + y^2 - 2yr + r^2 - m^2 = 0
    // y^2*((1-cosφ)^2/(sinφ^2) + 1) + y((1-cosφ)(m^2 - n^2)/(rsinφ^2) - 2r) + (m^2 - n^2)^2/(2r*sinφ)^2 + r^2 - m^2 = 0
    val cosPhi = cos(radians)
    val sinPhi = sin(radians)
    val a = (1 - cosPhi).pow(2)/sinPhi.pow(2) + 1.0
    val b = (1 - cosPhi)*(m.pow(2) - n.pow(2))/(lenOfPhone*(sinPhi.pow(2))) - 2*lenOfPhone
    val c = (m.pow(2) - n.pow(2)).pow(2)/(2*lenOfPhone*sinPhi).pow(2) + lenOfPhone.pow(2) - m.pow(2)
    val delta = b.pow(2) - 4 * a * c
    if (delta < 0) {
        return 0.0
    }
    val y1 = (-b + sqrt(delta))/(2*a)
    val y2 = (-b - sqrt(delta))/(2*a)
    val x1 = y1 * (1 - cosPhi) / sinPhi + (m.pow(2) - n.pow(2))/(2*lenOfPhone*sinPhi)
    val x2 = y2 * (1 - cosPhi) / sinPhi + (m.pow(2) - n.pow(2))/(2*lenOfPhone*sinPhi)
    println(Pair(x1, y1))
    println(Pair(x2, y2))
    if(y1 < lenOfPhone || y1 > m + lenOfPhone) {
        return atan2(x2, y2)
    }
    return atan2(x1, y1)
}