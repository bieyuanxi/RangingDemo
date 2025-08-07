package com.example.rangingdemo

/**
 * 双精度浮点复数表示
 *
 * 频繁创建和销毁普通对象可能存在性能问题，目前仅用于算法正确性验证
 */
data class Complex64(var real: Double, var imag: Double) {
    fun abs() = kotlin.math.sqrt(real * real + imag * imag)

    // 考虑到普通对象在做计算时会创建对象，因此不重载各运算符
}