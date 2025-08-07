package com.example.rangingdemo.lib

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface LibRustFFT : Library {
    companion object {
        val INSTANCE: LibRustFFT by lazy {
            Native.load("rust_fft_wrapper", LibRustFFT::class.java)
        }
    }
    fun fft_forward(input: Pointer, output: Pointer, n: Int, inverse: Boolean)

    fun add(a: Int, b: Int): Int
}