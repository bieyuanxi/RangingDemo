package com.example.rangingdemo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


private val f_c = 19000

class AudioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioTrackViewModel: AudioTrackViewModel by viewModels()
        val audioRecordViewModel: AudioRecordViewModel by viewModels()

        val audioData = modulate(ZC_hat, N, f_c, f_s)
        val stereoAudioData = complexArray2StereoFloatArray(audioData)

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
                            MpChartWithStateFlow(f_c)
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

// 优化后的Composable
@Composable
fun MpChartWithStateFlow(
    f_c: Int,
    viewModel: AudioRecordViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // 收集数据
    val audioChannel by viewModel.audioChannel.collectAsStateWithLifecycle()

    var leftPeekIndex by remember { mutableIntStateOf(0) }
    var rightPeekIndex by remember { mutableIntStateOf(0) }
    var chartData by remember { mutableStateOf<Pair<LineDataSet, LineDataSet>?>(null) }

    var showLeft by remember { mutableStateOf(true) }
    var showRight by remember { mutableStateOf(true) }

    Row {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("leftChannel")
            Checkbox(checked = showLeft, onCheckedChange = { showLeft = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("rightChannel")
            Checkbox(checked = showRight, onCheckedChange = { showRight = it })
        }
    }

    Row {
        Text("mL: $leftPeekIndex")
        Spacer(modifier = Modifier.width(5.dp))
        Text("mR: $rightPeekIndex")
    }

    // 处理后台计算
    LaunchedEffect(audioChannel, showLeft, showRight) {
        withContext(Dispatchers.Default) {  // 在计算型协程中处理数据
            // 清空数据，防止内存堆积
            chartData?.first?.clear()
            chartData?.second?.clear()

            val (data1, data2) = audioChannel
            if (data1.isEmpty() || data2.isEmpty()) {
                return@withContext
            }

            // 左声道处理
            val leftCir = demodulate(floatArray2ComplexArray(data1), ZC_hat_prime, N_prime, f_c, f_s)
            val leftMag = magnitude(leftCir)
            val (leftIndex, _) = getMaxIndexedValue(leftMag)
            val leftEntries = leftMag.mapIndexed { i, v ->
                Entry(i.toFloat(), v)
            }

            // 右声道处理
            val rightCir = demodulate(floatArray2ComplexArray(data2), ZC_hat_prime, N_prime, f_c, f_s)
            val rightMag = magnitude(rightCir)
            val (rightIndex, _) = getMaxIndexedValue(rightMag)
            val rightEntries = rightMag.mapIndexed { i, v ->
                Entry(i.toFloat(), v)
            }

            // 创建数据集
            val dataSet1 = LineDataSet(leftEntries, "左声道").apply {
                color = Color.BLUE
                setDrawValues(false)
            }
            val dataSet2 = LineDataSet(rightEntries, "右声道").apply {
                color = Color.GREEN
                setDrawValues(false)
            }

            // 更新UI状态
            withContext(Dispatchers.Main) { // 在UI协程中更新
                leftPeekIndex = leftIndex
                rightPeekIndex = rightIndex
                chartData = Pair(dataSet1, dataSet2)
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            LineChart(context).apply {
                setupLineChart(this)
            }
        },
        update = { lineChart ->
            chartData?.let { (dataSet1, dataSet2) ->
                dataSet1.isVisible = showLeft
                dataSet2.isVisible = showRight
                lineChart.data = LineData(dataSet1, dataSet2)
                lineChart.invalidate()
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
//        isVisible = false
    }

    // 设置新数据并刷新图表
    chart.data = LineData(dataSet)    // 允许多个dataSet
    chart.invalidate() // 强制重绘
}