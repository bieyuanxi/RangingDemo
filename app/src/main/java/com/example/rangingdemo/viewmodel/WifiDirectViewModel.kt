package com.example.rangingdemo.viewmodel

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import androidx.lifecycle.ViewModel

class WifiDirectViewModel: ViewModel() {
    var isWifiP2pEnabled = false
    private val peers = mutableListOf<WifiP2pDevice>()

    fun updateThisDevice(thisDevice: WifiP2pDevice) {

    }

    fun updatePeerList(peerList: WifiP2pDeviceList) {
        val refreshedPeers = peerList.deviceList
        peers.clear()
        peers.addAll(refreshedPeers)
    }
}