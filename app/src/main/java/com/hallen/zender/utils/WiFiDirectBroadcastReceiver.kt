package com.hallen.zender.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import com.hallen.zender.MainActivity
import com.orhanobut.logger.Logger

class WiFiDirectBroadcastReceiver(
    private val mManager: WifiP2pManager,
    private val mChannel: WifiP2pManager.Channel,
    private val activity: Activity
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {
        when (intent.action.toString()) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state: Int = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    (activity as MainActivity).discoverDevices()
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Logger.i("peers changed")
                mManager.requestPeers(mChannel, (activity as MainActivity).peerListListener)
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                Logger.i("WIFI_P2P_CONNECTION_CHANGED_ACTION")
                val netWorkInfo: NetworkInfo =
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)!!
                if (netWorkInfo.isConnected) {
                    Logger.i("IS CONNECTED")
                    mManager.requestConnectionInfo(
                        mChannel, (activity as MainActivity).connectionInfoListener
                    )
                } else {
                    Logger.i("IS NOT CONNECTED")
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // do something
            }


        }

    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                else -> false
            }
        } else {
            return connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }
}