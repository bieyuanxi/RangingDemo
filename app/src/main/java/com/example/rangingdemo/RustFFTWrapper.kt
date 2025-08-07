package com.example.rangingdemo

import com.example.rangingdemo.complex.Complex32Array
import com.sun.jna.Memory

object RustFFTWrapper {
    private val lib = LibRustFFT.INSTANCE

    private fun process(input: Complex32Array, inverse: Boolean): Complex32Array {
        val inputSize = input.size
        val inputPtr = Memory(inputSize * 8L) // 每个Complex占8字节（2个float）
        inputPtr.write(0, input.inner, 0, input.inner.size)

        val outputPtr = Memory(inputSize * 8L)

        lib.fft_forward(inputPtr, outputPtr, inputSize, inverse)

        val output = Complex32Array(inputSize)
        outputPtr.read(0, output.inner, 0, output.inner.size)

        // Free the native memory and set peer to zero
        inputPtr.close()
        outputPtr.close()

        return output
    }

    fun fft(input: Complex32Array): Complex32Array {
        return process(input, false)
    }

    fun ifft(input: Complex32Array): Complex32Array {
        return process(input, true)
    }
}