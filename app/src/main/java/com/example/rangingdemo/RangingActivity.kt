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
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rangingdemo.lib.RustFFTWrapper
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.AudioRecordViewModel
import com.example.rangingdemo.viewmodel.AudioTrackViewModel
import com.example.rangingdemo.viewmodel.ClientViewModel
import com.example.rangingdemo.viewmodel.ServerViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RangingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val host = intent.getStringExtra("host") ?: ""
        val isGroupOwner = intent.getBooleanExtra("is_group_owner", false)

        val audioRecordViewModel: AudioRecordViewModel by viewModels()
        val audioTrackViewModel: AudioTrackViewModel by viewModels()

        val serverViewModel: ServerViewModel by viewModels()
        serverViewModel.onMessageReceived = { msg ->
            when (msg) {
                is CmdResponseArray -> {
                    Log.d("CmdResponseArray", msg.array.toString())
                }

                is CmdPong -> {

                }
            }
        }

        val clientViewModel: ClientViewModel by viewModels()
        clientViewModel.onMessageReceived = { msg ->
            when(msg) {
                is CmdStartRecord -> {
                    audioRecordViewModel.start(N)
                }
                is CmdStopRecord -> {
                    audioRecordViewModel.stop()
                }
                is CmdStartPlay -> {
                    val audioData = modulate(ZC_hat, N, f_c, f_s)
                    val stereoAudioData = complexArray2StereoFloatArray(audioData)
                    audioTrackViewModel.start(stereoAudioData, -1)
                }
                is CmdStopPlay -> {
                    audioTrackViewModel.stop()
                }
                is CmdSetParams -> {

                }
                is CmdPing -> {

                }
                is CmdRequestArray -> {

                }

            }
        }

        enableEdgeToEdge()
        setContent {
            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    Column(modifier = Modifier.padding(innerPadding)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Ranging Activity")
                        }
                        if(isGroupOwner) {
                            NewServerUI()
                            HorizontalDivider(thickness = 2.dp)
                        }

                        NewClientUI(host)
                        HorizontalDivider(thickness = 2.dp)
                        MpChartWithStateFlow()
//                        StateFlow2()
                    }
                }
            }
        }
    }
}



private val f_c = 19000


@Composable
fun NewServerUI() {
    val serverViewModel: ServerViewModel = viewModel()

    val isServerRunning by remember { serverViewModel.isRunning }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Server")
    }

    Row {
        Button(onClick = {
            if (isServerRunning) {
                serverViewModel.stopServer()
            } else {
                serverViewModel.startServer()
            }
        }) { Text(if (!isServerRunning) "start server" else "stop server") }
        Spacer(Modifier.padding(10.dp))
        Button(
            onClick = {
                serverViewModel.viewModelScope.launch {
                    serverViewModel.write2AllClient(
                        jsonFormat.encodeToString(
                            CmdStartRecord() as Message // 必须要转成基类
                        )
                    )
                    delay(100)

                    serverViewModel.write2AllClient(
                        jsonFormat.encodeToString(
                            CmdStartPlay() as Message // 必须要转成基类
                        )
                    )
                    delay(5000)

                    serverViewModel.write2AllClient(
                        jsonFormat.encodeToString(
                            CmdStopPlay() as Message // 必须要转成基类
                        )
                    )
                    delay(100)

                    serverViewModel.write2AllClient(
                        jsonFormat.encodeToString(
                            CmdStopRecord() as Message // 必须要转成基类
                        )
                    )

                    // TODO: get distance
                }

            }
        ) { Text("start ranging") }
    }

    Text("Server received: ${serverViewModel.receivedMsg.collectAsState().value}")
}


@Composable
fun NewClientUI(host: String) {
    val clientViewModel: ClientViewModel = viewModel()

    val isClientRunning by remember { clientViewModel.isRunning }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Client")
    }
    Button(onClick = {
        if (isClientRunning) {
            clientViewModel.stopClient()
        } else {
            clientViewModel.startClient(host)
        }
    }) { Text(if (!isClientRunning) "start client" else "stop client") }

    Text("received: ${clientViewModel.receivedMsg.collectAsState().value}")
}


@Composable
fun MpChart(
    modifier: Modifier = Modifier,
    chartData: Pair<LineDataSet, LineDataSet>
) {
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

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            LineChart(context).apply {
                setupLineChart(this)
            }
        },
        update = { lineChart ->
            chartData.let { (dataSet1, dataSet2) ->
                dataSet1.isVisible = showLeft
                dataSet2.isVisible = showRight
                lineChart.data = LineData(dataSet1, dataSet2)
                lineChart.invalidate()
            }

        }
    )
}

//
//@Composable
//fun StateFlow2(
//    viewModel: AudioRecordViewModel = viewModel(),
//    modifier: Modifier = Modifier
//) {
//    // 收集数据
//    val audioChannel by viewModel.audioChannel.collectAsStateWithLifecycle()
//
//    var leftPeekIndex by remember { mutableIntStateOf(0) }
//    var rightPeekIndex by remember { mutableIntStateOf(0) }
//
//    // 处理后台计算
//    LaunchedEffect(audioChannel) {
//        launch(Dispatchers.Default) {    // 在计算型协程中处理数据
//            val (data1, data2) = audioChannel
//            if (data1.isEmpty() || data2.isEmpty()) {
//                return@launch
//            }
//
//            // 左声道处理
//            val leftCir = demodulate(floatArray2ComplexArray(data1), ZC_hat_prime, N_prime, f_c, f_s)
//            val leftMag = magnitude(leftCir)
//            val (leftIndex, _) = getMaxIndexedValue(leftMag)
//
//
//            // 右声道处理
//            val rightCir = demodulate(floatArray2ComplexArray(data2), ZC_hat_prime, N_prime, f_c, f_s)
//            val rightMag = magnitude(rightCir)
//            val (rightIndex, _) = getMaxIndexedValue(rightMag)
//
//            // 更新UI状态
//            withContext(Dispatchers.Main) { // 在UI协程中更新
//                leftPeekIndex = leftIndex
//                rightPeekIndex = rightIndex
//            }
//        }
//
//    }
//}

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