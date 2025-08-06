package com.example.rangingdemo

/**
 * 紧凑复数数组
 *
 * 内存布局：[[real0, imag0, real1, imag1, ...]]
 */
class ComplexArray(val size: Int) {
    //两倍空间交替存储real部分和imag部分
    private val inner = FloatArray(size shl 1)

    // 设置第index个复数
    operator fun set(index: Int, complex: Complex) {
        set(index, complex.real, complex.imag)
    }

    // 获取第index个复数
    operator fun get(index: Int): Complex {
        return Complex(inner[index shl 1], inner[(index shl 1) + 1])
    }

    fun set(index: Int, real: Float, imag: Float) {
        inner[index shl 1] = real
        inner[(index shl 1) + 1] = imag
    }
}

//class ComplexArray(size: Int) {
//    private val inner = FloatArray(2 * size)
//
//    // 实部访问器
//    val real = Accessor { index -> 2 * index }
//
//    // 虚部访问器
//    val imag = Accessor { index -> 2 * index + 1 }
//
//    // 自定义访问器类，仅提供 get 和 set
//    inner class Accessor(private val indexMapper: (Int) -> Int) {
//        operator fun get(index: Int): Float {
//            return inner[indexMapper(index)]
//        }
//
//        operator fun set(index: Int, value: Float) {
//            inner[indexMapper(index)] = value
//        }
//    }
//}