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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.rangingdemo.lib.RustFFTWrapper
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.AudioRecordViewModel
import com.example.rangingdemo.viewmodel.AudioTrackViewModel
import com.example.rangingdemo.viewmodel.ClientViewModel


class ClientActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val clientViewModel: ClientViewModel by viewModels()
        val isRunning = clientViewModel.isRunning

        val host = intent.getStringExtra("host")

        val audioRecordViewModel: AudioRecordViewModel by viewModels()
        val audioTrackViewModel: AudioTrackViewModel by viewModels()
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
                            Text("Client Activity")
                        }
                        Button(onClick = {
                            if (isRunning.value) {
                                clientViewModel.stopClient()
                            } else {
                                if (host != null) {
                                    clientViewModel.startClient(host)
                                }
                            }
                        }) { Text(if (!isRunning.value) "start client" else "stop client") }
                        Button(
                            onClick = {
                                clientViewModel.write("hi from client")
                            }
                        ) { Text("send2server") }
                        Text("received: ${clientViewModel.receivedMsg.collectAsState().value}")
                        MpChartWithStateFlow(f_c, audioRecordViewModel.cirList)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting5(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview6() {
    RangingDemoTheme {
        Greeting5("Android")
    }
}


private val f_c = 19000


