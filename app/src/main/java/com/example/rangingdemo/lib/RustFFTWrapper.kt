package com.example.rangingdemo.lib

import com.example.rangingdemo.complex.Complex32Array

object RustFFTWrapper {
    private val lib = LibRustFFT.INSTANCE

    private fun process(input: Complex32Array, inverse: Boolean) {
        val inputSize = input.size
        lib.try_fft(input.inner, inputSize, inverse)
    }

    fun fft(input: Complex32Array): Complex32Array {
        val cpxArray = input.clone()
        process(cpxArray, false)
        return cpxArray
    }

    fun ifft(input: Complex32Array): Complex32Array {
        val cpxArray = input.clone()
        process(cpxArray, true)
        return cpxArray
    }

    /**
     * 原地fft
     */
    fun fftInPlace(input: Complex32Array) {
        process(input, false)
    }

    /**
     * 原地ifft
     */
    fun ifftInPlace(input: Complex32Array) {
        process(input, true)
    }
}