package com.hallen.zender.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatCheckedTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.imageview.ShapeableImageView
import com.hallen.zender.R
import com.hallen.zender.databinding.HistoryItemBinding
import com.hallen.zender.model.History
import com.hallen.zender.model.interfaces.OnItemClickListener
import com.hallen.zender.ui.adapters.diffs.HistoryDiffCallback
import com.hallen.zender.utils.GetMimeFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    context: Context,
    var items: List<History> = emptyList(),
    private val checkCallback: (Boolean) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private lateinit var onItemClickListener: OnItemClickListener
    private val getMimeFile = GetMimeFile(context)
    var checkMode: Boolean = false
        set(value) {
            field = value
            checkCallback(value)
        }
    val checks: ArrayList<Long> = arrayListOf()

    fun setItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = HistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onItemClickListener)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size
    fun updateHistory(it: List<History>) {
        val historyDiffCallback = HistoryDiffCallback(items, it)
        val diffResult = DiffUtil.calculateDiff(historyDiffCallback)
        items = it
        diffResult.dispatchUpdatesTo(this@HistoryAdapter)
    }

    inner class ViewHolder(
        private val binding: HistoryItemBinding, onItemClickListener: OnItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.container.setOnClickListener {
                if (checkMode) {
                    val checkBox = binding.checkbox
                    if (checkBox.isChecked) {
                        checks.remove(items[position].id)
                    } else checks.add(items[position].id)
                    checkBox.toggle()
                } else onItemClickListener.onItemClick(position)
            }
            binding.container.setOnLongClickListener {
                binding.checkbox.visibility = View.VISIBLE
                binding.checkbox.toggle()
                checkMode = !checkMode
                checks.add(items[position].id)
                notifyDataSetChanged()
                true
            }
        }

        fun bind(item: History) {
            bind(item, binding)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bind(item: History, binding: HistoryItemBinding) {
        val file = item.file
        val glide = Glide.with(binding.root.context)
        val mime = getMimeFile.getmime(file)
        val targetText: String = if (item.send) "Enviado a: " else "Recivido de: "

        with(binding) {
            name.text = file.name
            size.text = formatSize(file.length())
            date.text = dateFormat.format(item.date)
            target.text = targetText + item.target
            setupCheckBox(checkbox, item.id)
            val bgColor = if (!file.exists()) {
                glide.load(R.drawable.ic_document).into(icon)
                ContextCompat.getColor(root.context, R.color.softred)
            } else {
                setIcon(mime, icon, item.file, glide)
                Color.WHITE
            }
            container.backgroundTintList = ColorStateList.valueOf(bgColor)
        }
    }

    private fun setupCheckBox(checkbox: AppCompatCheckedTextView, id: Long) {
        if (checkMode) {
            checkbox.visibility = View.VISIBLE
            if (id in checks) {
                if (!checkbox.isChecked) checkbox.toggle()
            } else if (checkbox.isChecked) checkbox.toggle()
        } else checkbox.visibility = View.INVISIBLE
    }

    private fun setApkIcon(file: File, icon: ShapeableImageView, glide: RequestManager) {
        CoroutineScope(Dispatchers.IO).launch {
            val drawable = getMimeFile.getApkIcon(file)
            CoroutineScope(Dispatchers.Main).launch {
                glide.load(drawable).into(icon)
            }
        }
    }

    private fun setIcon(
        mime: String,
        icon: ShapeableImageView,
        file: File,
        glide: RequestManager
    ) {
        val mimeSplit = mime.split("/")[0]

        when {
            mimeSplit == "image" || mimeSplit == "video" -> {
                glide.load(file).transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.color.black)
                    .error(android.R.color.black).into(icon)
            }

            mime == "application/vnd.android.package-archive" -> {
                setApkIcon(file, icon, glide)
            }

            else -> {
                val image = getMimeFile.getImageFromExtension()
                glide.load(image).error(R.drawable.ic_document).into(icon)
            }
        }
    }

    private fun formatSize(size: Long): String {
        return when (size) {
            in 0L..1000L -> "$size Bytes"
            in 1000L..1000000L -> String.format("%.2f", (size / 1000F)) + "Kb"
            in 1000000L..1000000000L -> {
                String.format("%.2f", (size / 1000000F)) + "Mb"
            }

            in 1000000000L..1000000000000L -> {
                String.format("%.2f", (size / 1000000000F)) + "Gb"
            }

            else -> "0 Bytes"
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun checkAll(value: Boolean) {
        checks.clear()
        checks.addAll(
            if (value) {
                ArrayList(items.map { it.id })
            } else arrayListOf()
        )
        checkMode = value
        notifyDataSetChanged()
    }
}