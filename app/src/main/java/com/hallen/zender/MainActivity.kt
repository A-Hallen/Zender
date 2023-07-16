package com.hallen.zender

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.hallen.zender.databinding.ActivityMainBinding
import com.hallen.zender.model.interfaces.OnProgressUpdateListener
import com.hallen.zender.ui.adapters.PagerAdapter
import com.hallen.zender.utils.Permissions
import com.hallen.zender.utils.Toas
import com.hallen.zender.utils.WifiClass
import com.hallen.zender.utils.receivers.ProgressRecevier
import com.hallen.zender.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnProgressUpdateListener {
    private lateinit var binding: ActivityMainBinding
    private val appsViewModels: AppViewModel by viewModels()
    val files: ArrayList<File> = arrayListOf()
    private val CHANNEL_ID_1 = "CHANNEL_1"
    private val progressRecevier: ProgressRecevier = ProgressRecevier()
    val wifiClass: WifiClass by lazy { WifiClass(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appsViewModels.getAllData(this)
        loadPager()
        wifiClass.setProgressBar(binding.progressBar)
        wifiClass.disconnectGroup {}
        Permissions(this).askStoragePermissions()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel1 = NotificationChannel(
                CHANNEL_ID_1, "ShareFiles", NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel1.description = "Sending and receiving files"
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel1)
        }
    }

    /**
     * Enciende el Wifi, y el GPS si no está habilitado, muestra un diálogo para
     * permitir al usuario habilitarlo.
     */
    fun turnOnWifi() {
        // Obtiene los servicios de localización y Wifi
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Verifica si el proveedor de localización GPS está habilitado
        // Si no está habilitado, se muestra un diálogo para habilitar la localización
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Se crea un intent para abrir la configuración de la localización
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            // Se lanza el intent para mostrar el diálogo
            startActivity(intent)
        }
        Permissions(this).turnOnWifi(wifiManager)
    }

    override fun onResume() {
        super.onResume(); wifiClass.registerReceiver()
        val filter = IntentFilter()
        filter.addAction("PROGRESS_BROADCAST")
        filter.addAction("VISIBILITY_BROADCAST")
        registerReceiver(progressRecevier, filter)
    }

    override fun onPause() {
        super.onPause(); wifiClass.unregisterReceiver()
        unregisterReceiver(progressRecevier)
    }

    private fun loadPager() {
        val adapter = PagerAdapter(supportFragmentManager, lifecycle)
        binding.pager.offscreenPageLimit = 4
        binding.pager.adapter = adapter

        //Crea una lista de los fragmentos
        val fragmentsList = listOf(
            "Archivos",
            "Apps",
            "Galería",
            "Videos",
            "Musica",
        )
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.setCustomView(R.layout.tab_custom_view)
            tab.text = fragmentsList[position] //Establece el texto para el tab
        }.attach()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toas(this, "Los permisos de ubicación son necesarios")
                }
            }

            2 -> {
                Permissions(this).askStoragePermissions()
            }
        }
    }

    override fun onProgressUpdate(progress: Int) {
        binding.progressBar.progress = progress
    }

    override fun onSetProgressBarVisibility(visibility: Boolean) {
        binding.progressBar.visibility = if (visibility) View.VISIBLE else View.GONE
    }

}