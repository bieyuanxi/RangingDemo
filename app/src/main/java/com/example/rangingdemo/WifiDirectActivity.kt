package com.example.rangingdemo

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
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

        requestConnectionInfo() // 预先请求一次连接信息，更新WifiP2pInfo

        enableEdgeToEdge()
        setContent {
            RangingDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Wifi Direct Control Activity")
                        }

                        WifiDirectInfo(
                            viewModel = wifiDirectViewModel,
                        )

                        Button(onClick = { createGroup() }) {
                            Text("createGroup")
                        }
                        Button(onClick = { discoverPeers() }) {
                            Text("discoverPeers")
                        }
                        Button(onClick = { removeGroup() }) {
                            Text("removeGroup")
                        }
                        Button(onClick = { cancelConnect() }) {
                            Text("cancelConnect")
                        }
                        Button(onClick = { requestConnectionInfo() }) {
                            Text("requestConnectionInfo")
                        }

                        Jump2RangingActivity()
                        DeviceList(wifiDirectViewModel.peers) { device ->
                            connectDevice(device)
                        }
                    }
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

    // 仅用于取消正在进行的连接请求（还未建立连接时
    fun cancelConnect() {
        manager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("cancelConnect", "ok")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e("cancelConnect", "code: $reasonCode")
            }
        })
    }

    fun requestConnectionInfo() {
        val wifiDirectViewModel: WifiDirectViewModel by viewModels()

        manager.requestConnectionInfo(channel) { info ->
            Log.d("connectionListener", "$info")

            wifiDirectViewModel.wifiP2pInfo.value = info
        }
    }


}

@Composable
fun WifiDirectInfo(viewModel: WifiDirectViewModel, modifier: Modifier = Modifier) {
    Text("isWifiP2pEnabled: ${viewModel.isWifiP2pEnabled.value}")
    Text("isGroupFormed: ${viewModel.wifiP2pInfo.value?.groupFormed}")
    Text("isGroupOwner: ${viewModel.wifiP2pInfo.value?.isGroupOwner}")
    Text("GOAddress: : ${viewModel.wifiP2pInfo.value?.groupOwnerAddress}")
}

@Composable
fun DeviceList(devices: List<WifiP2pDevice>, onClick: (device: WifiP2pDevice) -> Unit) {
    Text(text = "发现设备列表👇")
    LazyColumn {
        items(devices) { device ->
            Card(onClick = { onClick(device) }) {
                Text(text = device.deviceName)
            }
        }
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

@Composable
private fun Jump2RangingActivity() {
    val wifiDirectViewModel: WifiDirectViewModel = viewModel()
    val context = LocalContext.current

    var host by remember { mutableStateOf("") }
    var is_group_owner by remember { mutableStateOf(false) }
    host = if (wifiDirectViewModel.wifiP2pInfo.value.groupFormed) {
        wifiDirectViewModel.wifiP2pInfo.value.groupOwnerAddress.hostAddress
    } else {
        ""
    }
    is_group_owner = wifiDirectViewModel.wifiP2pInfo.value.isGroupOwner

    Button(
        onClick = {
            val intent = Intent(context, RangingActivity::class.java)
            intent.putExtra("host", host)
            intent.putExtra("is_group_owner", is_group_owner)
            context.startActivity(intent)
        },
        enabled = (host != "")
    ) {
        Text("Jump 2 Ranging Activity")
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
        Log.d("connectionListener", "$info")

        viewmodel.wifiP2pInfo.value = info

        if (info.groupFormed) {
            // String from WifiP2pInfo struct
//            val groupOwnerAddress = info.groupOwnerAddress.hostAddress

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
                viewmodel.isWifiP2pEnabled.value = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                Log.d("WIFI_P2P_STATE_CHANGED", "${viewmodel.isWifiP2pEnabled.value}")
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {   // 指示可用的对等设备列表已更改。
                Log.d("WIFI_P2P_PEERS_CHANGED", "WIFI_P2P_PEERS_CHANGED_ACTION")
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                manager.requestPeers(channel) { peers: WifiP2pDeviceList? ->
                    // Handle peers list
                    peers?.let { viewmodel.updatePeerList(it) }
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections
                // link: https://developer.android.google.cn/develop/connectivity/network-ops/reading-network-state
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val caps =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

                Log.d("WIFI_P2P_CON_CHANGED", "$caps")
                caps?.let {
                    val isP2pAvailable =
                        it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                && it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

                    if (isP2pAvailable) {
                        manager.requestConnectionInfo(channel, connectionListener)
                    }
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