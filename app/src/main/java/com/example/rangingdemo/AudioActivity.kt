package com.example.rangingdemo

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.AudioRecordViewModel
import com.example.rangingdemo.viewmodel.AudioTrackViewModel

class AudioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioTrackViewModel: AudioTrackViewModel by viewModels()
        val audioRecordViewModel: AudioRecordViewModel by viewModels()

        enableEdgeToEdge()
        setContent {
            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Audio Track & Record Activity")
                        }
                        AudioPlayer(audioTrackViewModel)
                        AudioRecorder(audioRecordViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting3(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview4() {
    RangingDemoTheme {
        Greeting3("Android")
    }
}

// AudioActivity debug UI
@Composable
fun AudioUIBtn() {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(context, AudioActivity::class.java)
            context.startActivity(intent)
        }
    ) {
        Text("AudioActivity")
    }
}

@Composable
fun AudioPlayer(viewModel: AudioTrackViewModel) {
    var isPlaying by remember { mutableStateOf(false) }
    Text("AudioPlayer")
    Button(
        onClick = {
            if (!isPlaying) {
                viewModel.start(
                    generateStereoAudio(20, 21000, 19000, 48000),
                    loopCount = 100,
                    sampleRate = 48000
                )
            } else {
                viewModel.stop()
            }
            isPlaying = !isPlaying
        }
    ) {
        Text(
            text = if (!isPlaying) "Play" else "Stop",
        )
    }
}

@Composable
fun AudioRecorder(viewModel: AudioRecordViewModel) {
    var isRecording by remember { mutableStateOf(false) }
    Text("AudioRecorder")
    Button(
        onClick = {
            if (!isRecording) {
                viewModel.start(frameLen = 40 * 48)
            } else {
                viewModel.stop()
            }
            isRecording = !isRecording
        }
    ) {
        Text(
            text = if (!isRecording) "Record" else "Stop",
        )
    }
}