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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketException

class ClientViewModel : ViewModel() {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null

    val isRunning = mutableStateOf(false)
    private val _receivedMsg = MutableStateFlow("")
    val receivedMsg: StateFlow<String> = _receivedMsg

    // 消息监听
    var onMessageReceived: ((Message) -> Unit)? = null

    fun startClient(host: String, port: Int = 8888) = viewModelScope.launch(Dispatchers.IO) {
        isRunning.value = true
        try {
            socket = Socket(host, port)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        socket?.let { socket ->
            Log.d("Client", "Connected to server: ${socket.inetAddress}")
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            writer = PrintWriter(socket.outputStream, true)
            try {
                do {
                    val json = reader.readLine()?: break
                    _receivedMsg.value = json

                    val msg = jsonFormat.decodeFromString<Message>(json)
                    withContext(Dispatchers.Default) {
                        onMessageReceived?.invoke(msg)  // TODO
                    }
                } while (viewModelScope.isActive)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            Log.d("Client", "disconnected.")
        }
    }

    fun write(msg: String) = viewModelScope.launch(Dispatchers.IO) {
        writer?.println(msg)
    }

    fun write(msg: Message) = viewModelScope.launch(Dispatchers.IO) {
        write(jsonFormat.encodeToString(msg))
    }

    fun stopClient() {
        socket?.close()
        socket = null
        writer = null
        isRunning.value = false
    }
}