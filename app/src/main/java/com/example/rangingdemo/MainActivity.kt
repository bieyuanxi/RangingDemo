package com.example.rangingdemo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.rangingdemo.ui.theme.RangingDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val requiredPermissions = getRequiredPermissions()
            StartupMultiplePermissionsChecker(requiredPermissions) {
                // 所有权限都已授予
            }

            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                    MainContent()
                }
            }
        }
    }
}

// 需要使用的权限列表
fun getRequiredPermissions(): List<String> {
    val requiredPermissions = mutableListOf<String>()

    requiredPermissions.add(Manifest.permission.RECORD_AUDIO)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {    // >= android 13
        requiredPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    return requiredPermissions
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RangingDemoTheme {
        Greeting("Android")
    }
}

// 权限检查结果
enum class MultiplePermissionsResult {
    ALL_GRANTED,
    DENIED
}

@Composable
fun StartupMultiplePermissionsChecker(
    requiredPermissions: List<String>,
    onAllPermissionsGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    // 检查所有权限是否已授予
    val allPermissionsGranted = remember(requiredPermissions) {
        requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    var permissionsResult by remember { mutableStateOf<MultiplePermissionsResult?>(null) }

    // 注册多权限请求器
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 检查是否所有权限都被授予
        val allGranted = permissions.all { it.value }
        permissionsResult = if (allGranted) {
            MultiplePermissionsResult.ALL_GRANTED
        } else {
            MultiplePermissionsResult.DENIED
        }

        permissions.forEach { (permission, isGranted) ->
            Log.d("Permission", "$permission: $isGranted")
        }
    }

    // 首次启动时检查权限
    LaunchedEffect(Unit) {
        if (!allPermissionsGranted) {
            // 启动多权限请求
            // 某些系统（如华为harmonyOS 4.2）可能禁用了被拒绝后重新申请权限的授权窗口
            // 只能由用户手动修改权限或重新安装程序
            permissionsLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            permissionsResult = MultiplePermissionsResult.ALL_GRANTED
        }
    }

    // 处理权限结果
    when (permissionsResult) {
        MultiplePermissionsResult.ALL_GRANTED -> {
            onAllPermissionsGranted()
        }
        MultiplePermissionsResult.DENIED -> {
//            // 任何一个权限被拒绝，退出应用
//            LaunchedEffect(Unit) {
//                activity.finish()
//            }
            // TODO: 以某种方式告知用户权限不足
        }
        null -> {

        }
    }
}

// 启动屏，权限检查期间显示
@Composable
fun LaunchScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("正在检查所需权限...")
    }
}



// 应用主内容
@Composable
fun MainContent() {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                val intent = Intent(context, DebugActivity::class.java)
                context.startActivity(intent)
            }
        ) {
            Text("Debug")
        }
    }
}