package com.example.rangingdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rangingdemo.lib.RustFFTWrapper
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.AudioRecordViewModel
import com.example.rangingdemo.viewmodel.AudioTrackViewModel
import com.example.rangingdemo.viewmodel.ServerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ServerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serverViewModel: ServerViewModel by viewModels()
        val isRunning = serverViewModel.isRunning

        val cnt = mutableStateOf(0)

        serverViewModel.onMessageReceived = { msg ->
            when (msg) {
                is CmdResponseArray -> {
                    Log.d("CmdResponseArray", msg.array.toString())
                }
                is CmdPong -> {

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
                            Text("Server Activity")
                        }
                        Button(onClick = {
                            if (isRunning.value) {
                                serverViewModel.stopServer()
                            } else {
                                serverViewModel.startServer()
                            }
                        }) { Text(if (!isRunning.value) "start server" else "stop server") }
                        Button(
                            onClick = {
//                                serverViewModel.write(serverViewModel.sockets.value, "hi from server: " + cnt.value++)
                            }
                        ) { Text("send2client") }
                        Text("received: ${serverViewModel.receivedMsg.collectAsState().value}")
                        ServerUI()
                        MpChartWithStateFlow()
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting4(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview5() {
    RangingDemoTheme {
        Greeting4("Android")
    }
}


@Composable
fun ServerUIBtn() {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(context, ServerActivity::class.java)
            context.startActivity(intent)
        }
    ) {
        Text("Server Activity")
    }
}

private val f_c = 19000


@Composable
fun ServerUI() {
    val serverViewModel: ServerViewModel = viewModel()
    val audioRecordViewModel: AudioRecordViewModel = viewModel()
    val audioTrackViewModel: AudioTrackViewModel = viewModel()

    Button(
        onClick = {
            serverViewModel.viewModelScope.launch {
                serverViewModel.write2AllClient(
                    jsonFormat.encodeToString(
                        CmdStartRecord() as Message // 必须要转成基类
                    )
                )
                audioRecordViewModel.start()
                delay(100)

                serverViewModel.write2AllClient(
                    jsonFormat.encodeToString(
                        CmdStartPlay() as Message
                    )
                )
                val audioData = modulate(ZC_hat, N, f_c, f_s)
                val stereoAudioData = complexArray2StereoFloatArray(audioData)
                audioTrackViewModel.start(stereoAudioData, -1)
                delay(5000)

                serverViewModel.write2AllClient(
                    jsonFormat.encodeToString(
                        CmdStopPlay() as Message
                    )
                )
                audioTrackViewModel.stop()
                delay(100)

                serverViewModel.write2AllClient(
                    jsonFormat.encodeToString(
                        CmdStopRecord() as Message
                    )
                )
                audioRecordViewModel.stop()

                // TODO: get distance
            }

        }
    ) { Text("start ranging") }
}