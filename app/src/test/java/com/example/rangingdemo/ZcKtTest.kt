package com.example.rangingdemo

import org.junit.Test

import org.junit.Assert.*
import kotlin.math.abs
import kotlin.math.sqrt

class ZcKtTest {

    @Test
    fun genZCSequence() {
    }

    @Test
    /// ZC 序列的幅度是恒定的
    fun test1() {
        val u = 2
        val q = 257
        val N = 128
        val zcSequence = genZCSequence(u, q, N)
        for (i in 0 until zcSequence.size) {
            assert(abs(zcSequence[i].abs() - 1.0f) < 1e-7)
        }
    }

    @Test
    // ZC 序列具有良好的自相关特性，同一 ZC 序列的不同循环移位版本之间的自相关性接近零，
    // 仅在零移位（即完全相同）时，自相关性达到峰值
    // 自相关特性验证
    fun testAutoCorrelation() {
        val u = 1
        val q = 257 // 素数
        val N = q
        val zcSequence = genZCSequence(u, q, N)

        // 检查 k = 0 时的自相关
        val autoCorrZero = autoCorrelation(zcSequence, 0)
        println("Auto-correlation at k = 0: ${autoCorrZero.real} + ${autoCorrZero.imag}j")
        assert(abs(autoCorrZero.abs() - N) < 1e-7)
        assert(abs(autoCorrZero.imag - 0) < 1e-7)

        // 检查 k = 1 时的自相关
        val autoCorrOne = autoCorrelation(zcSequence, 1)
        println("Auto-correlation at k = 1: ${autoCorrOne.real} + ${autoCorrOne.imag}j")
        assert(autoCorrOne.abs() < 1e-4)
        val autoCorrN = autoCorrelation(zcSequence, 10)
        println("Auto-correlation at k = N: ${autoCorrN.real} + ${autoCorrN.imag}j")
        assert(autoCorrN.abs() < 1e-4)
    }

    @Test
    // ZC 序列具有良好的自相关特性，同一 ZC 序列的不同循环移位版本之间的自相关性接近零，
    // 仅在零移位（即完全相同）时，自相关性达到峰值
    // 自相关特性验证
    fun testAutoCorrelation64() {
        val u = 1
        val q = 257 // 素数
        val N = q
        val zcSequence = genZCSequence64(u, q, N)

        // 检查 k = 0 时的自相关
        val autoCorrZero = autoCorrelation(zcSequence, 0)
        println("Auto-correlation at k = 0: ${autoCorrZero.real} + ${autoCorrZero.imag}j")
        assert(abs(autoCorrZero.abs() - N) < 1e-10)
        assert(abs(autoCorrZero.imag - 0) < 1e-10)

        // 检查 k = 1 时的自相关
        val autoCorrOne = autoCorrelation(zcSequence, 1)
        println("Auto-correlation at k = 1: ${autoCorrOne.real} + ${autoCorrOne.imag}j")
        assert(autoCorrOne.abs() < 1e-10)
        val autoCorrN = autoCorrelation(zcSequence, 10)
        println("Auto-correlation at k = N: ${autoCorrN.real} + ${autoCorrN.imag}j")
        assert(autoCorrN.abs() < 1e-10)
    }

    @Test
    // 低互相关特性: 当q=N时 互相关模长为 sqrt(N)
    fun testCrossCorrelation() {
        // u1 u2互质
        val u1 = 2
        val u2 = 3
        val q = 257 // q为质数
        val N = q   // 当q=N时 互相关模长为 sqrt(N)
        val zcSequence1 = genZCSequence(u1, q, N)
        val zcSequence2 = genZCSequence(u2, q, N)
        // 检查 k = 0 时的互相关
        val crossCorrZero = crossCorrelation(zcSequence1, zcSequence2, 0)
        println("Cross-correlation at k = 0: ${crossCorrZero.real} + ${crossCorrZero.imag}j")
        assert(abs(crossCorrZero.abs() - sqrt(N.toFloat())) < 1e-5)
        // 检查 k = 1 时的互相关
        val crossCorrOne = crossCorrelation(zcSequence1, zcSequence2, 1)
        println("Cross-correlation at k = 1: ${crossCorrOne.real} + ${crossCorrOne.imag}j")
        assert(abs(crossCorrOne.abs() - sqrt(N.toFloat())) < 1e-5)
    }

    @Test
    // 低互相关特性: 当q=N时 互相关模长为 sqrt(N)
    fun testCrossCorrelation64() {
        // u1 u2互质
        val u1 = 2
        val u2 = 3
        val q = 257 // q为质数
        val N = q   // 当q=N时 互相关模长为 sqrt(N)
        val zcSequence1 = genZCSequence64(u1, q, N)
        val zcSequence2 = genZCSequence64(u2, q, N)

        // 检查 k = 0 时的互相关
        val crossCorrZero = crossCorrelation(zcSequence1, zcSequence2, 0)
        println("Cross-correlation at k = 0: ${crossCorrZero.real} + ${crossCorrZero.imag}j")
        assert(abs(crossCorrZero.abs() - sqrt(N.toDouble())) < 1e-10)

        // 检查 k = 1 时的互相关
        val crossCorrOne = crossCorrelation(zcSequence1, zcSequence2, 1)
        println("Cross-correlation at k = 1: ${crossCorrOne.real} + ${crossCorrOne.imag}j")
        assert(abs(crossCorrOne.abs() - sqrt(N.toDouble())) < 1e-10)
    }
}