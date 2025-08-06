package com.example.rangingdemo

import com.sun.jna.Memory

object RustFFTWrapper {
    private val lib = LibRustFFT.INSTANCE

    private fun process(input: ComplexArray, inverse: Boolean): ComplexArray {
        val inputSize = input.size
        val inputPtr = Memory(inputSize * 8L) // 每个Complex占8字节（2个float）
        inputPtr.write(0, input.inner, 0, input.inner.size)

        val outputPtr = Memory(inputSize * 8L)

        lib.fft_forward(inputPtr, outputPtr, inputSize, inverse)

        val output = ComplexArray(inputSize)
        outputPtr.read(0, output.inner, 0, output.inner.size)

        // Free the native memory and set peer to zero
        inputPtr.close()
        outputPtr.close()

        return output
    }

    fun fft(input: ComplexArray): ComplexArray {
        return process(input, false)
    }

    fun ifft(input: ComplexArray): ComplexArray {
        return process(input, true)
    }
}