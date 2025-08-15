package com.example.rangingdemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.rangingdemo.lib.RustFFTWrapper
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import kotlin.system.measureTimeMillis

class OfdmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        OFDM("OFDM")
                        MeasurePreModulate()
                        MeasureModulate()
                    }
                }
            }
        }
    }
}

@Composable
fun OFDM(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview3() {
    RangingDemoTheme {
        Column() {
            OFDM("OFDM")
            MeasurePreModulate()
            MeasureModulate()
        }
    }
}

@Composable
fun MeasurePreModulate() {
    var timeTaken by remember { mutableStateOf(0L) }
    Button(onClick = {
        timeTaken = measureTimeMillis {
            val zc = genZCSequence(1, 81, 81)
            val ZC = RustFFTWrapper.fft(zc)
            val ZC_hat = frequencyRearrange(ZC)
        }
    }) { Text("gen zc + ZC + ZC_hat: $timeTaken ms") }
}

@Composable
fun MeasureModulate() {
    var timeTaken by remember { mutableStateOf(0L) }
    Button(onClick = {
        val zc = genZCSequence(1, 81, 81)
        val ZC = RustFFTWrapper.fft(zc)
        val ZC_hat = frequencyRearrange(ZC)
        timeTaken = measureTimeMillis {
            val result = modulate(ZC_hat, 960, 19000, 48000)
        }
    }) { Text("measure modulate time: $timeTaken ms") }
}


// OFDM debug UI
@Composable
fun OfdmUIBtn() {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(context, OfdmActivity::class.java)
            context.startActivity(intent)
        }
    ) {
        Text("OFDM")
    }
}