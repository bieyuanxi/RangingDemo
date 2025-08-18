package com.example.rangingdemo.viewmodel

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class WifiDirectViewModel: ViewModel() {
    val isWifiP2pEnabled = mutableStateOf(false)
    val peers = mutableStateListOf<WifiP2pDevice>()
    val wifiP2pInfo = mutableStateOf(WifiP2pInfo())

    fun updateThisDevice(thisDevice: WifiP2pDevice) {

    }

    fun updatePeerList(peerList: WifiP2pDeviceList) {
        val refreshedPeers = peerList.deviceList
        peers.clear()
        peers.addAll(refreshedPeers)
    }
}