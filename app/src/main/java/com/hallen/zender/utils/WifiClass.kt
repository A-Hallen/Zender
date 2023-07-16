package com.hallen.zender.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.IBinder
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.hallen.zender.MainActivity
import com.hallen.zender.databinding.DialogSelectDeviceBinding
import com.hallen.zender.ui.adapters.DeviceAdapter
import com.hallen.zender.utils.services.TransferService
import com.orhanobut.logger.Logger
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class WifiClass(private val ctx: Context) : ServiceConnection {
    val peers = MutableLiveData<MutableCollection<WifiP2pDevice>>()
    private var isHost: Boolean? = null
    private val mainLooper = ctx.mainLooper
    private lateinit var serverClass: ServerClass
    private lateinit var clientClass: ClientClass
    private var sendReceive: SendReceive? = null
    private val mManager = ctx.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val mChannel = mManager.initialize(ctx, mainLooper, null)
    private val mReceiver = WiFiDirectBroadcastReceiver(mManager, mChannel, ctx as MainActivity)
    private val mIntentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null
    private var sending = false
    private var transferService: TransferService? = null
    private var isBound = false
    private lateinit var progressBar: ProgressBar

    fun setProgressBar(p: ProgressBar) {
        progressBar = p
    }

    fun getProgressBar(): ProgressBar = progressBar

    fun registerReceiver() = ctx.registerReceiver(mReceiver, mIntentFilter)
    fun unregisterReceiver() = ctx.unregisterReceiver(mReceiver)

    fun sendFiles() {
        if (sending) {
            sending = false
            Logger.i("SENDING FILES")
            if (isBound) {
                Logger.i("SEND FILES CALLED")
                transferService!!.sendFiles((ctx as MainActivity).files)
                ctx.files.clear()
            }

//            if (sendReceive != null) {
//
//
//                sendReceive!!.write(files)
//            } else Logger.i("sendReceive is null")
        } else Logger.i("RECEIVING FILES")
    }

    /**
     * Crea un grupo de dispositivos usando la API WifiP2pManager.
     */
    fun createGroup(callBack: (Int?) -> Unit) {
        Logger.i("createGroup called")
        if (ActivityCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                ctx,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            (ctx as MainActivity).turnOnWifi()
            return
        } else {

            mManager.createGroup(mChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Si la creación del grupo es exitosa, mostrar un mensaje de éxito
                    callBack(null)
                }

                override fun onFailure(error: Int) {
                    if (error == WifiP2pManager.BUSY) { // Si el dispositivo esta ocupado desconectarse
                        disconnectGroup {
                            createGroup(callBack)
                        }
                    } else callBack(error)
                }
            })
        }

    }

    /**
    Inicia la búsqueda de dispositivos cercanos disponibles para conectarse mediante Wi-Fi Direct.
    Si se proporciona una función como parámetro, esta se llamará después de que se inicie la búsqueda.
    Se utiliza el objeto mManager de tipo WifiP2pManager para iniciar la búsqueda de dispositivos
    cercanos disponibles para conectarse mediante Wi-Fi Direct. Se proporciona un objeto anónimo
    de tipo WifiP2pManager.ActionListener para manejar las respuestas de éxito o fracaso de la
    operación.
     */
    @SuppressLint("MissingPermission")
    fun discoverDevices(send: Boolean = true) {
        mManager.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { // la busqueda se inicio correctamente
                choseDevice { device: WifiP2pDevice -> // se llama a la funcion chooseDevice que crea el dialogo
                    sending = send
                    // el dialogo devuelve un objeto WifiP2pDevice al escoger uno
                    val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
                    Logger.i("Discover called")
                    mManager.requestGroupInfo(mChannel) { groupInfo: WifiP2pGroup? ->
                        Logger.i("groupInfo.networkName ${groupInfo?.networkName}")
                        if (groupInfo != null) { // Si el grupo es el host
                            val client = groupInfo.clientList.map { client -> client.deviceAddress }
                            if (config.deviceAddress in client || groupInfo.owner.deviceAddress == config.deviceAddress) {
                                Logger.i("Ya esta conectado al dispositivo")
                                sendFiles() // si ya esta conectado al dispositivo se envia el archivo
                            } else {
                                Toas(ctx, "Estas conectado a un grupo")
                                disconnectGroup { connect(config) }
                            } // de lo contrario si esta conectado a otro se desconecta primero
                        } else connect(config) // Si no se esta conectado a ningun dispositivo se conecta
                    }
                }
            }

            // Si la operación falló, se muestra un mensaje de error en una notificación Toast.
            override fun onFailure(error: Int) {
                if (error == WifiP2pManager.BUSY) {
                    disconnectGroup {
                        Toast.makeText(
                            ctx, "Error al escanear dispositivos: $error", Toast.LENGTH_SHORT
                        ).show()
                        discoverDevices()
                    }
                } else {
                    Toast.makeText(ctx, "Activa el WiFi y la ubicación: $error", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        })
    }

    /**
     * Función que desconecta el dispositivo del grupo actual.
     * @param callBack funcion que se ejecuta el desconectarse
     */
    fun disconnectGroup(callBack: () -> Unit) {
        mManager.removeGroup(mChannel, object : WifiP2pManager.ActionListener {
            /**
             * Función que se ejecuta si se logró desconectar del grupo actual.
             * Se utiliza para actualizar el valor de la variable "isHost" y conectar a otro grupo.
             */
            override fun onSuccess() {
                Logger.i("Desconectado con exito del grupo actual")
                isHost = null
                callBack()
            }

            /**
             * Función que se ejecuta si ocurre un error al intentar desconectarse del grupo actual.
             * @param error el código de error correspondiente al fallo.
             */
            override fun onFailure(error: Int) {
                sending = false
            }
        })
    }

    // Cuando la búsqueda de resultados se mostraran aqui
    val peerListListener = WifiP2pManager.PeerListListener { deviceList: WifiP2pDeviceList ->
        Logger.i("peerListener llamado con exito: ${deviceList.deviceList.size}")
        peers.value = deviceList.deviceList
    }

    /**
     * Muestra un diálogo con una lista de dispositivos disponibles y llama al callback con el dispositivo elegido.
     *
     * @param callback función que se llamará con el dispositivo seleccionado.
     */
    private fun choseDevice(callback: (WifiP2pDevice) -> Unit) {
        val dialog = ZDialog(ctx) // Creamos el diálogo
        val dialogAdapter = DeviceAdapter() // Creamos el adaptador para la lista de dispositivos
        val binding = DialogSelectDeviceBinding.inflate(dialog.layoutInflater)
        dialog.apply {
            setContentView(binding.root) // Establecemos el layout del diálogo
            with(binding) {
                listView.adapter = dialogAdapter // Establecemos el adaptador a la lista
                // Creamos un observer que se llamará cada vez que cambie la lista de dispositivos
                val observer = Observer<MutableCollection<WifiP2pDevice>> {
                    if (it.isEmpty()) {
                        // Si la lista está vacía, mostramos un mensaje indicándolo
                        textView.visibility = View.VISIBLE
                        textView.text = "No se encontró ningún dispositivo"
                    } else {
                        // Si hay dispositivos, ocultamos el mensaje
                        textView.visibility = View.GONE
                    }
                    dialogAdapter.newItems(it) //Actualizamos el adaptador
                }
                peers.observeForever(observer) // Comenzamos a observar la lista de dispositivos
                setOnDismissListener { peers.removeObserver(observer) } // dejamos de observar la lista de dispositivos
                // Cuando se selecciona un dispositivo de la lista, llamamos al callback con el dispositivo seleccionado
                listView.setOnItemClickListener { _, _, i, _ ->
                    callback(dialogAdapter.items[i])
                    dismiss()
                }
            }
            show() // Mostramos el diálogo
        }
    }

    /**
     * Establece una conexión Wifi Direct con otro dispositivo utilizando la
     * configuración proporcionada como parámetro.
     * @param config Configuración de la conexión.
     */
    @SuppressLint("MissingPermission")
    fun connect(config: WifiP2pConfig) {
        // Se llama al método connect del WifiP2pManager para establecer la conexión.
        mManager.connect(mChannel, config, object : WifiP2pManager.ActionListener {
            // Si la conexión se estableció correctamente, se muestra un mensaje de éxito.
            override fun onSuccess() {
                Toas(ctx, "Conectado")
            }

            // Si la conexión falló, se muestra un mensaje de error.
            override fun onFailure(error: Int) {
                sending = false
                Toas(ctx, "No se pudo conectar, Error: $error")
            }

        })
    }


    // Se define una instancia de ConnectionInfoListener para manejar la conexión con otro dispositivo
    val connectionInfoListener: WifiP2pManager.ConnectionInfoListener =
        WifiP2pManager.ConnectionInfoListener {
            // Se obtiene la dirección del propietario del grupo
            val groupOwnerAddress: InetAddress = it.groupOwnerAddress
            // Se verifica si el dispositivo actual es el host del grupo
            isHost = it.groupFormed && it.isGroupOwner
            // Se utiliza un bloque when para manejar el comportamiento del dispositivo dependiendo de si es el host o no
            this.socket?.close()
            this.serverSocket?.close()
            when {
                // Si es el host, se inicia una instancia de ServerClass para esperar conexiones entrantes
                isHost!! -> {
                    Logger.i("Este dispositivo sera el HOST: $sending")
                    val intent = Intent(ctx, TransferService::class.java)
                    intent.putExtra("HostAdress", "")
                    startTheService(intent)

//                    serverClass = ServerClass { socket, serverSocket ->
//                        Logger.i("Obtenemos el socket: $sending")
//                        this.serverSocket = serverSocket; this.socket = socket
//                        sendReceive = SendReceive(this, this.socket!!)
//                        sendReceive!!.start()
//                        Logger.i("sendReceive iniciado: $sending")
//                        sendFiles()
//                    }
//                    serverClass.start()
                }
                // Si no es el host, se inicia una instancia de ClientClass para conectarse al host
                else -> {
                    Logger.i("Este dispositivo sera el CLIENT: $sending")
                    val intent = Intent(ctx, TransferService::class.java)
                    intent.putExtra("HostAdress", groupOwnerAddress.hostAddress)
                    startTheService(intent)


//                    clientClass = ClientClass(groupOwnerAddress) { socket: Socket ->
//                        this.socket = socket
//                        Logger.i("Obtenemos el socket: $sending")
//                        sendReceive = SendReceive(this, this.socket!!)
//                        sendReceive!!.start()
//                        Logger.i("sendReceive iniciado: $sending")
//                        sendFiles()
//                    }
//                    clientClass.start()
                }
            }
        }

    private fun startTheService(intent: Intent) {
        val pkg: String = ctx.packageName
        val intentService = Intent()
        intentService.component = ComponentName(
            pkg, "$pkg.utils.services.TransferService"
        )
        val bind = ctx.bindService(intentService, this, 0)
        if (!bind) {
            ctx.bindService(intent, this, 0)
        } else {
            ctx.startService(intent)
        }
    }


    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Logger.i("SERVICIO $name CONECTADO")
        val binder = service as TransferService.LocalBinder
        transferService = binder.service
        isBound = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.i("SERVICIO $name DESCONECTADO")
    }

}