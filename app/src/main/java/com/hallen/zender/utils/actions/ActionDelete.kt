package com.hallen.zender.utils.actions

import android.content.Context
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.hallen.zender.R
import com.hallen.zender.databinding.YesOrNoDialogBinding
import com.hallen.zender.utils.ZDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ActionDelete(private val context: Context) {

    fun deleteFiles(files: List<File>, callback: () -> Unit) {
        val dialog = ZDialog(context)
        val binding = YesOrNoDialogBinding.inflate(dialog.layoutInflater)
        val text = if (files.size > 1) {
            context.getString(R.string.delete_sure_plural)
        } else {
            getText(context, files.first().name)
        }
        dialog.apply {
            setContentView(binding.root)
            with(binding) {
                dialogTextWarning.text = text
                cancelButton.setOnClickListener { dismiss() }
                okButton.setOnClickListener {
                    deleteSelectedFileList(files, callback)
                    dismiss()
                }
            }
            show()
        }
    }

    private fun deleteSelectedFileList(files: List<File>, callback: () -> Unit) {
        files.forEach {
            if (it.exists() && it.canWrite()) {
                try {
                    if (it.isDirectory) it.deleteRecursively() else it.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        callback()
    }

    fun deleteFile(checks: List<Pair<String, Uri?>>, callback: () -> Unit) {
        val dialog = ZDialog(context)
        val binding = YesOrNoDialogBinding.inflate(dialog.layoutInflater)
        val text = if (checks.size > 1) {
            context.getString(R.string.delete_sure_plural)
        } else {
            val filePath = checks[0]
            val file = File(filePath.first)
            getText(context, file.name)
        }
        dialog.apply {
            setContentView(binding.root) // Establecemos el layout del diálogo
            with(binding) {
                dialogTextWarning.text = text
                cancelButton.setOnClickListener { dismiss() }
                okButton.setOnClickListener {
                    deleteSelectedFiles(checks, callback)
                    dismiss()
                }
            }
            show() // Mostramos el diálogo
        }
    }

    private fun getText(context: Context, name: String): SpannableString {
        val texto1 = context.getString(R.string.delete_sure)
        val spannable = SpannableString("$texto1 $name?")
        val colorTexto1 = ContextCompat.getColor(context, R.color.black)
        val colorTexto2 = ContextCompat.getColor(context, R.color.colorPrimary)
        spannable.setSpan(
            ForegroundColorSpan(colorTexto1),
            0,
            texto1.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(colorTexto2),
            texto1.length + 1,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(colorTexto1),
            texto1.length + name.length + 1,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    private fun error(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteSelectedFiles(checks: List<Pair<String, Uri?>>, callback: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            for (path in checks) {
                val uri = path.second ?: continue
                val file = File(path.first)
                if (file.exists() && file.canWrite()) {
                    try {
                        val deleted = file.delete()
                        if (deleted) {
                            context.contentResolver.delete(uri, null, null)
                        } else error("Error al eliminar el archivo")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    error("El archivo no existe")
                    context.contentResolver.delete(uri, null, null)
                }
            }
        }.invokeOnCompletion {
            CoroutineScope(Dispatchers.Main).launch {
                callback()
            }
        }
    }
}