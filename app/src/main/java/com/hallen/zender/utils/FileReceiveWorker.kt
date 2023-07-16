package com.hallen.zender.utils

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.orhanobut.logger.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import kotlin.math.roundToInt

@Suppress("BlockingMethodInNonBlockingContext")
class FileReceiveWorkerClass(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {
    private var downloads =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    override suspend fun doWork(): Result {

        val socket = SocketInstance.getSocket() ?: return Result.failure()

        Logger.i("socket created, isConnected: ${socket.isConnected}")
        val inputStream = ObjectInputStream(socket.getInputStream())
        Logger.i("inputStream created too.")

        receiveFiles(inputStream)

        return Result.success()
    }

    private fun receiveFiles(inputStream: ObjectInputStream) {
        while (true) {
            try {
                val fileNameBytes = inputStream.readObject() as ByteArray
                val fileLength = inputStream.readLong()
                val fileName = String(fileNameBytes, Charsets.UTF_8)
                Logger.i("FILENAME: $fileName, FILESIZE: $fileLength")
                val newFile = File(downloads, fileName)
                val buffer = ByteArray(4096)
                val fileOutputStream = FileOutputStream(newFile)
                var bytesRead: Int
                var fileSize = 0
                while (inputStream.read(buffer).also { bytesRead = it } > 0) {
                    fileSize += bytesRead
                    fileOutputStream.write(buffer, 0, bytesRead) // Guardar el archivo en fragmentos
                    val d: Double = (fileSize * 100).toDouble() / fileLength
                    val progressBarStatus = d.roundToInt()
                    Logger.i("Progress: $progressBarStatus, d: $d, fileSize: $fileSize, fileLength: $fileLength, bytes")
                    setProgressAsync(progressData(progressBarStatus))
                }

                fileOutputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun progressData(currentProgress: Int): Data {
        return Data.Builder()
            .putInt("progress", currentProgress)
            .build()
    }
}