package com.example.rangingdemo

// 生成指定频率的立体声音频
fun generateStereoAudio(
    durationMs: Int,
    leftFreq: Int,
    rightFreq: Int,
    sampleRate: Int
): FloatArray {
    assert(leftFreq * 2 <= sampleRate)
    assert(rightFreq * 2 <= sampleRate)

    // 计算总帧数（每帧包含左右声道各一个采样）
    val frameCount = (sampleRate * durationMs / 1000.0).toInt()
    // 总采样数 = 帧数 × 2（立体声道）
    val totalSamples = frameCount * 2
    val pcmData = FloatArray(totalSamples)

    var time = 0.0
    val timeStep = 1.0 / sampleRate

    for (i in 0 until frameCount) {
        // 左声道采样（偶数索引）
        pcmData[i * 2] = Math.sin(2 * Math.PI * leftFreq * time).toFloat()
        // 右声道采样（奇数索引）
        pcmData[i * 2 + 1] = Math.sin(2 * Math.PI * rightFreq * time).toFloat()

        time += timeStep
    }

    return pcmData
}