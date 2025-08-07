package com.example.rangingdemo.complex

/**
 * 双精度浮点复数数组
 */
class Complex64Array(val size: Int) {
    private val inner = DoubleArray(2 * size)

    // 实部访问器
    val real = Accessor { index -> index shl 1 }

    // 虚部访问器
    val imag = Accessor { index -> (index shl 1) + 1 }

    // 自定义访问器类，仅提供 get 和 set
    inner class Accessor(private val indexMapper: (Int) -> Int) {
        operator fun get(index: Int): Double {
            return inner[indexMapper(index)]
        }

        operator fun set(index: Int, value: Double) {
            inner[indexMapper(index)] = value
        }
    }

    fun shiftLeft(shift: Int) {
        val n = inner.size
        var actualShift = (shift shl 1) % n
        if (actualShift < 0) {
            actualShift += n
        }

        inner.reverse(0, actualShift)
        inner.reverse(actualShift, n)
        inner.reverse()
    }

    fun shiftRight(shift: Int) {
        shiftLeft(-shift)
    }
}