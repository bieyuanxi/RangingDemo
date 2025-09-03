package com.example.rangingdemo

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
import androidx.compose.runtime.snapshotFlow
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
import com.example.rangingdemo.viewmodel.AudioProcessingParams
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.flow.StateFlow


private val f_c = 19000

class AudioActivity : ComponentActivity() {
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

// 优化后的Composable
@Composable
fun MpChartWithStateFlow(
    f_c: Int,
    cirListStateFlow: StateFlow<List<Pair<FloatArray, FloatArray>>>,
    viewModel: AudioRecordViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val processingParams by viewModel.processingParams.collectAsStateWithLifecycle()

    val isLeftVisibleList = remember(processingParams) {
        mutableStateListOf<Boolean>().apply() {
            repeat(processingParams.size) {
                add(true)
            }
        }
    }

    val isRightVisibleList = remember(processingParams) {
        mutableStateListOf<Boolean>().apply() {
            repeat(processingParams.size) {
                add(true)
            }
        }
    }

    val fcList = remember(processingParams) {
        buildList {
            processingParams.forEach {
                add(it.f_c)
            }
        }
    }

    Column {
        processingParams.forEachIndexed { i, params ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("f_c=${params.f_c} ")
                Text("lChannel")
                Checkbox(checked = isLeftVisibleList[i], onCheckedChange = { isLeftVisibleList[i] = it })
                Text("rChannel")
                Checkbox(checked = isRightVisibleList[i], onCheckedChange = { isRightVisibleList[i] = it })
            }
        }
    }

    val cirList by cirListStateFlow.collectAsStateWithLifecycle()
    val lineDataSetList = remember { mutableStateListOf<LineDataSet>() }

    LaunchedEffect(cirList, isLeftVisibleList.toList(), isRightVisibleList.toList()) {
        lineDataSetList.forEach {
            it.clear()  // 释放LineDataSet持有的entries
        }
        lineDataSetList.clear() // 清空数据

        cirList.forEachIndexed { index, pair ->
            // TODO: 减少对象分配频率
            val lEntry = pair.first.mapIndexed { i, fl ->
                Entry(i.toFloat(), fl)
            }
            val rEntry = pair.second.mapIndexed { i, fl ->
                Entry(i.toFloat(), fl)
            }

            // 创建数据集
            val lDataSet = LineDataSet(lEntry, "${fcList[index]}L").apply {
                color = Color.BLUE
                isVisible = isLeftVisibleList.getOrElse(index) { true }
                setDrawValues(false)
            }
            val rDataSet = LineDataSet(rEntry, "${fcList[index]}R").apply {
                color = Color.GREEN
                isVisible = isRightVisibleList.getOrElse(index) { true }
                setDrawValues(false)
            }
            lineDataSetList.add(lDataSet)
            lineDataSetList.add(rDataSet)
        }
    }

    // TODO: 索引展示
    var leftPeekIndex by remember { mutableIntStateOf(0) }
    var rightPeekIndex by remember { mutableIntStateOf(0) }

    Row {
        Text("mL: $leftPeekIndex")
        Spacer(modifier = Modifier.width(5.dp))
        Text("mR: $rightPeekIndex")
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            Log.d("LineChartInit", "AndroidView LineChart init")
            LineChart(context).apply {
                setupLineChart(this)
            }
        },
        update = { lineChart ->
            lineChart.clear()
            lineChart.data = LineData(lineDataSetList.toList())
            lineChart.invalidate()
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