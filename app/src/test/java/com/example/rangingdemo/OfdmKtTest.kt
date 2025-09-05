package com.example.rangingdemo

import com.example.rangingdemo.lib.LibRustFFT
import com.example.rangingdemo.lib.RustFFTWrapper
import org.junit.Test

import org.junit.Assert.*
import kotlin.math.abs
import kotlin.system.measureTimeMillis

class OfdmKtTest {
    init {
        System.setProperty("jna.library.path", "native-libs")
    }

    @Test
    fun testModulate() {
        val timeTaken = measureTimeMillis {
            val zc = genZCSequence(1, 81, 81)
            val ZC = RustFFTWrapper.fft(zc)
            val ZC_hat = frequencyRearrange(ZC)
            val result = modulate(ZC_hat, 960, 19000, 48000)
            println(getMaxIndexedValue(result.toRealFloatArray()))
            for (i in 0 until result.size) {
//                println(result[i])
                assertEquals(true, abs(result[i].imag) < 1e-6)
            }
        }
        println(timeTaken)
    }

    @Test
    fun testModulate1() {
        val N = 48 * 40
        val zc = genZCSequence(1, 37, 37)
        val ZC = RustFFTWrapper.fft(zc)
        val ZC_hat = frequencyRearrange(ZC)
        val ZC_hat_prime = conjugate(ZC_hat)
        val N_prime = N
        val x = modulate(ZC_hat, N, 19000, 48000)
        for (i in 0 until x.size) {
//                println(result[i])
            assertEquals(true, abs(x[i].imag) < 1e-6)
        }
        println(getMaxIndexedValue(x.toRealFloatArray()))
        val shift = 47
        val y = shiftLeft(x.toRealFloatArray(), shift)

        val cir = demodulate(floatArray2ComplexArray(y), ZC_hat_prime, N_prime, 19000, 48000)
        val mag = magnitude(cir)
        val (index, value) = getMaxIndexedValue(mag)
        println(Pair(index, value))
        assertEquals(N_prime, shift + index)
    }

//    @Test
//    fun testMagnitude32() {
//        val timeTaken = measureTimeMillis {
//            val zc = genZCSequence(1, 81, 81)
//            val ZC = LibRustFFT.INSTANCE.magnitude32()
//            val ZC_hat = frequencyRearrange(ZC)
//            val result = modulate(ZC_hat, 960, 19000, 48000)
//
//            for (i in 0 until result.size) {
////                println(result[i])
//                assertEquals(true, abs(result[i].imag) < 1e-6)
//            }
//        }
//        println(timeTaken)
//    }
}