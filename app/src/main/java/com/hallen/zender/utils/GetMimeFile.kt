package com.hallen.zender.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.webkit.MimeTypeMap
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.hallen.zender.R
import java.io.File
import java.util.Locale

class GetMimeFile(private val context: Context) {
    private var extension: String = ""
    private var mime: String = ""

    fun getmime(file: File): String {
        extension = file.extension.lowercase(Locale.getDefault())
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        this.mime = mime ?: ""
        return mime ?: ""
    }

    fun getApkIcon(file: File): Drawable {
        val path = file.absolutePath
        val pm: PackageManager = context.packageManager
        val pi: PackageInfo = pm.getPackageArchiveInfo(path, 0)
            ?: return AppCompatResources.getDrawable(context, android.R.drawable.sym_def_app_icon)!!
        //the secrets lines of code
        pi.applicationInfo.sourceDir = path
        pi.applicationInfo.publicSourceDir = path
        return pi.applicationInfo.loadIcon(pm)
    }

    fun getImageFromExtension(): Drawable {
        return when (getTipeOfExtension()) {
            "microsoftWord" -> getDrawable(R.drawable.icon_microsoft_word)
            "audio" -> getDrawable(R.drawable.icon_music)
            "text" -> getDrawable(R.drawable.ic_document)
            "html" -> ContextCompat.getDrawable(context, R.drawable.icon_html)!!
            "contactos" -> getDrawable(R.drawable.icon_contactos)
            "epub" -> getDrawable(R.drawable.icon_epub)
            "PowerPoint" -> getDrawable(R.drawable.icon_power_point)
            "pdf" -> getDrawable(R.drawable.icon_pdf)
            "excel" -> getDrawable(R.drawable.icon_excel)
            else -> getDrawable(R.drawable.ic_document)
        }
    }

    private fun getDrawable(resource: Int): Drawable {
        return ContextCompat.getDrawable(context, resource)!!
    }

    private fun getTipeOfExtension(): String {
        return when (extension) {
            "doc" -> "microsoftWord"
            "docx" -> "microsoftWord"
            "dotx" -> "microsoftWord"
            "xhtml" -> "html"
            "xml" -> "text"
            "zip" -> "zip"
            "ppt" -> "PowerPoint"
            "pptx" -> "PowerPoint"
            "potx" -> "PowerPoint"
            "pdf" -> "pdf"
            "torrent" -> "torrent"
            "epub" -> "epub"
            "html" -> "html"
            "vcf" -> "contactos"
            "xls" -> "excel"
            "xlt" -> "excel"
            "xla" -> "excel"
            "xlsx" -> "excel"
            "xltx" -> "excel"
            "xlsm" -> "excel"
            "xltm" -> "excel"
            "xlam" -> "excel"
            "xlsb" -> "excel"
            else -> mime.split("/").firstOrNull() ?: ""
        }
    }
}
