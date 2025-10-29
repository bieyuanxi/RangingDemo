package com.example.rangingdemo.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioTrack.MODE_STATIC
import android.media.AudioTrack.PERFORMANCE_MODE_LOW_LATENCY
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

class StaticAudioPlayer : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private var audioTrack: AudioTrack? = null


    // FIXME: 竞态条件：多次调用此方法会创建多个协程，可能会重复实例化audioTrack或意外释放已实例化的audioTrack
    // FIXME: 维护一个播放状态，对其加锁，避免重复创建、停止
    // stereoAudioData: 立体声，双声道
    fun startPlay(stereoAudioData: FloatArray, loopCount: Int = 0) {
        stop()  // 停止可能存在的播放资源
        val createTimeTaken = measureTimeMillis {
            audioTrack = createFloatStereoAudioTrack(sampleRate = 48000)
        }
        Log.d("createAudioTrack", "createFloatStereoAudioTrack() takes $createTimeTaken ms")
        launch {
            val timeTaken = measureTimeMillis {
                audioTrack?.apply {
                    assert(
                        stereoAudioData.size ==
                                write(
                                    stereoAudioData,
                                    0,
                                    stereoAudioData.size,
                                    AudioTrack.WRITE_BLOCKING
                                )
                    )
                    val totalFrames = stereoAudioData.size / channelCount
                    val result = setLoopPoints(0, totalFrames, loopCount)
                    assert(result == AudioTrack.SUCCESS)
                    play()
                }

                // TODO: 创建协程跟踪audioTrack播放状态，在播放完毕后释放audioTrack
            }
            Log.d("startPlaytimeTaken", "startPlay() takes $timeTaken ms")
        }
    }

    fun stop() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}


private fun createFloatStereoAudioTrack(sampleRate: Int): AudioTrack {
    // 配置立体声道（左右声道）
    val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
    // 配置32位浮点格式
    val audioFormat = AudioFormat.ENCODING_PCM_FLOAT

    // 计算最小缓冲区大小
    val minBufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    )
//        // 实时流缓冲区：最小缓冲区的2倍（平衡延迟和稳定性）
//        val bufferSize = maxOf(minBufferSize, 2048)
    val bufferSize = minBufferSize
    Log.d("AudioTrackBufferSize", "$bufferSize")
    val attributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)  // 影响优先级和路由
        .build()

    // 音频格式配置
    val format = AudioFormat.Builder()
        .setSampleRate(sampleRate)
        .setChannelMask(channelConfig)
        .setEncoding(audioFormat)
        .build()

    return AudioTrack.Builder()
        .setAudioAttributes(attributes)
        .setAudioFormat(format)
        .setBufferSizeInBytes(bufferSize)
        .setPerformanceMode(PERFORMANCE_MODE_LOW_LATENCY)   // 低延迟模式
        .setTransferMode(MODE_STATIC)
        .build()
        .apply {
            if (state == AudioTrack.STATE_UNINITIALIZED) {
                release()
                throw IllegalStateException("音频播放器初始化失败, code: $state")
            }
//            Log.d("AudioTrackBuilder", "$state")
        }
}