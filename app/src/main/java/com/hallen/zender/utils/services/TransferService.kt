package com.hallen.zender.utils.services

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hallen.zender.R
import com.orhanobut.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
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
    private val executorService = Executors.newSingleThreadExecutor()
    private val semaphore = Semaphore(1)
    private var deviceConnected: String? = null
    private lateinit var notificationManager: NotificationManager

    private var notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID_1)
        .setSmallIcon(R.drawable.ic_send)
        .setContentText("Servicio de transferencia en segundo plano")
        .setOngoing(true)
        .setProgress(100, 0, true)

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

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
        deviceConnected = intent?.getStringExtra("DeviceConnected")
        if (hostAdress.isNullOrBlank()) startServer() else startClient(hostAdress)

        showNotification()
        return START_STICKY
    }

    private fun showNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_1)
            .setContentTitle("Servicio de transferencia en segundo plano")
            .setSmallIcon(R.drawable.ic_send)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .build()
        startForeground(1, notification)
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
        Thread {
            socket = Socket()
            socket!!.connect(InetSocketAddress(hostAdress, 8888), 1500)
            outputStream = ObjectOutputStream(socket?.getOutputStream())
            receiveFiles()
        }.start()
    }

    private fun sendProgressVisibility(visibility: Boolean) {
        val intent = Intent("VISIBILITY_BROADCAST")
        intent.putExtra("visibility", visibility)
        sendBroadcast(intent)
    }

    private fun sendFileTransferred(file: File, send: Boolean) {
        val intent = Intent("FILE_TRANSFERRED")
        intent.putExtra("FILE_PATH", file.absolutePath)
        intent.putExtra("TARGET", deviceConnected)
        intent.putExtra("SEND", send)
        sendBroadcast(intent)
        showNotification()
    }

    private var progressJob: Job? = null
    private fun sendProgressUpdate(progress: Int) {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Default).launch {
            val intent = Intent("PROGRESS_BROADCAST")
            intent.putExtra("progress", progress)
            sendBroadcast(intent)
            notificationBuilder.setProgress(100, progress, false)
            notificationManager.notify(1, notificationBuilder.build())
        }
    }

    private var fCounter = 0
    private fun getFile(name: String): File {
        File(downloads, name).also {
            return if (it.exists()) {
                val newName = it.nameWithoutExtension + fCounter + "." + it.extension
                getFile(newName)
            } else it
        }
    }

    private fun receiveFileData(input: ObjectInputStream): Pair<Long, File> {
        val fileNameBytes = input.readObject() as ByteArray
        sendProgressVisibility(true)
        val fileLength = input.readLong()
        val fileName = String(fileNameBytes, Charsets.UTF_8)
        notificationBuilder.setContentTitle(fileName)
        val newFile = getFile(fileName)
        return Pair(fileLength, newFile)
    }

    private fun receiveFile(inputStream: ObjectInputStream) {
        val (fileLength, newFile) = receiveFileData(inputStream)
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
        sendProgressVisibility(false)
        sendFileTransferred(newFile, false)
    }

    private fun receiveFiles() {
        val inputStream = ObjectInputStream(socket!!.getInputStream())
        while (true) {
            try {
                receiveFile(inputStream)
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

    private fun sendFile(file: File) {
        Logger.i("socket created, isConnected: ${socket?.isClosed}")
        if (outputStream == null) {
            Logger.i("outputStream is null")
            return
        }

        val fileName = file.name // Obtiene el nombre del archivo.
        notificationBuilder.setContentTitle(fileName)
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
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            sendProgressVisibility(false)
            sendFileTransferred(file, true)
            bufferedInputStream.close()
            fileInputStream.close()
        }
    }

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

    private fun sendFile2(file: File) {
        val fileName = file.name
        val fileLength = file.length()
        outputStream!!.writeUTF(fileName)
        outputStream!!.writeLong(fileLength)
        sendProgressVisibility(true)
        val bufferSize = 4096
        val buffer = ByteArray(bufferSize)
        val fileInputStream = FileInputStream(file)
        val bufferedInputStream = BufferedInputStream(fileInputStream, bufferSize)
        val bufferedOutputStream = BufferedOutputStream(outputStream!!, bufferSize)
        var totalSent = 0F
        var count: Int

        try {
            while (bufferedInputStream.read(buffer).also { count = it } != -1) {
                bufferedOutputStream.write(buffer, 0, count)
                bufferedOutputStream.flush()
                totalSent += count
                val progress: Int = ((totalSent * 100) / fileLength).toInt()
                sendProgressUpdate(progress)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            sendProgressVisibility(false)
            sendFileTransferred(file, true)
        }
    }

    private fun receiveFiles2() {
        val inputStream = ObjectInputStream(socket!!.getInputStream())
        while (socket?.isConnected == true) {
            try {
                val fileName = inputStream.readUTF()
                val fileLength = inputStream.readLong()
                sendProgressVisibility(true)
                val newFile = File(downloads, fileName)
                val bufferSize = 4096
                val buffer = ByteArray(bufferSize)
                val fileOutputStream = FileOutputStream(newFile)
                val bufferedInputStream = BufferedInputStream(inputStream, bufferSize)
                val bufferedOutputStream = BufferedOutputStream(fileOutputStream, bufferSize)
                var totalReceived = 0F
                var count: Int
                while (bufferedInputStream.read(buffer).also { count = it } != -1) {
                    bufferedOutputStream.write(buffer, 0, count)
                    totalReceived += count
                    val progress = ((totalReceived * 100) / fileLength).toInt()
                    sendProgressUpdate(progress)
                }
                bufferedInputStream.close()
                bufferedOutputStream.flush()
                bufferedOutputStream.close()
                fileOutputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                sendProgressVisibility(false)
            }
        }
    }


}