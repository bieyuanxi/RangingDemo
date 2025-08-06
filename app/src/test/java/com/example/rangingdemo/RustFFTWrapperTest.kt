package com.example.rangingdemo

import org.junit.Test

import org.junit.Assert.*

class RustFFTWrapperTest() {
    init {
        System.setProperty("jna.library.path", "native-libs")
    }

    @Test
    fun it_works() {
        val input = ComplexArray(4)
        input[0] = Complex(1.0f, 2.0f)
        input[1] = Complex(3.0f, 4.0f)
        input[2] = Complex(5.0f, 6.0f)
        input[3] = Complex(7.0f, 8.0f)

        val result = ComplexArray(4)
        result[0] = Complex(16f, 20f)
        result[1] = Complex(-8f, 0f)
        result[2] = Complex(-4f, -4f)
        result[3] = Complex(0f, -8f)

        assertEquals(true, RustFFTWrapper.fft(input).inner.contentEquals(result.inner))
    }

    @Test
    fun fft() {
        TODO()
    }

    @Test
    fun ifft() {
        TODO()
    }
}