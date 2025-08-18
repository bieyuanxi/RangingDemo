package com.example.rangingdemo.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class ServerViewModel: ViewModel() {
    private var serverSocket: ServerSocket? = null

    val isRunning = mutableStateOf(false)

    private val _receivedMsg = MutableStateFlow("")
    val receivedMsg: StateFlow<String> = _receivedMsg

    private val _sockets = MutableStateFlow(Socket())
    val sockets: StateFlow<Socket> = _sockets

    fun startServer(port: Int = 8888) = viewModelScope.launch(Dispatchers.IO) {
        isRunning.value = true
        serverSocket = ServerSocket(port)
        serverSocket?.let { serverSocket ->
            viewModelScope.launch(Dispatchers.IO) {
                while (isRunning.value) {
                    try {
                        val socket = serverSocket.accept()
                        Log.d("Server", "New client connected: ${socket.inetAddress}")
                        process(socket)
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

    fun process(socket: Socket) = viewModelScope.launch(Dispatchers.IO) {
        val reader = BufferedReader(InputStreamReader(socket.inputStream))
        val writer = PrintWriter(socket.outputStream, true)

        viewModelScope.launch(Dispatchers.IO) {
            while (socket.isConnected && reader.readLine().also { _receivedMsg.value = it } != null) {
            }
        }

        _sockets.value = socket
    }

    fun write(socket: Socket, msg: String) = viewModelScope.launch(Dispatchers.IO) {
        val writer = PrintWriter(socket.outputStream, true)
        writer.println(msg)
    }

    override fun onCleared() {
        super.onCleared()
        stopServer()
    }
}