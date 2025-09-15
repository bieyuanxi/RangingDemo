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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.RotationAngleViewModel


class GvrActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {

            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Grv()
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

@Composable
fun Grv() {
    val rotationAngleViewModel: RotationAngleViewModel = viewModel()
    val angle by rotationAngleViewModel.rotationAngle.collectAsStateWithLifecycle(initialValue = 0.0f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Game Rotation Vector Sensor")
    }
    Text("range=(-180°, 180°), angle = %.1f°".format(angle))
    Button(onClick = {
        rotationAngleViewModel.calibrate()
    }) { Text("calibrate") }
}


