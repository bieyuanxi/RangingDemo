package com.example.rangingdemo

import org.junit.Test

import org.junit.Assert.*

class ComplexArrayTest {

    @Test
    fun setAndGet() {
        val array = ComplexArray(20)
        array[0] = Complex(0f, 1f)
        array[1] = Complex(2f, 3f)
        array[2] = Complex(4f, 5f)

        assertEquals(array[0], Complex(0f, 1f))
        assertEquals(array[1], Complex(2f, 3f))
        assertEquals(array[2], Complex(4f, 5f))

        array[0] = Complex(-1f, 2f)
        array[1] = Complex(0f, 3f)

        assertEquals(array[0], Complex(-1f, 2f))
        assertEquals(array[1], Complex(0f, 3f))
    }

    @Test
    fun zeros() {
        val array = ComplexArray(20)
        for (i in 0 until array.size) {
            assertEquals(Complex(0f, 0f), array[0])
        }
    }

    @Test
    fun largeSize() {
        val array = ComplexArray(4096)
        for (i in 0 until array.size) {
            assertEquals(Complex(0f, 0f), array[0])
        }
    }
}