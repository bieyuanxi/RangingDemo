package com.example.rangingdemo.complex

import org.junit.Test

import org.junit.Assert.*

class Complex32ArrayTest {

    @Test
    fun setAndGet() {
        val array = Complex32Array(20)
        array[0] = Complex32(0f, 1f)
        array[1] = Complex32(2f, 3f)
        array[2] = Complex32(4f, 5f)

        assertEquals(array[0], Complex32(0f, 1f))
        assertEquals(array[1], Complex32(2f, 3f))
        assertEquals(array[2], Complex32(4f, 5f))

        array[0] = Complex32(-1f, 2f)
        array[1] = Complex32(0f, 3f)

        assertEquals(array[0], Complex32(-1f, 2f))
        assertEquals(array[1], Complex32(0f, 3f))
    }

    @Test
    fun zeros() {
        val array = Complex32Array(20)
        for (i in 0 until array.size) {
            assertEquals(Complex32(0f, 0f), array[0])
        }
    }

    @Test
    fun largeSize() {
        val array = Complex32Array(4096)
        for (i in 0 until array.size) {
            assertEquals(Complex32(0f, 0f), array[0])
        }
    }

    @Test
    fun testShiftLeft() {
        val arr0 = arrayOf(
            Complex32(0f, 1.0f),
            Complex32(2f, 3.0f),
            Complex32(4f, 5.0f),
            Complex32(6f, 7.0f),
            Complex32(8f, 9.0f),
            Complex32(10f, 11.0f),
            Complex32(12f, 13.0f),
            Complex32(14f, 15.0f),
            Complex32(16f, 17.0f),
        )

        val array = Complex32Array(arr0.size)

        for (i in 0 until array.size) {
            array[i] = arr0[i]
        }

        val shift = 3
        array.shiftLeft(shift)

        for (i in 0 until array.size) {
            assertEquals(arr0[(i + shift) % array.size], array[i])
        }

    }

    @Test
    fun testShiftRight() {
        val arr0 = arrayOf(
            Complex32(0f, 1.0f),
            Complex32(2f, 3.0f),
            Complex32(4f, 5.0f),
            Complex32(6f, 7.0f),
            Complex32(8f, 9.0f),
            Complex32(10f, 11.0f),
            Complex32(12f, 13.0f),
            Complex32(14f, 15.0f),
            Complex32(16f, 17.0f),
        )

        val array = Complex32Array(arr0.size)

        for (i in 0 until array.size) {
            array[i] = arr0[i]
        }

        val shift = 4
        array.shiftRight(shift)

        for (i in 0 until array.size) {
            assertEquals(arr0[(array.size - shift + i) % array.size], array[i])
        }

    }

    @Test
    fun testToRealFloatArray() {
        val arr0 = arrayOf(
            Complex32(0f, 1.0f),
            Complex32(2f, 3.0f),
            Complex32(4f, 5.0f),
            Complex32(6f, 7.0f),
            Complex32(8f, 9.0f),
            Complex32(10f, 11.0f),
            Complex32(12f, 13.0f),
            Complex32(14f, 15.0f),
            Complex32(16f, 17.0f),
        )

        val array = Complex32Array(arr0.size)

        for (i in 0 until array.size) {
            array[i] = arr0[i]
        }

        val realArray =  array.toRealFloatArray()

        for (i in 0 until array.size) {
            assertEquals(realArray[i], arr0[i].real)
        }

    }

    @Test
    fun testToImagFloatArray() {
        val arr0 = arrayOf(
            Complex32(0f, 1.0f),
            Complex32(2f, 3.0f),
            Complex32(4f, 5.0f),
            Complex32(6f, 7.0f),
            Complex32(8f, 9.0f),
            Complex32(10f, 11.0f),
            Complex32(12f, 13.0f),
            Complex32(14f, 15.0f),
            Complex32(16f, 17.0f),
        )

        val array = Complex32Array(arr0.size)

        for (i in 0 until array.size) {
            array[i] = arr0[i]
        }

        val ImagArray =  array.toImagFloatArray()

        for (i in 0 until array.size) {
            assertEquals(ImagArray[i], arr0[i].imag)
        }

    }
}