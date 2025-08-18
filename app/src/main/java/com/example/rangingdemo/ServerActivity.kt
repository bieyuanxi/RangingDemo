package com.example.rangingdemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.ServerViewModel

class ServerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serverViewModel: ServerViewModel by viewModels()
        val isRunning = serverViewModel.isRunning

        val cnt = mutableStateOf(0)

        enableEdgeToEdge()
        setContent {
            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Server Activity")
                        }
                        Button(onClick = {
                            if (isRunning.value) {
                                serverViewModel.stopServer()
                            } else {
                                serverViewModel.startServer()
                            }
                        }) { Text(if (!isRunning.value) "start server" else "stop server") }
                        Button(
                            onClick = {
                                serverViewModel.write(serverViewModel.sockets.value, "hi from server: " + cnt.value++)
                            }
                        ) { Text("send2client") }
                        Text("received: ${serverViewModel.receivedMsg.collectAsState().value}")
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting4(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview5() {
    RangingDemoTheme {
        Greeting4("Android")
    }
}


@Composable
fun ServerUIBtn() {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(context, ServerActivity::class.java)
            context.startActivity(intent)
        }
    ) {
        Text("Server Activity")
    }
}