package com.example.rangingdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.rangingdemo.complex.Complex32
import com.example.rangingdemo.complex.Complex32Array
import com.example.rangingdemo.lib.RustFFTWrapper
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class OfdmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("OFDM Activity")
                        }
                        MeasurePreModulate()
                        MeasureModulate()
                        MeasureDemodulate()
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
        timeTaken = measureNanoTime {
            val zc = genZCSequence(1, 81, 81)
            val ZC = RustFFTWrapper.fft(zc)
            val ZC_hat = frequencyRearrange(ZC)
        }
    }) { Text("gen zc + ZC + ZC_hat: $timeTaken ns") }
}

@Composable
fun MeasureModulate() {
    var timeTaken by remember { mutableStateOf(0L) }
    Button(onClick = {
        val zc = genZCSequence(1, 81, 81)
        val ZC = RustFFTWrapper.fft(zc)
        val ZC_hat = frequencyRearrange(ZC)
        timeTaken = measureNanoTime {
            val result = modulate(ZC_hat, 960, 19000, 48000)
        }
    }) { Text("measure modulate time: $timeTaken ns") }
}

@Composable
fun MeasureDemodulate() {
    var timeTaken by remember { mutableLongStateOf(0L) }
    var demodulateTimeTaken by remember { mutableLongStateOf(0L) }
    var magnitudeTimeTaken by remember { mutableLongStateOf(0L) }
    var maxIndexedValueTimeTaken by remember { mutableLongStateOf(0L) }
    Column {
        Button(onClick = {
            val N = 48 * 40 // 40ms
            val f_c = 19000
            val f_s = 48000
            val zc = genZCSequence(1, 81, 81)
            val ZC = RustFFTWrapper.fft(zc)
            val ZC_hat = frequencyRearrange(ZC)
            val ZC_hat_prime = conjugate(ZC_hat)
            val N_prime = N
            val x = modulate(ZC_hat, N, f_c, f_s)

            // -----------------------spread in air-------------------------//
            val offset = 287
            val y = x.shiftRight(offset)    // 假设循环移位了offset位

            val cir: Complex32Array
            val mag: FloatArray

            demodulateTimeTaken = measureNanoTime {
                cir = demodulate(y, ZC_hat_prime, N_prime, f_c, f_s)
            }

            magnitudeTimeTaken = measureNanoTime {
                mag = magnitude(cir)
            }

            maxIndexedValueTimeTaken = measureNanoTime {
//                maxIndexedValue = mag.withIndex().maxByOrNull { it.value }    //性能不佳
//                assert(offset == maxIndexedValue?.index)
                val result = getMaxIndexedValue(mag)
                assert(result.first == offset)
//                assert(result.first == maxIndexedValue?.index)
//                assert(result.second == maxIndexedValue?.value)
            }

            Log.d("lenOfDemodulate", "${mag.size}")
        }) { Text("measure demodulate time") }
        Text("demodulate(): ${ns2ms(demodulateTimeTaken)} ms")
        Text("magnitude(): ${ns2ms(magnitudeTimeTaken)} ms")
        Text("maxIndexedValue(): ${ns2ms(maxIndexedValueTimeTaken)} ms")
        Text("sum: ${ns2ms((demodulateTimeTaken + magnitudeTimeTaken + maxIndexedValueTimeTaken))} ms")
    }

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