package com.example.rangingdemo.lib

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface LibRustFFT : Library {
    companion object {
        // 使用库方法时加载
        val INSTANCE: LibRustFFT by lazy {
            Native.load("rust_fft_wrapper", LibRustFFT::class.java)
        }
//        // 引用LibRustFFT.INSTANCE时加载
//        val INSTANCE: LibRustFFT = Native.load("rust_fft_wrapper", LibRustFFT::class.java)
    }
    fun fft_forward(input: Pointer, output: Pointer, n: Int, inverse: Boolean)

    fun add(a: Int, b: Int): Int
}