package com.example.rangingdemo.activities

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rangingdemo.CmdPing
import com.example.rangingdemo.CmdPong
import com.example.rangingdemo.CmdRequestArray
import com.example.rangingdemo.CmdResponseArray
import com.example.rangingdemo.CmdSetParams
import com.example.rangingdemo.CmdStop
import com.example.rangingdemo.MpChartWithStateFlow
import com.example.rangingdemo.N
import com.example.rangingdemo.N_prime
import com.example.rangingdemo.Param
import com.example.rangingdemo.ZC_hat
import com.example.rangingdemo.ZC_hat_prime
import com.example.rangingdemo.consumeComplexArray2StereoFloatArray
import com.example.rangingdemo.f_s
import com.example.rangingdemo.get_distance
import com.example.rangingdemo.modulate
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.AudioProcessingParams
import com.example.rangingdemo.viewmodel.AudioRecordViewModel
import com.example.rangingdemo.viewmodel.AudioTrackViewModel
import com.example.rangingdemo.viewmodel.ClientViewModel
import com.example.rangingdemo.viewmodel.RotationAngleViewModel
import com.example.rangingdemo.viewmodel.ServerViewModel
import kotlin.system.measureTimeMillis
import com.example.rangingdemo.RotatePhoneDialog

class RangingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val host = intent.getStringExtra("host") ?: ""
        val isGroupOwner = intent.getBooleanExtra("is_group_owner", false)

        val audioRecordViewModel: AudioRecordViewModel by viewModels()
        val audioTrackViewModel: AudioTrackViewModel by viewModels()

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
                    audioRecordViewModel.start(frameLen = N)

                    val timeTaken1 = measureTimeMillis {
                        val stereoAudioData: FloatArray = consumeComplexArray2StereoFloatArray(
                            modulate(
                                ZC_hat,
                                N,
                                f_c.intValue,
                                f_s
                            ),
                            leftRate = 10.0f,
                            rightRate = 10.0f
                        )
                        audioTrackViewModel.start(stereoAudioData, -1)
                    }

                    Log.d("CmdSetParamsTimeTaken", "$timeTaken1 ms")
                }

                is CmdPing -> {

                }

                is CmdRequestArray -> {
                    val indexList = audioRecordViewModel.indexList.value
                    audioRecordViewModel.stopUpdate = true  // 上传数据时停止更新，保持当前数据状态
                    val arrayL = IntArray(indexList.size) { i ->
                        indexList[i].first.first
                    }
                    val arrayR = IntArray(indexList.size) { i ->
                        indexList[i].second.first
                    }
                    clientViewModel.write(
                        CmdResponseArray(
                            f_c.intValue,
                            arrayL,
                            arrayR
                        )
                    )
                }

            }
        }

        enableEdgeToEdge()
        setContent {
            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val cirList by audioRecordViewModel.cirList.collectAsStateWithLifecycle(
                        initialValue = emptyList()
                    )

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
                        AngleUI()
                        NewClientUI(host)
                        HorizontalDivider(thickness = 2.dp)
                        MpChartWithStateFlow(f_c = f_c.intValue, cirList)
//                        StateFlow2()

                    }
                }
            }
        }
    }
}

// client专用
private var f_c = mutableIntStateOf(0)

private val start_f_c = 18000
private val step = 1000

// TODO: 应该使用更好的方法
val leftMatrix = Array(10) {
    intArrayOf()
}
val rightMatrix = Array(10) {
    intArrayOf()
}

@Composable
fun NewServerUI() {
    val serverViewModel: ServerViewModel = viewModel()

    // 接收到返回数据的客户端设备数量
    var cmdResponseArrayCount by remember { mutableIntStateOf(0) }
    serverViewModel.onMessageReceived = { msg ->
        when (msg) {
            is CmdResponseArray -> {
                leftMatrix[(msg.f_c - start_f_c) / step] = msg.array_left
                rightMatrix[(msg.f_c - start_f_c) / step] = msg.array_right
                cmdResponseArrayCount += 1
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

    val isServerRunning by remember { serverViewModel.isRunning }
    // 已建立连接的设备数量
    val clientCounter by remember { serverViewModel.clientCounter }

    var distance by remember { mutableFloatStateOf(0f) }
    if (cmdResponseArrayCount > 1 && cmdResponseArrayCount == clientCounter) {    // 收到全部设备数据时计算
        distance = get_distance(    // 这里只计算前两个设备的距离
            m_aa = rightMatrix[0][0],
            m_ab = rightMatrix[1][0],
            m_ba = rightMatrix[0][1],
            m_bb = rightMatrix[1][1],
            N_prime = N,
            N = N
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Server(client count: ${clientCounter})")
    }

    Row {
        Button(onClick = {
            if (isServerRunning) {
                serverViewModel.stopServer()
            } else {
                serverViewModel.startServer()
            }
        }) { Text(if (!isServerRunning) "start server" else "stop server") }
        Spacer(Modifier.padding(2.dp))
        Button(
            onClick = {
                cmdResponseArrayCount = 0   // 初始接收到返回数据的客户端设备数量为0
                serverViewModel.performRangingJob() { count ->
                    allocateParamList(
                        deviceCnt = count,
                        start_f_c,
                        step
                    ).toTypedArray()
                }
            }
        ) { Text("开始测距") }
    }

    Text("distance: $distance")
}


/**
 * 测角UI
 */
@Composable
fun AngleUI() {
    val serverViewModel: ServerViewModel = viewModel()
    val audioRecordViewModel: AudioRecordViewModel = viewModel()
    val rotationAngleViewModel: RotationAngleViewModel = viewModel()

    val angle by rotationAngleViewModel.rotationAngle.collectAsStateWithLifecycle(initialValue = 0.0f)

    // 状态管理
    var isDialogShowing by remember { mutableStateOf(false) } // 弹窗显示状态
    var rotationProgress by remember { mutableStateOf(0f) } // 旋转进度（0f~360f）

    Button(
        onClick = {
            // TODO
            audioRecordViewModel.start()

            val paramsArray = allocateParamList(
                deviceCnt = 1,
                start_f_c,
                step
            ).toTypedArray()

            serverViewModel.write2AllClient(CmdSetParams(
                paramsArray[0].f_c, // deviceCnt == 1
                N,
                paramsArray
            ))

            // 提示旋转手机
            rotationAngleViewModel.calibrate()  // 校准为0
            isDialogShowing = true

            // 旋转完成
            serverViewModel.write2AllClient(CmdStop())
            audioRecordViewModel.stop()
            // 调用算法
        }
    ) { Text("开始测角") }

    // 旋转提示弹窗
    if (isDialogShowing) {
        val angle = angle.toInt()
        RotatePhoneDialog(
            isShowing = isDialogShowing,
            rotationProgress = if (angle < 0) 360 + angle else angle,
            onDismiss = { isDialogShowing = false } // 关闭弹窗
        )
    }

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
        Text("Client(fc = $f_c)")
    }
    Button(onClick = {
        if (isClientRunning) {
            clientViewModel.stopClient()
        } else {
            clientViewModel.startClient(host)
        }
    }) { Text(if (!isClientRunning) "start client" else "stop client") }
//    Text("received: ${clientViewModel.receivedMsg.collectAsState().value}")
}

/**
 * 默认的参数分配策略
 */
fun allocateParamList(deviceCnt: Int, start_f_c: Int, step: Int): List<Param> {
    val paramsList = (0 until deviceCnt).map { index ->
        Param(start_f_c + step * index, 1, 37)
    }
    return paramsList
}