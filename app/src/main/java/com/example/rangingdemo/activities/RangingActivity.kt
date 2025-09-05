package com.example.rangingdemo.activities

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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rangingdemo.CmdPing
import com.example.rangingdemo.CmdPong
import com.example.rangingdemo.CmdRequestArray
import com.example.rangingdemo.CmdResponseArray
import com.example.rangingdemo.CmdSetParams
import com.example.rangingdemo.CmdStartPlay
import com.example.rangingdemo.CmdStartRecord
import com.example.rangingdemo.CmdStop
import com.example.rangingdemo.CmdStopPlay
import com.example.rangingdemo.CmdStopRecord
import com.example.rangingdemo.Message
import com.example.rangingdemo.MpChartWithStateFlow
import com.example.rangingdemo.N
import com.example.rangingdemo.N_prime
import com.example.rangingdemo.Param
import com.example.rangingdemo.ZC_hat
import com.example.rangingdemo.ZC_hat_prime
import com.example.rangingdemo.complexArray2StereoFloatArray
import com.example.rangingdemo.f_s
import com.example.rangingdemo.get_distance
import com.example.rangingdemo.jsonFormat
import com.example.rangingdemo.modulate
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.AudioProcessingParams
import com.example.rangingdemo.viewmodel.AudioRecordViewModel
import com.example.rangingdemo.viewmodel.AudioTrackViewModel
import com.example.rangingdemo.viewmodel.ClientViewModel
import com.example.rangingdemo.viewmodel.ServerViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO: 应该使用更好的方法
val leftArrays = Array(10) { i ->
    intArrayOf()
}
val rightArrays = Array(10) { i ->
    intArrayOf()
}

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
                    leftArrays[(msg.f_c - start_f_c) / step] = msg.array_left
                    rightArrays[(msg.f_c - start_f_c) / step] = msg.array_right
                    Log.d(
                        "CmdResponseArrayLeft",
                        "fc: ${msg.f_c}, ${msg.array_left.contentToString()}"
                    )
                    Log.d(
                        "CmdResponseArrayRight",
                        "fc: ${msg.f_c}, ${msg.array_right.contentToString()}"
                    )
                }

                is CmdPong -> {

                }
            }
        }

        val clientViewModel: ClientViewModel by viewModels()
        clientViewModel.onMessageReceived = { msg ->
            when (msg) {
                is CmdStop -> {
                    audioTrackViewModel.stop()
                    audioRecordViewModel.stop()
                }

                is CmdSetParams -> {
                    f_c.intValue = msg.f_c
                    val params = msg.params.map { param ->
                        AudioProcessingParams(ZC_hat_prime, N_prime, param.f_c)
                    }
                    audioRecordViewModel.setProcessingParams(params)

                    audioRecordViewModel.start(N)

                    val audioData = modulate(ZC_hat, N, f_c.intValue, f_s)
                    val stereoAudioData = complexArray2StereoFloatArray(audioData)
                    audioTrackViewModel.start(stereoAudioData, -1)
                }

                is CmdPing -> {

                }

                is CmdRequestArray -> {
                    val indexList = audioRecordViewModel.indexList.value
                    val arrayL = IntArray(indexList.size) { i ->
                        indexList[i].first
                    }
                    val arrayR = IntArray(indexList.size) { i ->
                        indexList[i].second
                    }
                    clientViewModel.write(
                        jsonFormat.encodeToString(
                            CmdResponseArray(
                                f_c.intValue,
                                arrayL,
                                arrayR
                            ) as Message
                        )
                    )
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
                        if (isGroupOwner) {
                            NewServerUI()
                            HorizontalDivider(thickness = 2.dp)
                        }

                        NewClientUI(host)
                        HorizontalDivider(thickness = 2.dp)
                        MpChartWithStateFlow(f_c = f_c.intValue, audioRecordViewModel.cirList)
//                        StateFlow2()
                    }
                }
            }
        }
    }
}

// client专用
private var f_c = mutableIntStateOf(19000)

private val start_f_c = 18000
private val step = 1000


@Composable
fun NewServerUI() {
    val serverViewModel: ServerViewModel = viewModel()

    val isServerRunning by remember { serverViewModel.isRunning }

    var distance by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Server")
    }
    val scope = rememberCoroutineScope()


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
                serverViewModel.performRangingJob(start_f_c, step)
            }
        ) { Text("start ranging") }
    }
    val cmdResponseArray by serverViewModel.receivedMsgList.collectAsStateWithLifecycle()
    if (cmdResponseArray.size > 1) {
        for (msg in cmdResponseArray) {
            leftArrays[(msg.f_c - start_f_c) / step] = msg.array_left
            rightArrays[(msg.f_c - start_f_c) / step] = msg.array_right
        }
        distance = get_distance(
            m_aa = leftArrays[0][0],
            m_ab = leftArrays[1][0],
            m_ba = leftArrays[0][1],
            m_bb = leftArrays[1][1],
            N_prime = N,
            N = N
        )
    }

    Text("Server received: ${serverViewModel.receivedMsg.collectAsState().value}")
    Text("distance: $distance")
}


@Composable
fun NewClientUI(host: String) {
    val clientViewModel: ClientViewModel = viewModel()

    val isClientRunning by remember { clientViewModel.isRunning }

    val f_c by remember { f_c }

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
    Text("fc = $f_c")
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