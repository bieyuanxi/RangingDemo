package com.example.rangingdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rangingdemo.lib.RustFFTWrapper
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.AudioRecordViewModel
import com.example.rangingdemo.viewmodel.AudioTrackViewModel
import com.example.rangingdemo.viewmodel.ClientViewModel
import com.example.rangingdemo.viewmodel.ServerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RangingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val host = intent.getStringExtra("host") ?: ""
        val isGroupOwner = intent.getBooleanExtra("is_group_owner", false)

        val audioRecordViewModel: AudioRecordViewModel by viewModels()
        val audioTrackViewModel: AudioTrackViewModel by viewModels()
        val serverViewModel: ServerViewModel by viewModels()
        serverViewModel.onMessageReceived = { msg ->
            when (msg) {
                is CmdResponseArray -> {
                    Log.d("CmdResponseArray", msg.array.toString())
                }

                is CmdPong -> {

                }
            }
        }

        val clientViewModel: ClientViewModel by viewModels()
        clientViewModel.onMessageReceived = { msg ->
            when(msg) {
                is CmdStartRecord -> {
                    audioRecordViewModel.start()
                }
                is CmdStopRecord -> {
                    audioRecordViewModel.stop()
                }
                is CmdStartPlay -> {
                    val audioData = modulate(ZC_hat, N, f_c, f_s)
                    val stereoAudioData = complexArray2StereoFloatArray(audioData)
                    audioTrackViewModel.start(stereoAudioData, -1)
                }
                is CmdStopPlay -> {
                    audioTrackViewModel.stop()
                }
                is CmdSetParams -> {

                }
                is CmdPing -> {

                }
                is CmdRequestArray -> {

                }

            }
        }

        enableEdgeToEdge()
        setContent {
            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    Column(modifier = Modifier.padding(innerPadding)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Ranging Activity")
                        }
                        if(isGroupOwner) {
                            NewServerUI()
                            HorizontalDivider(thickness = 2.dp)
                        }

                        NewClientUI(host)
                        HorizontalDivider(thickness = 2.dp)
                        MpChartWithStateFlow()
                    }
                }
            }
        }
    }
}


private val N = 48 * 40 // 40ms
private val f_c = 19000
private val f_s = 48000
private val zc = genZCSequence(1, 81, 81)
private val ZC = RustFFTWrapper.fft(zc)
private val ZC_hat = frequencyRearrange(ZC)
private val ZC_hat_prime = conjugate(ZC_hat)
private val N_prime = N

@Composable
fun NewServerUI() {
    val serverViewModel: ServerViewModel = viewModel()

    val isServerRunning by remember { serverViewModel.isRunning }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Server")
    }

    Row {
        Button(onClick = {
            if (isServerRunning) {
                serverViewModel.stopServer()
            } else {
                serverViewModel.startServer()
            }
        }) { Text(if (!isServerRunning) "start server" else "stop server") }
        Spacer(Modifier.padding(10.dp))
        Button(
            onClick = {
                serverViewModel.viewModelScope.launch {
                    serverViewModel.write2AllClient(
                        jsonFormat.encodeToString(
                            CmdStartRecord() as Message // 必须要转成基类
                        )
                    )
                    delay(100)

                    serverViewModel.write2AllClient(
                        jsonFormat.encodeToString(
                            CmdStartPlay() as Message // 必须要转成基类
                        )
                    )
                    delay(5000)

                    serverViewModel.write2AllClient(
                        jsonFormat.encodeToString(
                            CmdStopPlay() as Message // 必须要转成基类
                        )
                    )
                    delay(100)

                    serverViewModel.write2AllClient(
                        jsonFormat.encodeToString(
                            CmdStopRecord() as Message // 必须要转成基类
                        )
                    )

                    // TODO: get distance
                }

            }
        ) { Text("start ranging") }
    }

    Text("Server received: ${serverViewModel.receivedMsg.collectAsState().value}")
}


@Composable
fun NewClientUI(host: String) {
    val clientViewModel: ClientViewModel = viewModel()

    val isClientRunning by remember { clientViewModel.isRunning }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Client")
    }
    Button(onClick = {
        if (isClientRunning) {
            clientViewModel.stopClient()
        } else {
            clientViewModel.startClient(host)
        }
    }) { Text(if (!isClientRunning) "start client" else "stop client") }

    Text("received: ${clientViewModel.receivedMsg.collectAsState().value}")
}