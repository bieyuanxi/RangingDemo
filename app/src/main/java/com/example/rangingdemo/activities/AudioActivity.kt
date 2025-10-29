package com.example.rangingdemo.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.AudioRecordViewModel
import com.example.rangingdemo.viewmodel.AudioTrackViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rangingdemo.FlowDataSaver
import com.example.rangingdemo.ui.components.MpChartWithStateFlow
import com.example.rangingdemo.N
import com.example.rangingdemo.N_prime
import com.example.rangingdemo.ZC_hat
import com.example.rangingdemo.ZC_hat_prime
import com.example.rangingdemo.consumeComplexArray2StereoFloatArray
import com.example.rangingdemo.f_s
import com.example.rangingdemo.generateSimpleStereoAudio
import com.example.rangingdemo.modulate
import com.example.rangingdemo.viewmodel.AudioProcessingParams
import com.example.rangingdemo.viewmodel.RotationAngleViewModel
import java.io.File


class AudioActivity : ComponentActivity() {
    private val f_c = 18000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioTrackViewModel: AudioTrackViewModel by viewModels()
        val audioRecordViewModel: AudioRecordViewModel by viewModels()

        val stereoAudioData = consumeComplexArray2StereoFloatArray(modulate(ZC_hat, N, f_c, f_s), leftRate = 10.0f, rightRate = 10.0f)

        audioRecordViewModel.setProcessingParams(listOf(AudioProcessingParams(ZC_hat_prime, N_prime, f_c)))

        enableEdgeToEdge()
        setContent {
            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val cirList by audioRecordViewModel.cirList.collectAsStateWithLifecycle(initialValue = emptyList())
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Audio Track & Record Activity")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("fixed freq AudioPlayer")
                            AudioPlayerUI(
                                stereoAudioData = generateSimpleStereoAudio(
                                    40,
                                    21000,
                                    19000,
                                    48000
                                )
                            )
                        }
                        HorizontalDivider(thickness = 2.dp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Modulated AudioPlayer")
                            AudioPlayerUI(stereoAudioData = stereoAudioData)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("AudioRecorder")
                            AudioRecorderUI(frameLen = N)
                        }
                        HorizontalDivider(thickness = 2.dp)
                        GrvAndAudio()
                        HorizontalDivider(thickness = 2.dp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MpChart")
                            MpChartWithStateFlow(f_c, cirList)
                        }
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
fun AudioPlayerUI(
    viewModel: AudioTrackViewModel = viewModel(),
    stereoAudioData: FloatArray,
) {
    var isPlaying by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (!isPlaying) {
                viewModel.start(
                    stereoAudioData,
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
fun AudioRecorderUI(viewModel: AudioRecordViewModel = viewModel(), frameLen: Int) {
    var isRecording by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (!isRecording) {
                viewModel.start(frameLen = frameLen)
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
fun GrvAndAudio() {
    val rotationAngleViewModel: RotationAngleViewModel = viewModel()
    val audioRecordViewModel: AudioRecordViewModel = viewModel()

    val flowSaver = remember {
        FlowDataSaver(rotationAngleViewModel.viewModelScope)
    }

    val angle by rotationAngleViewModel.rotationAngle.collectAsStateWithLifecycle(initialValue = 0.0f)

    var isRecording by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Game Rotation Vector Sensor")
    }
    Text("range=(-180°, 180°), angle = %.1f°".format(angle))
    Row {
        Button(onClick = {
            rotationAngleViewModel.calibrate()
        }) { Text("calibrate") }
        Button(onClick = {
            isRecording = !isRecording
            if (isRecording) {
                val outputFile = File(context.filesDir, "${Build.MODEL}_angle_audio_${System.currentTimeMillis()}.csv")
                flowSaver.init(outputFile)
                flowSaver.saveFlows(rotationAngleViewModel.rotationAngle, audioRecordViewModel.indexList)
            } else {
                flowSaver.close()
            }
        }) { Text(if(!isRecording) "Record angle&index" else "stop") }
    }
}

fun startSave(isRecording: Boolean) {

}

fun stopSave() {

}