package com.example.rangingdemo.activities

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.RotationAngleViewModel


class GvrActivity : ComponentActivity() {
    private val rotationAngleViewModel: RotationAngleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val angle = rotationAngleViewModel.rotationAngle.collectAsState()

            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Game Rotation Vector Sensor")
                        }
                        Text("range=(-180°, 180°)")
                        Text("angle(degree) = ${angle.value}")
                        Button(onClick = {
                            rotationAngleViewModel.calibrate()
                        }) { Text("calibrate") }
                    }

                }
            }
        }
    }
}

@Composable
fun GrvUIButton() {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(context, GvrActivity::class.java)
            context.startActivity(intent)
        }
    ) {
        Text("GrvActivity")
    }
}


