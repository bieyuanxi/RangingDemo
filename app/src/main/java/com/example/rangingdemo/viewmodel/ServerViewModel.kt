package com.example.rangingdemo.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rangingdemo.CmdPong
import com.example.rangingdemo.CmdRequestArray
import com.example.rangingdemo.CmdRequestArrayV2
import com.example.rangingdemo.CmdResponseArray
import com.example.rangingdemo.CmdSetParamsV2
import com.example.rangingdemo.CmdStop
import com.example.rangingdemo.Message
import com.example.rangingdemo.N
import com.example.rangingdemo.Param
import com.example.rangingdemo.ParamV2
import com.example.rangingdemo.jsonFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap

class ServerViewModel : ViewModel() {
    private var serverSocket: ServerSocket? = null

    val isRunning = mutableStateOf(false)

    private val clientConnections = ConcurrentHashMap<String, ClientConnection>()
    val clientCounter = mutableIntStateOf(0)    // 已连接设备数

    // 消息监听
    var onMessageReceived: ((Message) -> Unit)? = null

    fun startServer(port: Int = 8888) = viewModelScope.launch(Dispatchers.IO) {
        isRunning.value = true
        serverSocket = ServerSocket(port)
        serverSocket?.let { serverSocket ->
            viewModelScope.launch(Dispatchers.IO) {
                while (isRunning.value) {
                    try {
                        val socket = serverSocket.accept()
                        Log.d("Server", "New client connected: ${socket.inetAddress}")
                        handleClientSocket(socket)
                    } catch (e: SocketException) {
                        Log.d("Socket", "Socket is closing due to cancellation.")
                    }
                }
            }
        }
        Log.d("ServerSocket", "Server is listening on port $port")
    }

    fun stopServer() {
        isRunning.value = false
        serverSocket?.close()
        serverSocket = null
    }

    /**
     * 测距流程
     * @param genParamsArray 参数列表回调，根据设备数量为设备分配参数列表，例如f_c
     */
    fun performRangingJob(genParamsArray: (deviceCounter: Int) -> Array<ParamV2>) = viewModelScope.launch(Dispatchers.IO) {
        val paramsArray: Array<ParamV2> = genParamsArray(clientCounter.intValue)
        assert(paramsArray.size == clientCounter.intValue)

        val deferredJobs = clientConnections.entries.withIndex()    // index may be unstable
            .map { (index, entry) ->
                // 用async启动并行协程
                async(Dispatchers.IO) {
                    write(
                        entry.key,
                        CmdSetParamsV2(
                            index,      // TODO: device index may change if count changes
                            paramsArray
                        )
                    )
                }
            }
        // 等待所有并行任务完成
        deferredJobs.awaitAll()

        delay(700)

        write2AllClient(CmdRequestArrayV2())
        delay(200)
        write2AllClient(CmdStop())
    }

    /**
     *
     * @param genParamsArray 参数列表回调，根据设备数量为设备分配参数列表，例如f_c
     */
    fun performAngleJobStart(genParamsArray: (deviceCounter: Int) -> Array<ParamV2>) = viewModelScope.launch(Dispatchers.IO) {
        val paramsArray: Array<ParamV2> = genParamsArray(clientCounter.intValue)
        assert(paramsArray.size == clientCounter.intValue)

        val deferredJobs = clientConnections.entries.withIndex()    // index may be unstable
            .map { (index, entry) ->
                // 用async启动并行协程
                async(Dispatchers.IO) {
                    write(
                        entry.key,
                        CmdSetParamsV2(
                            index,      // TODO: device index may change if count changes
                            paramsArray
                        )
                    )
                }
            }
        // 等待所有并行任务完成
        deferredJobs.awaitAll()
    }

    /**
     *
     * @param genParamsArray 参数列表回调，根据设备数量为设备分配参数列表，例如f_c
     */
    fun performAngleJobStop() = viewModelScope.launch(Dispatchers.IO) {
        write2AllClient(CmdStop())
    }

    // 处理客户端消息
    private fun handleClientSocket(socket: Socket) = viewModelScope.launch(Dispatchers.IO) {
        // 创建客户端连接并存储
        val clientId = "client_${System.currentTimeMillis()}"   // FIXME: ms级别可能不唯一，使用ns级别或者其他方法
        val clientConnection = ClientConnection(socket, clientId)
        clientConnections[clientConnection.clientId] = clientConnection
        clientCounter.value += 1

        try {
            while (isRunning.value && !clientConnection.socket.isClosed) {
                val json = clientConnection.reader.readLine() ?: break
                Log.d("handleClientMessages", json)
                val msg = jsonFormat.decodeFromString<Message>(json)
                withContext(Dispatchers.Main) {
                    onMessageReceived?.invoke(msg)
                }
            }
        } catch (e: IOException) {
            // 客户端断开连接
            e.printStackTrace()
        } finally {
            clientConnections.remove(clientConnection.clientId)
            clientConnection.close()
            clientCounter.value -= 1
        }
    }



    fun write(clientId: String, msg: Message) = write(clientId, jsonFormat.encodeToString(msg))

    fun write(clientId: String, msg: String) = viewModelScope.launch(Dispatchers.IO) {
//        Log.d("beforeWriteCmd", msg)
        clientConnections[clientId]?.writer?.println(msg)
//        Log.d("afterWriteCmd", msg)
    }

    fun write2AllClient(msg: Message) = write2AllClient(jsonFormat.encodeToString(msg))

    fun write2AllClient(msg: String) = viewModelScope.launch(Dispatchers.IO) {
        clientConnections.forEach { (id, cli) ->
            write(id, msg)
        }
    }

    fun getClientConnections(): ConcurrentHashMap<String, ClientConnection> {
        return this.clientConnections
    }

    override fun onCleared() {
        super.onCleared()
        stopServer()
    }
}

// 客户端连接封装类
class ClientConnection(val socket: Socket, val clientId: String) {
    val reader = BufferedReader(InputStreamReader(socket.inputStream))
    val writer = PrintWriter(socket.outputStream, true)

    fun close() {
        try {
            reader.close()
            writer.close()
            socket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}