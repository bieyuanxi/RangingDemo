package com.example.rangingdemo.viewmodel

import androidx.lifecycle.ViewModel
import com.example.rangingdemo.conjugate
import com.example.rangingdemo.frequencyRearrange
import com.example.rangingdemo.genZCSequence
import com.example.rangingdemo.lib.RustFFTWrapper

class ZcViewModel: ViewModel() {
    var zc = genZCSequence(1, 81, 81)
    var ZC = RustFFTWrapper.fft(zc)
    var ZC_hat = frequencyRearrange(ZC)
    var ZC_hat_prime = conjugate(ZC_hat)
}