package com.example.rangingdemo.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.filters.HighPass
import be.tarsos.dsp.io.TarsosDSPAudioFormat

/**
 * 函数式高通滤波器（适配 AudioEvent(TarsosDSPAudioFormat) 构造方式）
 * 核心：输入双声道音频数组 → 内部处理格式和事件封装 → 输出滤波后数组
 */
class HighPassFilter(
    private val sampleRate: Int = 48000,         // 采样率（Hz）
    private val cutOffFreq: Float = 16000.0f,    // 截止频率（过滤 cutOffFreq Hz以下低频）
) {
    // 音频格式（单声道，用于创建AudioEvent）
    private val monoFormat = TarsosDSPAudioFormat(
        sampleRate.toFloat(),        // 采样率
        32,                          // 位深度（32位浮点）
        2,                           // 声道数（单声道，左右声道分别处理）
        true,                        // 浮点格式
        false                        // 字节顺序（无关浮点格式）
    )

    // 左右声道独立的高通滤波器
    private val filter = HighPass(cutOffFreq, sampleRate.toFloat())

    /**
     * 函数式滤波方法：输入→滤波→输出
     * @param input 原始双声道音频（交错格式：[左1, 右1, 左2, 右2, ...]）
     * @return 滤波后的双声道音频（同格式）
     */
    fun filter(input: FloatArray): FloatArray {
        require(input.size % 2 == 0) { "双声道数据长度必须为偶数" }

        // 滤波
        val output = processChannels(filter, input)

        return output
    }

    private fun processChannels(filter: HighPass, input: FloatArray): FloatArray {
        val audioEvent = AudioEvent(monoFormat).apply {
            // 设置音频缓冲区（复制输入数据）
            floatBuffer = input.copyOf()
        }
        filter.process(audioEvent)

        return audioEvent.floatBuffer
    }
}