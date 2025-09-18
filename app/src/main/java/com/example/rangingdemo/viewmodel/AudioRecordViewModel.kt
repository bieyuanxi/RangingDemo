package com.example.rangingdemo.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rangingdemo.audio.AudioRecorder
import com.example.rangingdemo.N_prime
import com.example.rangingdemo.ZC_hat_prime
import com.example.rangingdemo.complex.Complex32Array
import com.example.rangingdemo.demodulate
import com.example.rangingdemo.f_s
import com.example.rangingdemo.floatArray2ComplexArray
import com.example.rangingdemo.getMaxIndexedValue
import com.example.rangingdemo.magnitude
import com.example.rangingdemo.ns2ms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.measureNanoTime


class AudioRecordViewModel : ViewModel() {
    private val audioRecorder = AudioRecorder()

    var stopUpdate = false

    private val _audioChannel = MutableSharedFlow<Pair<FloatArray, FloatArray>>()
    val audioChannel: SharedFlow<Pair<FloatArray, FloatArray>> = _audioChannel

    // 可动态修改的参数列表
    private val _processingParams = MutableStateFlow(
        listOf<AudioProcessingParams>()
    )
    val processingParams: StateFlow<List<AudioProcessingParams>> = _processingParams

    // cir list
    private val _cirList = MutableSharedFlow<List<Pair<FloatArray, FloatArray>>>()
    val cirList: SharedFlow<List<Pair<FloatArray, FloatArray>>> = _cirList

    // 处理后的结果: 峰值列表(列表每一项对应一个频段的左右声道峰值)
    private val _indexList =
        MutableStateFlow<List<Pair<Pair<Int, Float>, Pair<Int, Float>>>>(listOf())
    val indexList: StateFlow<List<Pair<Pair<Int, Float>, Pair<Int, Float>>>> = _indexList

    init {
        audioRecorder.audioDataFlow.onEach { data ->
            val (leftChannel, rightChannel) = splitStereoChannels(data)
            _audioChannel.emit(leftChannel to rightChannel)
        }.flowOn(Dispatchers.Default).launchIn(viewModelScope)


        // 监听原始声道数据，在后台进行处理
        // 同时监听音频数据和参数列表的变化
        combine(audioChannel, processingParams) { rawData, params ->
            // 组合数据：原始音频 + 当前参数列表
            Pair(rawData, params)
        }.filter {
            !stopUpdate // 停止更新信息
        }.map { (rawData, params) ->
            // 并行处理所有任务
            val result: List<Pair<FloatArray, FloatArray>>
            val timeSpent = measureNanoTime {
                result = processInParallel(rawData, params)
            }
            Log.d("processInParallel", "${ns2ms(timeSpent)}, params=${params.size}")
            result
        }.onEach { processedData ->
            _cirList.emit(processedData)
        }.flowOn(Dispatchers.Default).launchIn(viewModelScope)


        cirList.map { list ->
            list.map { (leftCir, rightCir) ->
                val leftIndexValue = getMaxIndexedValue(leftCir)
                val rightIndexValue = getMaxIndexedValue(rightCir)
                Log.d("indexValue", "L: $leftIndexValue, R: $rightIndexValue")
                Pair(leftIndexValue, rightIndexValue)
            }
        }.onEach { data ->
            _indexList.emit(data)
        }.flowOn(Dispatchers.Default).launchIn(viewModelScope)


        // debug
        indexList.onEach { data ->
            Log.d("indexListBy:f_c", "$data")
        }.flowOn(Dispatchers.IO).launchIn(viewModelScope)
    }

    fun start(frameLen: Int = 40 * 48) {
        stopUpdate = false
        viewModelScope.launch {
            audioRecorder.startRecording(frameLen)
        }

    }

    fun stop() {
        audioRecorder.stopRecording()
    }

    override fun onCleared() {
        super.onCleared()

        stop()
    }

    // 设置参数
    fun setProcessingParams(newParams: List<AudioProcessingParams>) {
        _processingParams.value = newParams
    }

    // 并行处理函数：接收原始数据和参数列表，返回所有处理结果
    private suspend fun processInParallel(
        rawData: Pair<FloatArray, FloatArray>, paramsList: List<AudioProcessingParams>
    ): List<Pair<FloatArray, FloatArray>> = withContext(Dispatchers.Default) {
        //coroutineScope创建一个 “局部作用域”，所有子协程（async）会随该作用域的完成/取消而销毁，避免协程逃逸

        // 对每个参数创建一个并行任务
        val deferredList = paramsList.map { params ->
            // 用async启动并行协程，指定Dispatchers.Default处理计算密集型任务
            async {
                processAudioData(rawData, params)
            }
        }

        // 等待所有并行任务完成，返回结果列表
        deferredList.awaitAll()
    }


    // 并行处理左右声道的音频数据
    private suspend fun processAudioData(
        rawData: Pair<FloatArray, FloatArray>, params: AudioProcessingParams
    ): Pair<FloatArray, FloatArray> = withContext(Dispatchers.Default) {  // 用于管理子协程
        val (leftChannel, rightChannel) = rawData

        // 左声道处理任务（并行）
        val leftDeferred = async {
            val leftCir = demodulate(
                floatArray2ComplexArray(leftChannel),
                params.ZC_hat_prime,
                params.N_prime,
                params.f_c,
                f_s
            )
            magnitude(leftCir)
        }

        // 右声道处理任务（并行）
        val rightDeferred = async {
            val rightCir = demodulate(
                floatArray2ComplexArray(rightChannel),
                params.ZC_hat_prime,
                params.N_prime,
                params.f_c,
                f_s
            )
            magnitude(rightCir)
        }

        // 等待两个并行任务完成，组合结果
        Pair(leftDeferred.await(), rightDeferred.await())
    }
}

// 音频处理参数类
data class AudioProcessingParams(
    val ZC_hat_prime: Complex32Array,
    val N_prime: Int,
    val f_c: Int,
)

private fun splitStereoChannels(data: FloatArray): Pair<FloatArray, FloatArray> {
    val sampleCount = data.size / 2
    val left = FloatArray(sampleCount)
    val right = FloatArray(sampleCount)
    for (i in 0 until sampleCount) {
        left[i] = data[i * 2]       // 左声道：索引0,2,4...
        right[i] = data[i * 2 + 1]  // 右声道：索引1,3,5...
    }
    return left to right
}