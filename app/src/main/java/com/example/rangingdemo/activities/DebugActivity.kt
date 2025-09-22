package com.example.rangingdemo.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.rangingdemo.helloPython
import com.example.rangingdemo.npVersion
import com.example.rangingdemo.pandasVersion
import com.example.rangingdemo.ui.components.PhoneNetworkExample
import com.example.rangingdemo.ui.theme.RangingDemoTheme


class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        Log.d("hello_python", helloPython())
        Log.d("np_version", npVersion())
        Log.d("pandas_version", pandasVersion())

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Debug Activity")
                        }
                        AudioUIBtn()
                        OfdmUIBtn()
                        WifiDirectUIBtn()
                        GrvUIButton()
                        AngleUIBtn()
                        PhoneNetworkExample()
                    }

                }
            }
        }
    }
}

@Composable
fun Greeting2(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "This is debug activity",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    RangingDemoTheme {
        Greeting2("Android")
    }
}


