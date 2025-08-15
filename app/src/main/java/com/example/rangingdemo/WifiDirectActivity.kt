package com.example.rangingdemo

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.rangingdemo.ui.theme.RangingDemoTheme
import com.example.rangingdemo.viewmodel.WifiDirectViewModel

class WifiDirectActivity : ComponentActivity() {
    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: BroadcastReceiver

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val wifiDirectViewModel: WifiDirectViewModel by viewModels()

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
        receiver = WiFiDirectBroadcastReceiver(manager, channel, wifiDirectViewModel)

        enableEdgeToEdge()
        setContent {
            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting3(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    /* register the broadcast receiver with the intent values to be matched */
    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, intentFilter)
        }
    }

    /* unregister the broadcast receiver */
    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    @SuppressLint("MissingPermission")
    fun createGroup() {
        manager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("createGroup", "ok")
            }

            override fun onFailure(reason: Int) {
                Log.d("createGroup", "failure, code: $reason")
            }
        })
    }

    fun removeGroup() {
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("removeGroup", "ok")
            }

            override fun onFailure(reason: Int) {
                Log.d("removeGroup", "failure, code: $reason")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        Log.d("discoverPeers", "try discoverPeers")
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("discoverPeers", "ok")
            }

            override fun onFailure(code: Int) {
                Log.e("discoverPeers", "failure, code: $code")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("connectDevice", "${device.deviceName}: ok")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e("connectDevice", "code: $reasonCode")
            }
        })
    }

    fun disconnect() {
        manager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("cancelConnect", "ok")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e("cancelConnect", "code: $reasonCode")
            }
        })
    }


}

@Composable
fun Greeting3(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Wifi Direct Control Activity",
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


@Composable
fun WifiDirectUIBtn() {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(context, WifiDirectActivity::class.java)
            context.startActivity(intent)
        }
    ) {
        Text("WifiDirect")
    }
}

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val viewmodel: WifiDirectViewModel
) : BroadcastReceiver() {

    private val connectionListener = WifiP2pManager.ConnectionInfoListener { info ->
        // String from WifiP2pInfo struct
        val groupOwnerAddress = info.groupOwnerAddress.hostAddress

        // After the group negotiation, we can determine the group owner
        // (server).
        if (info.groupFormed) {
            if (info.isGroupOwner) {
                // Do whatever tasks are specific to the group owner.
                // One common case is creating a group owner thread and accepting
                // incoming connections.
            } else {
                // The other device acts as the peer (client). In this case,
                // you'll want to create a peer thread that connects
                // to the group owner.
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {   // 指示是否启用 WLAN 直连
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                viewmodel.isWifiP2pEnabled = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {   // 指示可用的对等设备列表已更改。
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                manager.requestPeers(channel) { peers: WifiP2pDeviceList? ->
                    // Handle peers list
                    peers?.let { viewmodel.updatePeerList(it) }
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections
//                val networkInfo =
//                    intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
//
//                if (networkInfo?.isConnected == true) {
//                    // We are connected with the other device, request connection
//                    // info to find group owner IP
//                    manager.requestConnectionInfo(channel, connectionListener)
//                }


                // ---------------------replacement--------------------------//
                // link: https://developer.android.google.cn/develop/connectivity/network-ops/reading-network-state
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

                val isP2pAvailable =
                    caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_WIFI_P2P) == true && caps.hasTransport(
                        NetworkCapabilities.TRANSPORT_WIFI
                    )

                if (isP2pAvailable) {
                    manager.requestConnectionInfo(channel, connectionListener)
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's wifi state changing
                val thisDevice = intent.getParcelableExtra<WifiP2pDevice>(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
                )
                thisDevice?.let { viewmodel.updateThisDevice(it) }
            }
        }
    }
}