package com.example.rangingdemo.activities

import android.content.Intent
import android.graphics.Color
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.AudioRecordViewModel
import com.example.rangingdemo.viewmodel.AudioTrackViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rangingdemo.MpChartWithStateFlow
import com.example.rangingdemo.N
import com.example.rangingdemo.N_prime
import com.example.rangingdemo.ZC_hat
import com.example.rangingdemo.ZC_hat_prime
import com.example.rangingdemo.complexArray2StereoFloatArray
import com.example.rangingdemo.f_s
import com.example.rangingdemo.generateSimpleStereoAudio
import com.example.rangingdemo.modulate
import com.example.rangingdemo.viewmodel.AudioProcessingParams
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.flow.StateFlow




class AudioActivity : ComponentActivity() {
    private val f_c = 19000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioTrackViewModel: AudioTrackViewModel by viewModels()
        val audioRecordViewModel: AudioRecordViewModel by viewModels()

        val audioData = modulate(ZC_hat, N, f_c, f_s)
        val stereoAudioData = complexArray2StereoFloatArray(audioData)

        audioRecordViewModel.setProcessingParams(listOf(AudioProcessingParams(ZC_hat_prime, N_prime, f_c)))

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
                        Column {
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
                        Column {
                            Text("Modulated AudioPlayer")
                            AudioPlayerUI(stereoAudioData = stereoAudioData)
                        }
                        Column {
                            Text("AudioRecorder")
                            AudioRecorderUI(frameLen = N)
                        }
                        HorizontalDivider(thickness = 2.dp)
                        Column {
                            Text("MpChart")
                            MpChartWithStateFlow(f_c, audioRecordViewModel.cirList)
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