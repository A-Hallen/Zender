package com.hallen.zender.utils

import android.os.Environment
import android.os.Handler
import com.orhanobut.logger.Logger
import java.io.*
import java.net.Socket

class SendReceive(private val socket: Socket, private val handler: Handler) : Thread() {
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var downloads =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    init {
        try {
            inputStream = socket.getInputStream()
            outputStream = DataOutputStream(socket.getOutputStream())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun run() {
        val buffer = ByteArray(4096)
        val tempFile = File("${downloads.absolutePath}/tempFile")
        if (!tempFile.exists()) tempFile.createNewFile()
        if (tempFile.exists()) {
            FileOutputStream(tempFile).use { fileOutputStream ->
                var read: Int
                while (inputStream!!.read(buffer).also { read = it } > 0) {
                    fileOutputStream.write(buffer, 0, read)
                }
                fileOutputStream.flush()
            }
            Logger.i("Archivo enviado correctamente")
        }

    }

    fun write(file: File) {
        FileInputStream(file).use { fileInputStream ->
            val buffer = ByteArray(4096)
            var read: Int
            while (fileInputStream.read(buffer).also { read = it } > 0) {
                outputStream?.write(buffer, 0, read)
                Logger.i("SENDING...")
            }
            outputStream?.flush()
        }
    }
}