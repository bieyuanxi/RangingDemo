package com.example.rangingdemo.complex

/**
 * 紧凑复数数组
 *
 * 内存布局：[[real0, imag0, real1, imag1, ...]]
 */
class Complex32Array(val size: Int) {
    //两倍空间交替存储real部分和imag部分
    var inner = FloatArray(size shl 1)

    // 设置第index个复数
    operator fun set(index: Int, complex: Complex32) {
        set(index, complex.real, complex.imag)
    }

    // 获取第index个复数
    operator fun get(index: Int): Complex32 {
        return Complex32(inner[index shl 1], inner[(index shl 1) + 1])
    }

    fun set(index: Int, real: Float, imag: Float) {
        inner[index shl 1] = real
        inner[(index shl 1) + 1] = imag
    }

    fun shiftLeft(shift: Int): Complex32Array {
        val n = inner.size
        var actualShift = (shift shl 1) % n
        if (actualShift < 0) {
            actualShift += n
        }

        inner.reverse(0, actualShift)
        inner.reverse(actualShift, n)
        inner.reverse()

        return this
    }

    fun shiftRight(shift: Int): Complex32Array {
        return shiftLeft(-shift)
    }

    fun toRealFloatArray(): FloatArray {
        val arr = FloatArray(size)
        arr.forEachIndexed { index, _ -> arr[index] = inner[(index shl 1)] }
        return arr
    }

    fun toImagFloatArray(): FloatArray {
        val arr = FloatArray(size)
        arr.forEachIndexed { index, _ -> arr[index] = inner[(index shl 1) + 1] }
        return arr
    }

    fun clone(): Complex32Array {
        val result = Complex32Array(size)
        result.inner = inner.copyOf()
        return result
    }
}

