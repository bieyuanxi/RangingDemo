package com.example.rangingdemo.complex

import org.junit.Test

import org.junit.Assert.*

class Complex64ArrayTest {

    @Test
    fun shiftLeft() {
        val arr0 = arrayOf(
            Complex64(0.0, 1.0),
            Complex64(2.0, 3.0),
            Complex64(4.0, 5.0),
            Complex64(6.0, 7.0),
            Complex64(8.0, 9.0),
            Complex64(10.0, 11.0),
            Complex64(12.0, 13.0),
            Complex64(14.0, 15.0),
            Complex64(16.0, 17.0),
        )

        val array = Complex64Array(arr0.size)

        for (i in 0 until array.size) {
            array.real[i] = arr0[i].real
            array.imag[i] = arr0[i].imag
        }

        val shift = 3
        array.shiftLeft(shift)

        for (i in 0 until array.size) {
            assertEquals(arr0[(i + shift) % array.size].real, array.real[i], 1e-10)
            assertEquals(arr0[(i + shift) % array.size].imag, array.imag[i], 1e-10)
        }
    }

    @Test
    fun shiftRight() {
        val arr0 = arrayOf(
            Complex64(0.0, 1.0),
            Complex64(2.0, 3.0),
            Complex64(4.0, 5.0),
            Complex64(6.0, 7.0),
            Complex64(8.0, 9.0),
            Complex64(10.0, 11.0),
            Complex64(12.0, 13.0),
            Complex64(14.0, 15.0),
            Complex64(16.0, 17.0),
        )

        val array = Complex64Array(arr0.size)

        for (i in 0 until array.size) {
            array.real[i] = arr0[i].real
            array.imag[i] = arr0[i].imag
        }

        val shift = 4
        array.shiftRight(shift)

        for (i in 0 until array.size) {
            assertEquals(arr0[(array.size - shift + i) % array.size].real, array.real[i], 1e-10)
            assertEquals(arr0[(array.size - shift + i) % array.size].imag, array.imag[i], 1e-10)
        }
    }
}