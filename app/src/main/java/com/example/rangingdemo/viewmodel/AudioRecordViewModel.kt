package com.example.rangingdemo.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rangingdemo.AudioRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class AudioRecordViewModel : ViewModel() {
    private val audioRecorder = AudioRecorder()

    private val _audioChannel = MutableStateFlow(Pair(floatArrayOf(), floatArrayOf()))
    val audioChannel: StateFlow<Pair<FloatArray, FloatArray>> = _audioChannel

    init {
        viewModelScope.launch {
            audioRecorder.audioDataFlow.collect { data ->
                val left = FloatArray(data.size / 2)
                val right = FloatArray(data.size / 2)
                data.forEachIndexed { index, fl ->
                    if (index % 2 == 0) {
                        left[index / 2] = fl
                    } else {
                        right[index / 2] = fl
                    }
                }
                _audioChannel.value = Pair(left, right)
//                Log.d("_audioChannel", "")
            }
        }

//        // Debug
//        viewModelScope.launch {
//            audioChannel.collect { data ->
//                val (l, r) = data
//                Log.d("audioChannel", "l = ${l.maxOrNull()}, r = ${r.maxOrNull()}")
//            }
//        }
    }

    fun start(frameLen: Int = 40 * 48) {
        viewModelScope.launch {
            audioRecorder.startRecording(frameLen)
        }

    }

    fun stop() {
        viewModelScope.launch {
            audioRecorder.stopRecording()
        }
    }
}
