package com.hallen.zender.utils

import android.os.Environment
import android.view.View
import com.orhanobut.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket

class SendReceive(
    private val wifiClass: WifiClass,
    private val socket: Socket
) : Thread() {
    private val outputStream = ObjectOutputStream(socket.getOutputStream())
    private val downloads =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    /**
    Esta función se encarga de recibir archivos de otro dispositivo y guardarlos en el directorio de descargas.
    @throws EOFException si la conexión se ha cerrado inesperadamente.
     */

    private fun runReceive() {
        val inputStream = ObjectInputStream(socket.getInputStream())

        while (true) {
            CoroutineScope(Dispatchers.Main).launch {
                wifiClass.getProgressBar().visibility = View.VISIBLE
            }
            try {
                val fileNameBytes = inputStream.readObject() as ByteArray
                val fileLength = inputStream.readLong()
                val fileName = String(fileNameBytes, Charsets.UTF_8)
                val newFile = File(downloads, fileName)
                val buffer = ByteArray(4096)
                val fileOutputStream = FileOutputStream(newFile)
                var bytesRead: Int
                var fileSize = 0F
                while (inputStream.read(buffer).also { bytesRead = it } > 0) {
                    fileSize += bytesRead
                    fileOutputStream.write(buffer, 0, bytesRead) // Guardar el archivo en fragmentos
                    val progress: Int = ((fileSize * 100) / fileLength).toInt()
                    CoroutineScope(Dispatchers.Main).launch {
                        wifiClass.getProgressBar().progress = progress
                    }
                }
                fileOutputStream.close()
                Logger.i("CLOSED ALL")
                CoroutineScope(Dispatchers.Main).launch {
                    wifiClass.getProgressBar().visibility = View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun run() {
//        runReceive()
    }

    private fun sendFile(file: File) {
        Logger.i("socket created, isConnected: ${socket.isClosed}")


        val fileName = file.name // Obtiene el nombre del archivo.
        val fileLength = file.length()
        CoroutineScope(Dispatchers.Main).launch {
            wifiClass.getProgressBar().visibility = View.VISIBLE
        }
        Logger.i("FILENAME: $fileName, FILESIZE: $fileLength")
        outputStream.writeObject(fileName.toByteArray(Charsets.UTF_8)) // Escribe el nombre del archivo en el outputStream.
        outputStream.writeLong(fileLength)
        var fileSize = 0F
        val buf = ByteArray(4096)
        val fileInputStream = FileInputStream(file)
        val bufferedInputStream = BufferedInputStream(fileInputStream)
        var len: Int
        try {
            while (bufferedInputStream.read(buf).also { len = it } != -1) {
                fileSize += len
                outputStream.write(buf, 0, len)
                outputStream.flush()
                //count for progress bar
                val progress: Int = ((fileSize * 100) / fileLength).toInt()
                CoroutineScope(Dispatchers.Main).launch {
                    wifiClass.getProgressBar().progress = progress
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                wifiClass.getProgressBar().visibility = View.GONE
                //wifiClass.files.remove(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Logger.i("Enviado todo: $file")
        bufferedInputStream.close()
        fileInputStream.close()
    }


    private fun send(files: ArrayList<File>) {
        for (file in files) {
            sendFile(file)
        }
    }


    fun write(files: ArrayList<File>) {
        CoroutineScope(Dispatchers.IO).launch {
            send(files)
        }
    }

}