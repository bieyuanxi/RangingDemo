package com.example.rangingdemo.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class AudioRecorder : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO // 音频录制是IO操作，使用IO调度器


    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    // 音频数据Flow（对外暴露不可变Flow）
    private val _audioDataFlow = MutableStateFlow(floatArrayOf())
    val audioDataFlow: StateFlow<FloatArray> = _audioDataFlow

    fun startRecording(frameLen: Int = 40 * 48) = launch {
        if (isRecording) return@launch
        isRecording = true

        // 初始化AudioRecord
        audioRecord = createDefaultAudioRecord(48000)
        audioRecord?.startRecording()

        Log.d("bufferSizeInFrames", "${audioRecord?.bufferSizeInFrames}")
        audioRecord?.let {
            assert(frameLen < it.bufferSizeInFrames)
        }

        val buffer = FloatArray(frameLen * 2) // 立体声需要×2
        while (isRecording) {
            // 读取浮点音频数据（返回值为实际读取的样本数）
            val readSize = audioRecord?.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
            if (readSize != null && readSize > 0) {
                // 发送数据到Flow（复制一份避免缓冲区覆盖）
                _audioDataFlow.value = buffer.copyOf(readSize)
//                Log.d("_audioDataFlow", "$readSize")
            }
        }
    }

    // 4. 停止录制（取消协程 + 释放资源）
    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    // 5. 销毁时确保停止录制（例如在ViewModel.onCleared()中调用）
    fun destroy() {
        if (isRecording) {
            stopRecording()
        }
        job.cancel() // 彻底取消Job，避免残留
    }
}


@SuppressLint("MissingPermission")
private fun createDefaultAudioRecord(sampleRate: Int): AudioRecord {
    val channelConfig = AudioFormat.CHANNEL_IN_STEREO // 双声道
    val audioFormat = AudioFormat.ENCODING_PCM_FLOAT // 32位浮点编码（高精度）
    val minBufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    )
//    val bufferSize = minBufferSize * 2
    val bufferSize: Int = (minBufferSize * 1.5).toInt()  // 似乎系统实际至少会采用1.5倍minBufferSize
    Log.d("BufferSizeInBytes", "$bufferSize")
    return AudioRecord.Builder()
        .setAudioSource(MediaRecorder.AudioSource.MIC)
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(audioFormat)
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .build()
        )
        .setBufferSizeInBytes(bufferSize)
        .build()
        .takeIf { it.state == AudioRecord.STATE_INITIALIZED }
        ?: throw IllegalStateException("无法初始化AudioRecord，检查权限和参数")
}