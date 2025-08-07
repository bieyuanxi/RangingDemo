package com.example.rangingdemo

import com.example.rangingdemo.complex.Complex32
import com.example.rangingdemo.complex.Complex32Array
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
}