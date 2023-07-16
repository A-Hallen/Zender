package com.hallen.zender.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.hallen.zender.BuildConfig

class Permissions(private val context: Context) {
    private val PERMISSION_REQUEST = 1

    fun turnOnWifi(wifiManager: WifiManager): Permissions {
        if (!wifiManager.isWifiEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                (context as Activity).startActivityForResult(panelIntent, 2)
            } else {
                wifiManager.isWifiEnabled = true
                askStoragePermissions()
            }
        } else askStoragePermissions()
        return this
    }

    /**
     * Solicita los permisos necesarios para acceder a la ubicación del dispositivo y almacenamiento externo.
     * En caso de que el dispositivo tenga Android Q o posterior, también solicita permiso para acceder a la ubicación de los archivos multimedia.
     * Si los permisos ya han sido otorgados, la función no hace nada.
     * Si los permisos aún no han sido otorgados, se muestra una ventana emergente para que el usuario los acepte.
     * Si el dispositivo tiene Android R o posterior y el usuario no ha dado permiso para acceder a todos los archivos del dispositivo, se muestra una ventana emergente para que el usuario lo permita.
     */
    fun askStoragePermissions() {
        // Array que contiene todos los permisos necesarios
        var permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        // Si el dispositivo tiene Android Q o posterior, se agrega el permiso para acceder a la ubicación de los archivos multimedia
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions += Manifest.permission.ACCESS_MEDIA_LOCATION
        }

        // Comprueba si los permisos ya han sido otorgados
        if (permissions.all {
                ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }) {
            return
        }
        // Si los permisos aún no han sido otorgados, muestra una ventana emergente para que el usuario los acepte
        ActivityCompat.requestPermissions(context as Activity, permissions, PERMISSION_REQUEST)

        // Si el dispositivo tiene Android R o posterior y el usuario no ha dado permiso para acceder a todos los archivos del dispositivo, muestra una ventana emergente para que el usuario lo permita.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                    context.startActivityForResult(intent, 4)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}