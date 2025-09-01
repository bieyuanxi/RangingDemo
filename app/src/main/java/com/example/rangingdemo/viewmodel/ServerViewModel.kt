package com.example.rangingdemo.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rangingdemo.Message
import com.example.rangingdemo.jsonFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap

class ServerViewModel: ViewModel() {
    private var serverSocket: ServerSocket? = null

    val isRunning = mutableStateOf(false)

    private val _receivedMsg = MutableStateFlow("")
    val receivedMsg: StateFlow<String> = _receivedMsg

    val clientConnections = ConcurrentHashMap<String, ClientConnection>()

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

    // 处理客户端消息
    private fun handleClientMessages(clientConnection: ClientConnection) = viewModelScope.launch(Dispatchers.IO) {
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
            val clientId = clientConnection.clientId
            clientConnections.remove(clientId)
            clientConnection.close()
        }
    }

    fun write(clientId: String, msg: String) = viewModelScope.launch(Dispatchers.IO) {
        clientConnections[clientId]?.writer?.println(msg)
    }

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