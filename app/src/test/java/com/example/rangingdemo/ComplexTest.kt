package com.example.rangingdemo

import org.junit.Test

import org.junit.Assert.*

class ComplexTest {

    @Test
    fun getReal() {
        val complex = Complex(3f, -4f)
        assertEquals(3f, complex.real)
    }

    @Test
    fun getImag() {
        val complex = Complex(3f, -4f)
        assertEquals(-4f, complex.imag)
    }

    @Test
    fun plus() {
        val c1 = Complex(3f, -4f)
        val c2 = Complex(3f, -4f)

        assertEquals(Complex(6f, -8f), c1 + c2)
    }

    @Test
    fun minus() {
        val c1 = Complex(3f, -4f)
        val c2 = Complex(15f, 5f)

        assertEquals(Complex(-12f, -9f), c1 - c2)

        assertEquals(Complex(0f, 0f), Complex(3f, -4f) - Complex(3f, -4f))
    }

    @Test
    fun times() {
        val c1 = Complex(3f, -4f)
        val c2 = Complex(3f, -4f)

        assertEquals(Complex(-7f, -24f), c1 * c2)
    }

    @Test
    fun zero() {
        // FIXME: 浮点数0f和-0f bit表示不同，因此转换成Complex里的long类型会导致不相等
//        assertEquals(Complex(0f, 0f), Complex(-0f, -0f))

        assert(Complex(0f, 0f).eq(Complex(-0f, -0f)))
    }


}