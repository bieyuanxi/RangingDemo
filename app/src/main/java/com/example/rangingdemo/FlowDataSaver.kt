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
    val index: Int,
    val angle: Float,
    val cirLeft: Float,
    val cirRight: Float,
    val diff: Float,
) {
    fun toCsvRow(): String {
        return "${timestamp},${index},${angle},${cirLeft},${cirRight},${diff}\n"
    }

    companion object {
        fun toCsvHeader(): String {
            return "timestamp,index,angle,cir_left,cir_right,diff\n"
        }
    }
}

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
            writeHeader(FlowData.toCsvHeader())
        } catch (e: IOException) {
            e.printStackTrace()
            fileWriter = null
        }
    }

    // 写入表头（CSV格式）
    fun writeHeader(header: String) {
        fileWriter?.write(header)
        fileWriter?.flush()
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
            val flow = mutableListOf<FlowData>()

            for (i in 0 until indexList.size) {
                val pairCir = indexList[i]
                val leftPair = pairCir.first
                val rightPair = pairCir.second

                flow.add(FlowData(
                    timestamp = System.currentTimeMillis(),
                    index = i,
                    angle = angle,
                    cirLeft = leftPair.second,
                    cirRight = rightPair.second,
                    diff = leftPair.second - rightPair.second,
                ))
            }
            flow.toList()
        }

        // 收集合并后的流，写入文件
        flowJob = combinedFlow.onEach { flowData ->
            writeToFile(flowData)
        }.launchIn(scope)
    }


    /**
     * 将FlowData写入文件（CSV格式）
     */
    private suspend fun writeToFile(list: List<FlowData>) = withContext(Dispatchers.IO) {
        try {
            for (data in list) {
                val line = data.toCsvRow()
                Log.d("fileWriter", line)
                fileWriter?.write(line)
            }
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
