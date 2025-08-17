package com.example.rangingdemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.example.rangingdemo.lib.RustFFTWrapper
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.AudioRecordViewModel
import com.example.rangingdemo.viewmodel.AudioTrackViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

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
                        AudioPlayerUI()
                        AudioRecorderUI()

                        Spacer(modifier = Modifier.height(20.dp))

                        ModulateAudioPlayerUI()
                        DemodulateAudioRecorderUI()
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
fun AudioPlayerUI(viewModel: AudioTrackViewModel = viewModel()) {
    var isPlaying by remember { mutableStateOf(false) }
    Text("AudioPlayer")
    Button(
        onClick = {
            if (!isPlaying) {
                viewModel.start(
                    generateStereoAudio(20, 21000, 19000, 48000),
                    loopCount = -1,
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
fun AudioRecorderUI(viewModel: AudioRecordViewModel = viewModel()) {
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

@Composable
fun ModulateAudioPlayerUI(viewModel: AudioTrackViewModel = viewModel()) {
    var isPlaying by remember { mutableStateOf(false) }
    Text("ModulateAudioPlayer")
    Button(
        onClick = {
            if (!isPlaying) {
                val zc = genZCSequence(1, 81, 81)
                val ZC = RustFFTWrapper.fft(zc)
                val ZC_hat = frequencyRearrange(ZC)
                val audioData = modulate(ZC_hat, 960, 19000, 48000)
                val stereoAudioData = complexArray2StereoFloatArray(audioData)
                viewModel.start(
                    stereoAudioData,
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
fun DemodulateAudioRecorderUI(viewModel: AudioRecordViewModel = viewModel()) {
    var isRecording by remember { mutableStateOf(false) }
    Text("DemodulateAudioRecorder")
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