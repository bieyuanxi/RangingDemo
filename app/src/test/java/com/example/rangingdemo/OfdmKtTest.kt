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

            for (i in 0 until result.size) {
//                println(result[i])
                assertEquals(true, abs(result[i].imag) < 1e-6)
            }
        }
        println(timeTaken)
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