package com.hallen.zender.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.orhanobut.logger.Logger
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.ObjectOutputStream
import kotlin.math.roundToInt

@Suppress("BlockingMethodInNonBlockingContext")
class FileSendWorkerclass(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    object FileParams {
        const val KEY_FILES = "key_file_source"
    }

    override suspend fun doWork(): Result {

        val filePaths: Array<String>? = inputData.getStringArray(FileParams.KEY_FILES)

        Logger.i("filePaths: ${filePaths?.get(0)}")

        val socket = SocketInstance.getSocket() ?: return Result.failure()

        Logger.i("socket created, isConnected: ${socket.isConnected}")

        val files = filePaths?.map { File(it) } ?: return Result.failure()

        val outputStream = ObjectOutputStream(socket.getOutputStream())
        Logger.i("outputStream created too.")

        for (file in files) sendFile(file, outputStream)


        return Result.success()
    }

    private fun sendFile(file: File, outputStream: ObjectOutputStream) {
        val fileName = file.name // Obtiene el nombre del archivo.
        val fileLength = file.length()
        Logger.i("FILENAME: $fileName, FILESIZE: $fileLength")
        outputStream.writeObject(fileName.toByteArray(Charsets.UTF_8)) // Escribe el nombre del archivo en el outputStream.
        outputStream.writeLong(fileLength)
        var progressBarStatus: Int
        var fileSize = 0
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
                val d: Double = (fileSize * 100).toDouble() / fileLength
                progressBarStatus = d.roundToInt()
                val log =
                    "Progress: $progressBarStatus, d: $d, fileSize: $fileSize, len: $len, filelength: $fileLength, name: ${file.name}"
                setProgressAsync(progressData(progressBarStatus, log))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            setProgressAsync(progressData(100, e.message.toString()))
        }
        setProgressAsync(progressData(100, "Ya je envio todo"))
        outputStream.close()
        fileInputStream.close()
        bufferedInputStream.close()
    }

    private fun progressData(currentProgress: Int, log: String): Data {
        return Data.Builder()
            .putString("log", log)
            .putInt("progress", currentProgress)
            .build()
    }
/*
    private fun copyFile(source: File, destination: File, move: Boolean = false) {
        val bufferedInputStream =
        val bufferedOutputStream =
        val size = source.length()
        var total = 0F
        try {
            val buf = ByteArray(2048)
            var nosRead: Int
            var pers: Int
            var longPers = 0

            while (bufferedInputStream.read(buf).also { nosRead = it } != -1) {
                total += nosRead
                pers = ((total * 100) / size).toInt()
                bufferedOutputStream.write(buf, 0, nosRead)

                if (pers != longPers) {
                    longPers = pers
                    builder.setProgress(100, pers, false)
                }
            }
            if (move) {
                source.delete()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
        }
    }
 */
}