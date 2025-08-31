package com.example.rangingdemo

import com.example.rangingdemo.lib.RustFFTWrapper

// 采样率
val f_s = 48000
val N = 48 * 40 // 40ms
val N_prime = N
val zc = genZCSequence(1, 37, 37)
val ZC = RustFFTWrapper.fft(zc)
val ZC_hat = frequencyRearrange(ZC)
val ZC_hat_prime = conjugate(ZC_hat)