package com.hallen.zender

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.hallen.zender.databinding.ActivityMainBinding
import com.hallen.zender.databinding.DialogSelectDeviceBinding
import com.hallen.zender.ui.adapters.DeviceAdapter
import com.hallen.zender.ui.adapters.PagerAdapter
import com.hallen.zender.utils.*
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.net.Socket

const val MESSAGE_READ = 1

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiManager: WifiManager
    private lateinit var mManager: WifiP2pManager
    private lateinit var mChannel: WifiP2pManager.Channel
    private lateinit var mReceiver: BroadcastReceiver
    private lateinit var mIntentFilter: IntentFilter
    private lateinit var dialogAdapter: DeviceAdapter
    private var isHost: Boolean? = null
    private val peers: ArrayList<WifiP2pDevice> = arrayListOf()
    private lateinit var files: List<File>

    private lateinit var serverClass: ServerClass
    private lateinit var clientClass: ClientClass
    private lateinit var sendReceive: SendReceive
    val peerListListener = WifiP2pManager.PeerListListener {
        peers.clear()
        peers.addAll(it.deviceList)
        if (!dialogAdapter.dismissed) {
            dialogAdapter.items = peers
            dialogAdapter.notifyDataSetChanged()
            if (peers.isEmpty()) dialogAdapter.runFailed() else dialogAdapter.runSuccess()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadPager()
        loadWifiOjects()
        Permissions(this).turnOnWifi(wifiManager).askStoragePermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        mManager.cancelConnect(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(p0: Int) {}
        })
    }

    private val handler: Handler = Handler { msg ->
        when (msg.what) {
            MESSAGE_READ -> {
                val readBuff = msg.obj as ByteArray
                val tempMsg = String(readBuff, 0, msg.arg1)
                Toas(this@MainActivity, "Message: $tempMsg")
            }
        }
        true
    }

    fun discoverDevices(function: (() -> Unit)? = null) {
        mManager.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                if (function != null) function()
            }

            override fun onFailure(error: Int) {
                Toas(this@MainActivity, "Activa el WiFi y la ubicacion")
            }
        })
    }

    private fun loadWifiOjects() {
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        mManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager.initialize(this, mainLooper, null)
        mReceiver = WiFiDirectBroadcastReceiver(mManager, mChannel, this)
        mIntentFilter = IntentFilter()
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        dialogAdapter = DeviceAdapter()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mReceiver, mIntentFilter)
        discoverDevices()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }

    private fun loadPager() {
        val adapter = PagerAdapter(supportFragmentManager, lifecycle)
        binding.pager.offscreenPageLimit = 4
        binding.pager.adapter = adapter

        //Crea una lista de los fragmentos
        val fragmentsList = listOf(
            "Apps",
            "Galería",
            "Videos",
            "Musica",
            "Otros"
        )
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.setCustomView(R.layout.tab_custom_view)
            tab.text = fragmentsList[position] //Establece el texto para el tab
        }.attach()
    }

    private fun connect(config: WifiP2pConfig) {
        mManager.connect(mChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toas(this@MainActivity, "Conectado")
            }

            override fun onFailure(p0: Int) {
                Toas(this@MainActivity, "No se pudo conectar")
            }

        })
    }

    private fun sendFileCallback() {
        choseDevice {
            val config = WifiP2pConfig()
            config.deviceAddress = it.deviceAddress
            mManager.cancelConnect(mChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    connect(config)
                }

                override fun onFailure(error: Int) {
                    WifiP2pManager.BUSY
                    if (error == WifiP2pManager.BUSY) {
                        connect(config)
                    }
                }
            })
        }
    }

    val connectionInfoListener: WifiP2pManager.ConnectionInfoListener =
        WifiP2pManager.ConnectionInfoListener {
            Toas(this@MainActivity, "connectionInfoListener")
            val groupOwnerAddress = it.groupOwnerAddress
            isHost = it.groupFormed && it.isGroupOwner
            if (isHost!!) {
                serverClass = ServerClass { socket: Socket ->
                    sendReceive = SendReceive(socket, handler)
                    sendReceive.start()
//
                }
                Toas(this, "Server")
                serverClass.start()
            } else {
                clientClass = ClientClass(groupOwnerAddress) { socket: Socket ->
                    sendReceive = SendReceive(socket, handler)
                    sendReceive.start()
                    try {

                        for (file in files) {
                            sendReceive.write(file)
                        }
                    } catch (e: UninitializedPropertyAccessException) {
                        Toas(this, "UninitializedPropertyAccessException")
                    }
                }
                Toas(this, "Client")
                clientClass.start()
            }
        }

    private fun choseDevice(callback: (WifiP2pDevice) -> Unit) {
        val dialog = Dialog(this)
        val dialogBinding = DialogSelectDeviceBinding.inflate(dialog.layoutInflater)
        dialog.apply {
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.apply {
                copyFrom(window?.attributes)
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.CENTER
            }
            window?.attributes = layoutParams
            dialogBinding.listView.adapter = dialogAdapter
            dialogBinding.listView.setOnItemClickListener { _, _, i, _ ->
                dialogAdapter.items[i].primaryDeviceType
                Toas(this@MainActivity, dialogAdapter.items[i].primaryDeviceType)
                callback(dialogAdapter.items[i])
            }
            dialogAdapter.dismissed = false
            dialogAdapter.failed {
                dialogBinding.textView.visibility = View.VISIBLE
                dialogBinding.textView.text = "No se encontró ningun dispositivo"
            }
            dialogAdapter.success {
                dialogBinding.textView.visibility = View.GONE
            }
            if (dialogAdapter.items.size > 0) dialogAdapter.runSuccess()
        }
        dialog.setOnDismissListener {
            dialogAdapter.dismissed = true
        }
        dialog.show()
    }

    fun sendFile(files: List<File>) {
        this.files = files
        discoverDevices { sendFileCallback() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Los permisos han sido aceptados, llamamos la función
                    sendFileCallback()
                } else {
                    // Los permisos han sido denegados
                    Toas(this, "Los permisos de ubicación son necesarios")
                }
            }
            2 -> {
                Permissions(this).checkLocationPermissions()
            }
        }
    }

}