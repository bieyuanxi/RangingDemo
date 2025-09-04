package com.example.rangingdemo.viewmodel

import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.system.measureNanoTime


class AudioRecordViewModel : ViewModel() {
    private val audioRecorder = AudioRecorder()

    private val _audioChannel = MutableStateFlow(Pair(floatArrayOf(), floatArrayOf()))
    val audioChannel: StateFlow<Pair<FloatArray, FloatArray>> = _audioChannel

    // 可动态修改的参数列表
    private val _processingParams = MutableStateFlow(
        // 默认参数（可初始化为空或默认值）
        listOf(
            AudioProcessingParams(ZC_hat_prime, N_prime, 19000),
            AudioProcessingParams(ZC_hat_prime, N_prime, 20000),
            AudioProcessingParams(ZC_hat_prime, N_prime, 21000),
        )
    )
    val processingParams: StateFlow<List<AudioProcessingParams>> = _processingParams

    // cir list
    private val _cirList = MutableStateFlow<List<Pair<FloatArray, FloatArray>>>(listOf())
    val cirList: StateFlow<List<Pair<FloatArray, FloatArray>>> = _cirList

    // 处理后的结果: 峰值下标列表
    private val _indexList = MutableStateFlow<List<Pair<Int, Int>>>(listOf())
    val indexList: StateFlow<List<Pair<Int, Int>>> = _indexList

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

        // 监听原始声道数据，在后台进行处理
        // 同时监听音频数据和参数列表的变化
        combine(audioChannel, processingParams) { rawData, params ->
            // 组合数据：原始音频 + 当前参数列表
            Pair(rawData, params)
        }.filter { (rawData, params) ->
            val (left, right) = rawData
            left.isNotEmpty() && right.isNotEmpty() && params.isNotEmpty()
        }.map { (rawData, params) ->
            // 并行处理所有任务
            val result: List<Pair<FloatArray, FloatArray>>
            val timeSpent = measureNanoTime {
                result = processInParallel(rawData, params)
            }
            Log.d("processInParallel", "${timeSpent / 1_000_000.0}, params=${params.size}")
            result
        }.onEach { processedData ->
            _cirList.value = processedData
        }.flowOn(Dispatchers.Default).launchIn(viewModelScope)


        cirList.filter { list ->
            list.isNotEmpty()
        }.map { list ->
            list.map { (leftCir, rightCir) ->
                Pair(getMaxIndexedValue(leftCir).first, getMaxIndexedValue(rightCir).first)
            }
        }.onEach { data ->
            _indexList.value = data
        }.flowOn(Dispatchers.Default).launchIn(viewModelScope)


        // debug
        indexList.filter { list ->
            list.isNotEmpty()
        }.flowOn(Dispatchers.IO).onEach { data ->
            Log.d("indexList", "$data")
        }.launchIn(viewModelScope)
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

    // 设置参数
    fun setProcessingParams(newParams: List<AudioProcessingParams>) {
        _processingParams.value = newParams
    }

    // 并行处理函数：接收原始数据和参数列表，返回所有处理结果
    private suspend fun processInParallel(
        rawData: Pair<FloatArray, FloatArray>, paramsList: List<AudioProcessingParams>
    ): List<Pair<FloatArray, FloatArray>> {
        // 对每个参数创建一个并行任务
        val deferredList = paramsList.map { params ->
            // 用async启动并行协程，指定Dispatchers.Default处理计算密集型任务
            viewModelScope.async(Dispatchers.Default) {
                processAudioData(rawData, params)
            }
        }

        // 等待所有并行任务完成，返回结果列表
        return deferredList.awaitAll()
    }


    // 并行处理左右声道的音频数据
    private suspend fun processAudioData(
        rawData: Pair<FloatArray, FloatArray>, params: AudioProcessingParams
    ): Pair<FloatArray, FloatArray> = coroutineScope {  // 用于管理子协程
        val (leftChannel, rightChannel) = rawData

        // 左声道处理任务（并行）
        val leftDeferred = async(Dispatchers.Default) {
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
        val rightDeferred = async(Dispatchers.Default) {
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