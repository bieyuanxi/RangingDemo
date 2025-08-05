package com.example.rangingdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.AudioTrackViewModel


class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val audioTrackViewModel: AudioTrackViewModel by viewModels()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Greeting2(
                            name = "Android",
                        )
                        AudioPlayer(
                            onPlay = {
                                audioTrackViewModel.startPlay(
                                    generateStereoAudio(20, 21000, 19000, 48000),
                                    loopCount = 100,
                                    sampleRate = 48000
                                )
                            }, onStop = {
                                audioTrackViewModel.stopPlay()
                            }
                        )
                    }

                }
            }
        }
    }
}

@Composable
fun Greeting2(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "This is debug activity",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    RangingDemoTheme {
        Greeting2("Android")
    }
}


@Composable
fun AudioPlayer(modifier: Modifier = Modifier, onPlay: () -> Unit, onStop: () -> Unit) {
    var isPlaying by remember { mutableStateOf(false) }
    Button(modifier = modifier,
        onClick = {
            if (isPlaying) {
                onStop()
            } else {
                onPlay()
            }
            isPlaying = !isPlaying
        }
    ) {
        Text(
            text = if (!isPlaying) "Play" else "Stop",
        )
    }

}