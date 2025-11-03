package com.example.rangingdemo.activities

import android.os.Build
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rangingdemo.CmdPing
import com.example.rangingdemo.CmdPong
import com.example.rangingdemo.CmdRequestArray
import com.example.rangingdemo.CmdRequestArrayV2
import com.example.rangingdemo.CmdResponseArray
import com.example.rangingdemo.CmdResponseArrayV2
import com.example.rangingdemo.CmdSetParams
import com.example.rangingdemo.CmdSetParamsV2
import com.example.rangingdemo.CmdSquareMatrix
import com.example.rangingdemo.CmdStop
import com.example.rangingdemo.FlowDataSaver
import com.example.rangingdemo.ui.components.MpChartWithStateFlow
import com.example.rangingdemo.N
import com.example.rangingdemo.N_prime
import com.example.rangingdemo.Param
import com.example.rangingdemo.ParamV2
import com.example.rangingdemo.ZC_hat
import com.example.rangingdemo.ZC_hat_prime
import com.example.rangingdemo.calculateAngle
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
import com.example.rangingdemo.ui.components.RotatePhoneDialog
import com.example.rangingdemo.getAngleFromFile
import com.example.rangingdemo.readCsv
import com.example.rangingdemo.squareMatrix
import com.example.rangingdemo.ui.components.DeviceInfo
import com.example.rangingdemo.ui.components.DeviceMapVisualizer
import com.example.rangingdemo.viewmodel.SquareMatrixViewModel
import java.io.File

class RangingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val host = intent.getStringExtra("host") ?: ""
        val isGroupOwner = intent.getBooleanExtra("is_group_owner", false)

        val audioRecordViewModel: AudioRecordViewModel by viewModels()
        val audioTrackViewModel: AudioTrackViewModel by viewModels()

        val clientViewModel: ClientViewModel by viewModels()
        val squareMatrixViewModel: SquareMatrixViewModel by viewModels()
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
                            modulate(   // TODO: normalization
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

                is CmdSetParamsV2 -> {
                    f_c.intValue = msg.params[msg.index].f_c

                    squareMatrixViewModel.index.intValue = msg.index
                    squareMatrixViewModel.leftSquareMatrix = squareMatrix(msg.params.size)
                    squareMatrixViewModel.rightSquareMatrix = squareMatrix(msg.params.size)

                    val params = msg.params.map { param ->
                        AudioProcessingParams(ZC_hat_prime, N_prime, param.f_c)
                    }
                    audioRecordViewModel.setProcessingParams(params)
                    audioRecordViewModel.start(frameLen = N)

                    val timeTaken1 = measureTimeMillis {
                        val stereoAudioData: FloatArray = consumeComplexArray2StereoFloatArray(
                            modulate(   // TODO: normalization
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

                    Log.d("CmdSetParamsV2TimeTaken", "$timeTaken1 ms")
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

                is CmdRequestArrayV2 -> {
                    val indexList = audioRecordViewModel.indexList.value
                    audioRecordViewModel.stopUpdate = true  // 上传数据时停止更新，保持当前数据状态
                    // SAFETY: map each element of arrayL & arrayR with AudioProcessingParams
                    val arrayL = IntArray(indexList.size) { i ->
                        indexList[i].first.first
                    }
                    val arrayR = IntArray(indexList.size) { i ->
                        indexList[i].second.first
                    }
                    clientViewModel.write(
                        CmdResponseArrayV2(
                            squareMatrixViewModel.index.intValue,
                            arrayL,
                            arrayR
                        )
                    )
                }

                is CmdSquareMatrix -> {
                    squareMatrixViewModel.leftSquareMatrix = msg.left_matrix
                    squareMatrixViewModel.rightSquareMatrix = msg.right_matrix

                    val matrix = squareMatrixViewModel.rightSquareMatrix
                    val size = matrix.size
                    val index = squareMatrixViewModel.index.intValue

                    val array = Array(size) { 0f }
                    for (i in 0 until size) {
                        if (i != index) {
                            val distance = get_distance(
                                matrix[i][i],
                                matrix[i][index],
                                matrix[index][index],
                                matrix[index][i],
                                N_prime = N,
                                N = N
                            )
                            array[i] = distance
                            Log.d("distance", "$i -> $index: $distance")
                        }
                    }
                    squareMatrixViewModel.distances.value = array
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

lateinit var leftSquareMatrix: Array<Array<Int>>
lateinit var rightSquareMatrix: Array<Array<Int>>

@Composable
fun NewServerUI() {
    val serverViewModel: ServerViewModel = viewModel()

    val isServerRunning by remember { serverViewModel.isRunning }
    // 已建立连接的设备数量
    val clientCounter by remember { serverViewModel.clientCounter }

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

            is CmdResponseArrayV2 -> {
                leftMatrix[msg.index] = msg.array_left
                rightMatrix[msg.index] = msg.array_right

                leftSquareMatrix[msg.index] = msg.array_left.toTypedArray()
                rightSquareMatrix[msg.index] = msg.array_right.toTypedArray()

                cmdResponseArrayCount += 1

                if (cmdResponseArrayCount > 1 && cmdResponseArrayCount == clientCounter) {
                    serverViewModel.write2AllClient(
                        CmdSquareMatrix(
                            leftSquareMatrix,
                            rightSquareMatrix
                        )
                    )
                }

                Log.d(
                    "CmdResponseArrayLeft",
                    "index: ${msg.index}, ${msg.array_left.contentToString()}"
                )
                Log.d(
                    "CmdResponseArrayRight",
                    "index: ${msg.index}, ${msg.array_right.contentToString()}"
                )
            }

            is CmdPong -> {

            }
        }
    }


    val deviceInfos = remember { mutableStateListOf<DeviceInfo>() }
    var angleOffset by remember { mutableFloatStateOf(0.0f) }

    var distance by remember { mutableFloatStateOf(0f) }
    if (cmdResponseArrayCount > 1 && cmdResponseArrayCount == clientCounter) {    // 收到全部设备数据时计算
        distance = get_distance(    // 这里只计算前两个设备的距离
            m_aa = rightSquareMatrix[0][0],
            m_ab = rightSquareMatrix[1][0],
            m_ba = rightSquareMatrix[0][1],
            m_bb = rightSquareMatrix[1][1],
            N_prime = N,
            N = N
        )
        if (deviceInfos.isNotEmpty()) {
            deviceInfos[0].distance = distance  // TODO: 多设备
        } else {
            deviceInfos.add(DeviceInfo(distance, angleOffset, "device"))
        }

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
                    leftSquareMatrix = squareMatrix(count)
                    rightSquareMatrix = squareMatrix(count)

                    allocateParamList(
                        deviceCnt = count,
                        start_f_c,
                        step
                    ).toTypedArray()
                }
            }
        ) { Text("开始测距v2") }
    }

    Text("distance: $distance")

    val context = LocalContext.current

    val audioRecordViewModel: AudioRecordViewModel = viewModel()
    val rotationAngleViewModel: RotationAngleViewModel = viewModel()

    val grVAngle by rotationAngleViewModel.rotationAngle.collectAsStateWithLifecycle(initialValue = 0.0f)

    // 状态管理
    var isRotatePhoneDialogShowing by remember { mutableStateOf(false) } // 弹窗显示状态
    var isAngleDialogShowing by remember { mutableStateOf(false) } // 摇一摇

    // 保存数据
    val flowSaver = remember {
        FlowDataSaver(rotationAngleViewModel.viewModelScope)
    }


    Row {
        Button(
            onClick = {
                rotationAngleViewModel.calibrate()  // 校准为0
                angleOffset = 0f
                deviceInfos.clear()
                isRotatePhoneDialogShowing = true  // 提示旋转手机

                val paramsArray = allocateParamList(
                    deviceCnt = 1,
                    start_f_c,
                    step
                ).toTypedArray()

                val params = paramsArray.map { param ->
                    AudioProcessingParams(ZC_hat_prime, N_prime, param.f_c)
                }

                audioRecordViewModel.setProcessingParams(params)
                audioRecordViewModel.start()

                serverViewModel.write2AllClient(
                    CmdSetParamsV2(
                        0, // deviceCnt == 1
                        paramsArray
                    )
                )

                // 记录旋转角和音频数据   // TODO: 考虑添加延迟，因为录音和播放音频以及网络延迟
                val fileName = "${Build.MODEL}_angle_audio_${System.currentTimeMillis()}.csv"
                val outputFile = File(context.filesDir, fileName)
                flowSaver.init(outputFile)
                flowSaver.saveFlows(
                    rotationAngleViewModel.rotationAngle,
                    audioRecordViewModel.indexList
                )
            }
        ) { Text("测角（发现模式）") }  // 以旋转360度的方式发现周围的设备（通过录制音频）

        Button(onClick = {
            isAngleDialogShowing = !isAngleDialogShowing
            if (isAngleDialogShowing) {
                rotationAngleViewModel.calibrate()  // 校准为0
                angleOffset = 0f
                deviceInfos.clear()

                val paramsArray = allocateParamList(
                    deviceCnt = 1,
                    start_f_c,
                    step
                ).toTypedArray()

                val params = paramsArray.map { param ->
                    AudioProcessingParams(ZC_hat_prime, N_prime, param.f_c)
                }

                audioRecordViewModel.setProcessingParams(params)
                audioRecordViewModel.start()

                serverViewModel.write2AllClient(
                    CmdSetParamsV2(
                        0, // deviceCnt == 1
                        paramsArray
                    )
                )

                // 记录旋转角和音频数据   // TODO: 考虑添加延迟，因为录音和播放音频以及网络延迟
                val fileName = "${Build.MODEL}_angle_audio_${System.currentTimeMillis()}.csv"
                val outputFile = File(context.filesDir, fileName)
                flowSaver.init(outputFile)
                flowSaver.saveFlows(
                    rotationAngleViewModel.rotationAngle,
                    audioRecordViewModel.indexList
                )
            } else {
                serverViewModel.write2AllClient(CmdStop())
                audioRecordViewModel.stop()

                flowSaver.close()   // 关闭文件流

                // 调用算法
                val inputFile = File(context.filesDir, flowSaver.fileName ?: "NotExistFile")
                val (angleRaw, diffRaw) = readCsv(inputFile)
                val angleOffsetCandidate =
                    calculateAngle(angleRaw.toDoubleArray(), diffRaw.toDoubleArray())
                angleOffset = angleOffsetCandidate.toFloat()
                deviceInfos.add(
                    DeviceInfo(
                        distance = if (distance > 0) distance else 1e-3f,
                        angleOffset,
                        "device"
                    )
                )
            }
        }) { Text(if (!isAngleDialogShowing) "测角（摇一摇）" else "结束测角") }
    }

    Text(
        "algo: %.1f°, grv: %.1f°, angle = %.1f°".format(
            angleOffset,
            grVAngle,
            angleOffset - grVAngle
        )
    )

    DeviceMapVisualizer(
        deviceInfos = deviceInfos,
        currentAngle = grVAngle,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )

    // 旋转提示弹窗
    RotatePhoneDialog(
        isShowing = isRotatePhoneDialogShowing,
        rotationProgress = if (grVAngle < 0) 360 + grVAngle else grVAngle,
        onDismiss = {   // 旋转完成
            serverViewModel.write2AllClient(CmdStop())
            audioRecordViewModel.stop()

            flowSaver.close()   // 关闭文件流

            // 调用算法
            val inputFile = File(context.filesDir, flowSaver.fileName ?: "NotExistFile")
            val angleOffsetCandidate = getAngleFromFile(inputFile)
            // FIXME: IndexOutOfBoundsException: Empty list doesn't contain element at index 0.
            angleOffset = angleOffsetCandidate[0].toFloat()
            // 关闭弹窗
            isRotatePhoneDialogShowing = false
            deviceInfos.add(
                DeviceInfo(
                    if (distance > 0) distance else 1e-3f,
                    angleOffset,
                    "device"
                )
            )
        }
    )
}


@Composable
fun NewClientUI(host: String) {
    val clientViewModel: ClientViewModel = viewModel()
    val squareMatrixViewModel: SquareMatrixViewModel = viewModel()

    val isClientRunning by remember { clientViewModel.isRunning }

    val distances by remember { squareMatrixViewModel.distances }
    val index by remember { squareMatrixViewModel.index }

    val f_c by remember { f_c }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Client(fc = $f_c, index = $index)")
    }
    Button(onClick = {
        if (isClientRunning) {
            clientViewModel.stopClient()
        } else {
            clientViewModel.startClient(host)
        }
    }) { Text(if (!isClientRunning) "start client" else "stop client") }
    Column {
        for (i in 0 until distances.size) {
            if(i != index) {
                Text("$i -> $index: ${distances[i]}")
            }
        }
    }
}

/**
 * 默认的参数分配策略
 */
fun allocateParamList(deviceCnt: Int, start_f_c: Int, step: Int): List<ParamV2> {
    val paramsList = (0 until deviceCnt).map { index ->
        ParamV2(start_f_c + step * index, 1, 37, 480 * 40)
    }
    return paramsList
}