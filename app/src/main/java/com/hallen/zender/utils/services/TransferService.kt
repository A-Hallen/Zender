package com.hallen.zender.utils.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hallen.zender.R
import com.orhanobut.logger.Logger
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore


class TransferService : Service() {
    private var socket: Socket? = null
    private val serviceBinder: IBinder = LocalBinder()
    private val CHANNEL_ID_1 = "CHANNEL_1"
    private var outputStream: ObjectOutputStream? = null
    private val downloads =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    private val queue: Queue<File> = LinkedList()
    private var isSending = false


    override fun onBind(intent: Intent?): IBinder {
        return serviceBinder
    }

    inner class LocalBinder : Binder() {
        val service: TransferService
            get() = this@TransferService
    }

    override fun onDestroy() {
        socket?.close()
        Logger.i("SOCKET CLOSED")
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        socket?.close()
        Logger.i("SOCKET CLOSED")
        stopSelf()
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Iniciar el servicio como un servicio en primer plano

        val hostAdress = intent?.getStringExtra("HostAdress")
        if (hostAdress.isNullOrBlank()) startServer() else startClient(hostAdress)

        val notification = showNotification()
        startForeground(1, notification)
        return START_STICKY
    }

    private fun showNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID_1)
            .setContentTitle("Servicio de transferencia en segundo plano")
            .setSmallIcon(R.drawable.ic_send)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun startServer() {
        object : Thread() {
            override fun run() {
                val serverSocket = ServerSocket(8888)
                socket = serverSocket.accept()
                outputStream = ObjectOutputStream(socket?.getOutputStream())
                receiveFiles()
            }
        }.start()
    }

    private fun startClient(hostAdress: String) {
        Logger.i("START CLIENT CALLED: $hostAdress")
        object : Thread() {
            override fun run() {
                socket = Socket()
                socket!!.connect(InetSocketAddress(hostAdress, 8888), 1500)
                outputStream = ObjectOutputStream(socket?.getOutputStream())
                receiveFiles()
            }
        }.start()
    }

    private fun sendProgressVisibility(visibility: Boolean) {
        val intent = Intent("VISIBILITY_BROADCAST")
        intent.putExtra("visibility", visibility)
        sendBroadcast(intent)
    }

    private fun sendProgressUpdate(progress: Int) {
        val intent = Intent("PROGRESS_BROADCAST")
        intent.putExtra("progress", progress)
        sendBroadcast(intent)
    }


    private fun receiveFiles() {
        Logger.i("RECEIVING FILES STARTED")
        val inputStream = ObjectInputStream(socket!!.getInputStream())
        while (true) {
            try {
                val fileNameBytes = inputStream.readObject() as ByteArray
                sendProgressVisibility(true)
                val fileLength = inputStream.readLong()
                val fileName = String(fileNameBytes, Charsets.UTF_8)
                Logger.i("FILENAME: $fileName, FILE LENGTH: $fileLength")
                val newFile = File(downloads, fileName)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var fileSize = 0F
                FileOutputStream(newFile).use { fileOutputStream ->
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        fileSize += bytesRead
                        fileOutputStream.write(buffer, 0, bytesRead)
                        val progress: Int = ((fileSize * 100) / fileLength).toInt()
                        // Enviar el progreso
                        sendProgressUpdate(progress)
                    }
                }
                Logger.i("CLOSED ALL: $fileName RECEIVED")
                sendProgressVisibility(false)
            } catch (e: Exception) {
                inputStream.close()
                socket?.close()
                stopSelf()
                e.printStackTrace()
            } finally {
                sendProgressVisibility(false)
            }
        }
    }

    private val executorService = Executors.newSingleThreadExecutor()
    private val semaphore = Semaphore(1)
    fun sendFiles(files: ArrayList<File>) {
        Logger.i("SENDING FILES STARTED WITH FILES: ${files.size} files, QUEUE SIZE: ${queue.size}, IS SENDING: $isSending, QUEUE: $queue")
        if (queue.isEmpty() && !isSending) {
            queue.addAll(files)
            executorService.submit {
                isSending = true
                while (queue.isNotEmpty()) {
                    semaphore.acquire()
                    val file = queue.poll()!!
                    sendFile(file)
                    semaphore.release()
                }
                isSending = false
            }
        } else queue.addAll(files)
    }

    private fun sendFile(file: File) {
        Logger.i("socket created, isConnected: ${socket?.isClosed}")
        if (outputStream == null) {
            Logger.i("outputStream is null")
            return
        }

        val fileName = file.name // Obtiene el nombre del archivo.
        val fileLength = file.length()
        sendProgressVisibility(true)
        Logger.i("FILENAME: $fileName, FILESIZE: $fileLength")
        outputStream!!.writeObject(fileName.toByteArray(Charsets.UTF_8)) // Escribe el nombre del archivo en el outputStream.
        outputStream!!.writeLong(fileLength)
        var fileSize = 0F
        val buf = ByteArray(4096)
        val fileInputStream = FileInputStream(file)
        val bufferedInputStream = BufferedInputStream(fileInputStream)
        var len: Int
        try {
            while (bufferedInputStream.read(buf).also { len = it } != -1) {
                fileSize += len
                outputStream!!.write(buf, 0, len)
                outputStream!!.flush()
                //count for progress bar
                val progress: Int = ((fileSize * 100) / fileLength).toInt()

                // Enviar el progreso
                sendProgressUpdate(progress)
            }
            // remove the file from the queue
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            sendProgressVisibility(false)
            Logger.i("Enviado todo: $file")
            bufferedInputStream.close()
            fileInputStream.close()
        }
    }


}