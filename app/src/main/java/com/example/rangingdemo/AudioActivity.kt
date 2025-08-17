package com.example.rangingdemo

import android.content.Intent
import android.graphics.Color
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rangingdemo.lib.RustFFTWrapper
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.AudioRecordViewModel
import com.example.rangingdemo.viewmodel.AudioTrackViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

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
                        MpChartWithStateFlow()
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

@Composable
fun MpChartWithStateFlow(
    viewModel: AudioRecordViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // 1. 收集 StateFlow 数据（自动在主线程更新）
    val data by viewModel.leftChannel.collectAsStateWithLifecycle()

    // 2. 通过 AndroidView 集成 LineChart
    AndroidView(
        modifier = modifier.fillMaxSize(),
        // 初始化 LineChart
        factory = { context ->
            LineChart(context).apply {
                // 配置图表基本属性
//                setupLineChart(this)
            }
        },
        // 3. 数据更新时刷新图表（data 变化会触发此回调）
        update = { lineChart ->
            if (data.isNotEmpty()) {
                // 将 FloatArray 转换为 MPAndroidChart 所需的 Entry 列表
                val entries = data.mapIndexed { index, value ->
                    Entry(index.toFloat(), value) // x=索引，y=数据值
                }
                // 更新图表数据
                updateLineChartData(lineChart, entries)
            }
        }
    )
}

// 配置 LineChart 样式（坐标轴、网格线等）
private fun setupLineChart(chart: LineChart) {
    chart.apply {
        // 禁用描述文本
        description.isEnabled = false

        // 配置 X 轴
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM // X 轴在底部
            setDrawGridLines(false) // 禁用 X 轴网格线
            axisLineColor = Color.GRAY // 轴线颜色
        }

        // 配置 Y 轴（左侧）
        axisLeft.apply {
            setDrawGridLines(true)
            gridColor = Color.LTGRAY // 网格线颜色
            axisLineColor = Color.GRAY
        }

        // 禁用右侧 Y 轴
        axisRight.isEnabled = false

        // 启用触摸和缩放
        setTouchEnabled(true)
        isDragEnabled = true // 可拖动
        setScaleEnabled(true) // 可缩放
    }
}

// 更新图表数据
private fun updateLineChartData(chart: LineChart, entries: List<Entry>) {
    val dataSet = LineDataSet(entries, "data").apply {
        color = Color.BLUE // 线颜色
//        lineWidth = 2f // 线宽
//        valueTextColor = Color.RED
//        setCircleColor(Color.RED) // 数据点颜色
//        circleRadius = 4f // 数据点半径
        setDrawValues(false) // 不显示数据点数值
    }

    // 设置新数据并刷新图表
    chart.data = LineData(dataSet, )    // 允许多个dataSet
    chart.invalidate() // 强制重绘
}