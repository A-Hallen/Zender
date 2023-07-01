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
    private val PERMISSION_REQUEST_LOCATION = 1

    fun checkLocationPermissions(): Permissions {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Los permisos ya han sido aceptados, llamamos la función
        } else {
            // Pedimos los permisos
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_REQUEST_LOCATION
            )
        }
        return this
    }

    fun turnOnWifi(wifiManager: WifiManager): Permissions {
        if (!wifiManager.isWifiEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                (context as Activity).startActivityForResult(panelIntent, 2)
            } else {
                wifiManager.isWifiEnabled = true
            }
        } else {
            checkLocationPermissions()
        }
        return this
    }

    fun askStoragePermissions() {
        Toas(context, "askStoragePermissions")
        // Verificamos si tenemos permiso para escribir en almacenamiento externo
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Si no tenemos permiso, lo solicitamos
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                3
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(Manifest.permission.ACCESS_MEDIA_LOCATION), 3
                )
            }


            val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val intent =
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                        context.startActivityForResult(intent, 3)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}