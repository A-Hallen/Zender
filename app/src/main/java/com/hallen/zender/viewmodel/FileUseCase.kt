package com.hallen.zender.viewmodel

import java.io.File
import java.util.*
import javax.inject.Inject

class FileUseCase @Inject constructor() {

    fun getFilesFromFolder(folderPath: String): ArrayList<File>? {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.canRead()) return null

        val files: ArrayList<File> = arrayListOf()

        val archivos = folder.listFiles() ?: return null
        Arrays.sort(archivos) { f1, f2 ->
            when {
                f1.isDirectory && !f2.isDirectory -> -1
                !f1.isDirectory && f2.isDirectory -> 1
                else -> f1.name.lowercase().compareTo(f2.name.lowercase())
            }
        }
        return files.apply { addAll(archivos) }
    }


}
