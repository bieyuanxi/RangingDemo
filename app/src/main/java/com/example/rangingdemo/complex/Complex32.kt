package com.example.rangingdemo.complex

/**
 * 复数表示，使用value class包装以尽量内联，使用一个Long存储两个Float（通过位运算）
 *
 * 内存布局为：(real, imag)
 *
 * 由于使用long进行包装，因此存在0f和-0f导致long值不等的问题，判断相等时应使用eq()方法
 * */
@JvmInline
value class Complex32(private val inner: Long) {
    constructor(real: Float, imag: Float) : this(
        // 将两个Float的二进制表示打包到Long中
        (real.toRawBits().toLong() shl 32) or (imag.toRawBits().toLong() and 0xFFFFFFFFL)
    )

    val real: Float
        get() = Float.fromBits((inner shr 32).toInt())

    val imag: Float
        get() = Float.fromBits(inner.toInt())

    // 复数加法
    operator fun plus(other: Complex32): Complex32 {
        return Complex32(real + other.real, imag + other.imag)
    }

    // 复数减法
    operator fun minus(other: Complex32): Complex32 {
        return Complex32(real - other.real, imag - other.imag)
    }

    // 复数乘法
    operator fun times(other: Complex32): Complex32 {
        val newReal = real * other.real - imag * other.imag
        val newImag = real * other.imag + imag * other.real
        return Complex32(newReal, newImag)
    }

    fun abs(): Float {
        return kotlin.math.sqrt(real * real + imag * imag)
    }

    fun conjugate(): Complex32 {
        return Complex32(real, -imag)
    }

    // 为了解决0f和-0f值相同但是编码不同导致Complex不相等的问题
    fun eq(other: Complex32): Boolean {
        return real == other.real && imag == other.imag
    }

    override fun toString(): String {
        return "Complex(real: $real, imag: $imag)"
    }
}