package com.hallen.zender.ui.adapters

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hallen.zender.databinding.AppItemBinding
import java.io.File

class AppAdapter :
    RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    val apps: ArrayList<ApplicationInfo> = arrayListOf()
    val checkeds: MutableLiveData<ArrayList<String>> =
        MutableLiveData<ArrayList<String>>().apply { value = arrayListOf() }
    val checks: ArrayList<String> = arrayListOf()
    private var checkAll = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = AppItemBinding.inflate(LayoutInflater.from(parent.context))
        val lp = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.root.layoutParams = lp
        return AppViewHolder(binding)
    }

    // Override the onBindViewHolder method to bind the app data to the PhotoViewHolder
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = apps[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = apps.size

    fun formatBytes(size: Long): String {
        val units = arrayOf("b", "Kb", "Mb", "Gb")
        var bytes = size
        var index = 0
        while (bytes > 1024 && index < units.size - 1) {
            bytes /= 1024
            index++
        }
        return "$bytes ${units[index]}"
    }

    @SuppressLint("NotifyDataSetChanged")
    fun checkAll(value: Boolean) {
        checkAll = value
        checks.clear()
        if (checkAll) {
            apps.map { checks.add(it.packageName) }
        }
        checkeds.value = checks
        notifyDataSetChanged()
    }

    // Define an inner class called PhotoViewHolder that takes in a binding object
    inner class AppViewHolder(private val binding: AppItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Define a function called bind that sets the app data to the views in the layout
        fun bind(app: ApplicationInfo) {
            val packageManager = binding.root.context.packageManager
            val appIconUri: Uri? =
                Uri.parse("android.resource://" + app.packageName + "/" + app.icon)
            Glide.with(binding.root.context).load(appIconUri)
                .error(android.R.drawable.sym_def_app_icon)
                .into(binding.icon)
            with(binding) {
                if (app.packageName in checks) {
                    if (!checkbox.isChecked) checkbox.toggle()
                } else if (checkbox.isChecked) checkbox.toggle()
                text1.text = packageManager.getApplicationLabel(app)
                text2.text = formatBytes(File(app.publicSourceDir).length())
                parent.setOnClickListener {
                    checkbox.toggle()
                    if (checkbox.isChecked) {
                        checks.add(app.packageName)
                    } else checks.remove(app.packageName)
                    checkeds.value = checks
                }
            }
        }
    }
}