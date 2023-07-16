package com.hallen.zender.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.hallen.zender.R
import com.hallen.zender.databinding.DialogDeviceItemBinding
import com.hallen.zender.databinding.DialogSelectDeviceBinding
import java.io.File
import java.util.*

class ActionView {
    private lateinit var activities: List<ResolveInfo>
    private val launchables: MutableLiveData<List<ResolveInfo>> = MutableLiveData()

    fun showImage(file: File, context: Context) {
        val uri = FileProvider.getUriForFile(
            context.applicationContext,
            "${context.applicationContext.packageName}.provider",
            file
        ).normalizeScheme()
        showImage(uri, context)
    }

    fun showItem(file: File, context: Context, mime: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context.applicationContext,
            "${context.applicationContext.packageName}.provider", file
        ).normalizeScheme()
        showFile(uri, context, mime)
    }

    fun showVideo(uri: Uri, context: Context) {
        showFile(uri, context, "video/*")
    }

    fun showAudio(uri: Uri, context: Context) {
        showFile(uri, context, "audio/*")
    }

    fun showImage(uri: Uri, context: Context) {
        showFile(uri, context, "image/*")
    }

    private fun showFile(uri: Uri, context: Context, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        if (mimeType.isNotBlank()) {
            intent.setDataAndType(uri, mimeType)
        } else intent.data = uri
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        context.startActivity(intent)
    }

    fun showImageAppChooser(file: File, context: Context) {
        val uri = FileProvider.getUriForFile(
            context.applicationContext,
            "${context.applicationContext.packageName}.provider",
            file
        ).normalizeScheme()

        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setDataAndType(uri, "image/*")
        val packageManager = context.packageManager
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        activities =
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_SYSTEM_ONLY)
        Collections.sort(activities, ResolveInfo.DisplayNameComparator(packageManager))
        launchables.value = activities
        showDialogChooser(context, packageManager) { resolveInfo ->

        }
    }

    private fun showDialogChooser(
        context: Context, packageManager: PackageManager, callback: (ResolveInfo) -> Unit
    ) {
        val dialog = ZDialog(context) // Creamos el diálogo
        val dialogAdapter = AppChooserAdapter(
            context,
            packageManager
        ) // Creamos el adaptador para la lista de dispositivos

        val binding = DialogSelectDeviceBinding.inflate(dialog.layoutInflater)
        dialog.apply {
            setContentView(binding.root) // Establecemos el layout del diálogo
            with(binding) {
                listView.adapter = dialogAdapter // Establecemos el adaptador a la lista
                // Creamos un observer que se llamará cada vez que cambie la lista de dispositivos
                val observer = androidx.lifecycle.Observer<List<ResolveInfo>> {
                    if (it.isEmpty()) {
                        // Si la lista está vacía, mostramos un mensaje indicándolo
                        textView.visibility = View.VISIBLE
                        textView.text = "No se encontró ningún dispositivo"
                    } else {
                        // Si hay dispositivos, ocultamos el mensaje
                        textView.visibility = View.GONE
                    }
                    dialogAdapter.newItems(it) //Actualizamos el adaptador
                }
                launchables.observeForever(observer) // Comenzamos a observar la lista de dispositivos
                setOnDismissListener { launchables.removeObserver(observer) } // dejamos de observar la lista de dispositivos
                // Cuando se selecciona un dispositivo de la lista, llamamos al callback con el dispositivo seleccionado
                listView.setOnItemClickListener { _, _, i, _ ->
                    callback(dialogAdapter.items[i])
                    dismiss()
                }
            }
            show() // Mostramos el diálogo
        }

    }


}

class AppChooserAdapter(
    private val context: Context,
    private val packageManager: PackageManager,
    var items: ArrayList<ResolveInfo> = arrayListOf()
) : BaseAdapter() {

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding = DialogDeviceItemBinding.inflate(
            LayoutInflater.from(parent?.context), null, false
        )

        Glide.with(binding.icon.context).load(R.drawable.ic_wifi).into(binding.icon)
        val item = items[position]
        val name = item.activityInfo.loadLabel(packageManager) as String
        val icon = item.loadIcon(packageManager)
        binding.text.text = name
        Glide.with(context).load(icon)
            .error(android.R.drawable.sym_def_app_icon)
            .into(binding.icon)
        return binding.root
    }

    fun newItems(it: List<ResolveInfo>) {
        items.clear(); items.addAll(it)
        notifyDataSetChanged()
    }
}