package com.example.rangingdemo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rangingdemo.audio.StaticAudioPlayer
import kotlinx.coroutines.launch

class AudioTrackViewModel: ViewModel() {
    private val audioPlayer = StaticAudioPlayer()

    fun start(
        stereoAudioData: FloatArray,
        loopCount: Int = 0,
        sampleRate: Int = 48000
    ) {
        // 使用viewModelScope确保协程随ViewModel销毁而取消
        viewModelScope.launch {
            audioPlayer.startPlay(stereoAudioData, loopCount)
        }
    }

    /**
     * 停止音频播放
     */
    fun stop() {
        viewModelScope.launch {
            audioPlayer.stop()
        }
    }



    /**
     * ViewModel销毁时释放播放器资源
     */
    override fun onCleared() {
        super.onCleared()
    }
}