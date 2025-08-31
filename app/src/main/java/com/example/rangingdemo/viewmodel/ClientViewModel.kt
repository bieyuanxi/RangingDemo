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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketException

class ClientViewModel : ViewModel() {
    private var socket: Socket? = null

    val isRunning = mutableStateOf(false)
    private val _receivedMsg = MutableStateFlow("")
    val receivedMsg: StateFlow<String> = _receivedMsg

    // 消息监听
    var onMessageReceived: ((Message) -> Unit)? = null

    fun startClient(host: String, port: Int = 8888) = viewModelScope.launch(Dispatchers.IO) {
        isRunning.value = true
        socket = Socket(host, port)
        socket?.let { socket ->
            Log.d("Client", "Connected to server: ${socket.inetAddress}")
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            try {
                while (reader.readLine().also {
                        withContext(Dispatchers.Main) {
                            Log.d("readLine", it)
                            val msg = jsonFormat.decodeFromString<Message>(it)
                            onMessageReceived?.invoke(msg)
                        }
                        _receivedMsg.value = it
                    } != null) {
                }
            } catch (e: SocketException) {

            }

            Log.d("Client", "disconnected.")
        }
    }

    fun write(msg: String) = viewModelScope.launch(Dispatchers.IO) {
        socket?.let { socket ->
            val writer = PrintWriter(socket.outputStream, true)
            writer.println(msg)
        }
    }

    fun stopClient() {
        socket?.close()
        socket = null
        isRunning.value = false
    }
}