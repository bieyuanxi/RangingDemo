package com.example.rangingdemo

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

// 封装单个流的数据（包含标识符、时间戳和值）
data class FlowData(
    val timestamp: Long, // 时间戳（毫秒）
    val angle: Float,
    val cirLeft: Float,
    val cirRight: Float,
    val diff: Float
)

class FlowDataSaver(private val scope: CoroutineScope) {
    // 用于写入文件的Writer（确保线程安全）
    private var fileWriter: FileWriter? = null
    private var flowJob: Job? = null

    // 时间戳格式化（可选，用于可读性）
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    var fileName: String? = null

    /**
     * 初始化文件写入器
     * @param outputFile 目标文件
     */
    fun init(outputFile: File) {
        try {
            // 若文件已存在，追加模式；否则创建新文件
            fileWriter = FileWriter(outputFile, true)
            fileName = outputFile.name
            // 写入表头（CSV格式）
            fileWriter?.write("timestamp,angle,cir_left,cir_right,diff\n")
        } catch (e: IOException) {
            e.printStackTrace()
            fileWriter = null
        }
    }

    /**
     * 合并多个流并保存到文件
     * @param flows 键值对：flowId -> 待收集的流
     */
    fun saveFlows(
        angleFlow: Flow<Float>,
        indexFlow: Flow<List<Pair<Pair<Int, Float>, Pair<Int, Float>>>>
    ) {
        if (fileWriter == null) {
            Log.e("saveFlows", "请先调用init()初始化文件")
            return
        }

        val combinedFlow = indexFlow.filter {
            it.isNotEmpty()
        }.combine(angleFlow) { indexList, angle ->
            val index = indexList[0]    // TODO: 取不同的流
            val left = index.first
            val right = index.second
            FlowData(
                timestamp = System.currentTimeMillis(),
                angle = angle,
                cirLeft = left.second,
                cirRight = right.second,
                diff = left.second - right.second,
            )
        }

        // 收集合并后的流，写入文件
        flowJob = combinedFlow.onEach { flowData ->
            writeToFile(flowData)
        }.launchIn(scope)
    }


    /**
     * 将单条FlowData写入文件（CSV格式）
     */
    private suspend fun writeToFile(data: FlowData) = withContext(Dispatchers.IO) {
        try {
            val formattedTime = dateFormat.format(Date(data.timestamp))
            // 拼接CSV行（处理特殊字符，如逗号）
            val line = "${data.timestamp},${data.angle},${data.cirLeft},${data.cirRight},${data.diff}\n"
            Log.d("fileWriter", line)
            fileWriter?.write(line)
            fileWriter?.flush() // 实时刷新到文件（可根据性能需求调整）
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 关闭文件写入器，释放资源
     */
    fun close() {
        try {
            flowJob?.cancel()   // 为什么取消不了？？？？
            fileWriter?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
