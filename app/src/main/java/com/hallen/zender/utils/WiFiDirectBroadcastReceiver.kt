package com.hallen.zender.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import com.hallen.zender.MainActivity
import com.orhanobut.logger.Logger

/**
 * Clase que extiende de BroadcastReceiver y se encarga de recibir los diferentes eventos de Wifi Direct.
 *
 * @param mManager Instancia de WifiP2pManager.
 * @param mChannel Canal de comunicación.
 * @param activity Instancia de la actividad principal de la aplicación.
 */
class WiFiDirectBroadcastReceiver(
    private val mManager: WifiP2pManager,
    private val mChannel: WifiP2pManager.Channel,
    private val activity: MainActivity
) : BroadcastReceiver() {

    /**
     * Método que se ejecuta cuando se recibe un evento de Wifi Direct.
     *
     * @param context Contexto de la aplicación.
     * @param intent Intent que contiene la información del evento.
     */
    override fun onReceive(context: Context?, intent: Intent) {
        when (intent.action) {
            // Si se cambia el estado de Wifi Direct a deshabilitado, se cambia la lista
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                Logger.i("WIFI_P2P_STATE_CHANGED_ACTION")
                if (intent.getIntExtra(
                        WifiP2pManager.EXTRA_WIFI_STATE,
                        -1
                    ) == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                ) {
                    mManager.requestPeers(mChannel, activity.wifiClass.peerListListener)
                } else activity.wifiClass.peers.postValue(arrayListOf())
            }

            // Si hay cambios en la lista de dispositivos disponibles, se solicita la lista actualizada.
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Logger.i("peers changed")
                mManager.requestPeers(mChannel, activity.wifiClass.peerListListener)
            }

            // Si cambia el estado de la conexión Wi-Fi Direct, se solicita la información de la conexión.
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                Logger.i("WIFI_P2P_CONNECTION_CHANGED_ACTION CALLED")
                val netWorkInfo: NetworkInfo =
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)!!
                if (netWorkInfo.isConnected) {
                    Logger.i("WIFI_P2P_CONNECTION_CHANGED_ACTION: IS CONNECTED")
                    mManager.requestConnectionInfo(
                        mChannel,
                        activity.wifiClass.connectionInfoListener
                    )
                } else {
                    Logger.i("WIFI_P2P_CONNECTION_CHANGED_ACTION: IS NOT CONNECTED")
                }
            }

            // Si cambia la información del dispositivo, se registra en el log.
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                Logger.i("WIFI_P2P_THIS_DEVICE_CHANGED_ACTION")
                mManager.requestPeers(mChannel, activity.wifiClass.peerListListener)
            }
        }
    }
}