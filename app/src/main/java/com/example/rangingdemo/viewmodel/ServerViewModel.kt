package com.example.rangingdemo.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rangingdemo.CmdPong
import com.example.rangingdemo.CmdRequestArray
import com.example.rangingdemo.CmdResponseArray
import com.example.rangingdemo.CmdSetParams
import com.example.rangingdemo.CmdStartPlay
import com.example.rangingdemo.CmdStartRecord
import com.example.rangingdemo.CmdStopPlay
import com.example.rangingdemo.CmdStopRecord
import com.example.rangingdemo.Message
import com.example.rangingdemo.N
import com.example.rangingdemo.Param
import com.example.rangingdemo.activities.leftArrays
import com.example.rangingdemo.activities.rightArrays
import com.example.rangingdemo.f_s
import com.example.rangingdemo.get_distance
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

    private val _receivedMsg = MutableStateFlow("")
    val receivedMsg: StateFlow<String> = _receivedMsg

    val clientConnections = ConcurrentHashMap<String, ClientConnection>()

    // 消息监听
    var onMessageReceived: ((Message) -> Unit)? = null

    private val _receivedMsgList = MutableStateFlow<List<CmdResponseArray>>(emptyList())
    val receivedMsgList: StateFlow<List<CmdResponseArray>> = _receivedMsgList

    fun startServer(port: Int = 8888) = viewModelScope.launch(Dispatchers.IO) {
        isRunning.value = true
        serverSocket = ServerSocket(port)
        serverSocket?.let { serverSocket ->
            viewModelScope.launch(Dispatchers.IO) {
                while (isRunning.value) {
                    try {
                        val socket = serverSocket.accept()
                        Log.d("Server", "New client connected: ${socket.inetAddress}")
                        // 创建客户端连接并存储
                        val clientId = "client_${System.currentTimeMillis()}"
                        val clientConnection = ClientConnection(socket, clientId)
                        clientConnections[clientId] = clientConnection
                        handleClientMessages(clientConnection)
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

    fun allocateParamList(deviceCnt: Int, start_f_c: Int, step: Int): List<Param> {
        val paramsList = (0 until deviceCnt).map { index ->
            Param(f_s, start_f_c + step * index, 1, 37)
        }
        return paramsList
    }

    fun performRangingJob(start_f_c: Int, step: Int) = viewModelScope.launch(Dispatchers.IO) {
        val paramsArray = allocateParamList(clientConnections.size, start_f_c, step).toTypedArray()

        _receivedMsgList.value = emptyList()    // 清空消息

        val deferredJobs = clientConnections.entries.withIndex()
            .map { (index, entry) ->
                // 用async启动并行协程
                async(Dispatchers.IO) {
                    write(
                        entry.key,
                        CmdSetParams(
                            paramsArray[index].f_c,
                            paramsArray
                        )
                    )
                }
            }
        // 等待所有并行任务完成
        deferredJobs.awaitAll()

        delay(700)

        write2AllClient(CmdRequestArray())
        write2AllClient(CmdStopPlay())
        write2AllClient(CmdStopRecord())
    }

    // 处理客户端消息
    private fun handleClientMessages(clientConnection: ClientConnection) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                while (isRunning.value && !clientConnection.socket.isClosed) {
                    val json = clientConnection.reader.readLine() ?: break
                    Log.d("handleClientMessages", json)
                    val msg = jsonFormat.decodeFromString<Message>(json)
                    withContext(Dispatchers.Main) {
                        onMessageReceived?.invoke(msg)
                    }
                    _onMessageReceived(msg)
                }
            } catch (e: IOException) {
                // 客户端断开连接
                e.printStackTrace()
            } finally {
                val clientId = clientConnection.clientId
                clientConnections.remove(clientId)
                clientConnection.close()
            }
        }

    private fun _onMessageReceived(msg: Message) {
        when (msg) {
            is CmdResponseArray -> {
                _receivedMsgList.value += msg
            }

            is CmdPong -> {

            }
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