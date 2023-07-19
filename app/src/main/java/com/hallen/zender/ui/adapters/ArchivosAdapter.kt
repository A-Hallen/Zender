package com.hallen.zender.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hallen.zender.R
import com.hallen.zender.databinding.ArchivosItemBinding
import com.hallen.zender.model.interfaces.OnItemClickListener
import com.hallen.zender.ui.adapters.diffs.ArchivosDiffCallback
import com.hallen.zender.utils.GetMimeFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ArchivosAdapter(private val context: Context, private val checkCallback: (Boolean) -> Unit) :
    RecyclerView.Adapter<ArchivosAdapter.ArchivosViewHolder>() {
    private lateinit var onItemClickListener: OnItemClickListener
    private val getMimeFile = GetMimeFile(context)
    var items: List<File> = emptyList()
    var checkMode: Boolean = false
        set(value) {
            field = value
            checkCallback(value)
        }
    val checks: MutableLiveData<ArrayList<String>> = MutableLiveData(arrayListOf())

    private fun ArrayList<String>?.addValue(item: String) {
        this?.add(item)
        checks.value = this
    }

    private fun ArrayList<String>?.removeValue(item: String) {
        this?.remove(item)
        checks.value = this
    }

    fun setItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchivosViewHolder {
        val binding =
            ArchivosItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArchivosViewHolder(binding, onItemClickListener)
    }

    override fun onBindViewHolder(holder: ArchivosViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun update(it: List<File>?) {
        val fileDiffCallback = ArchivosDiffCallback(items, it ?: emptyList())
        val diffResult = DiffUtil.calculateDiff(fileDiffCallback)
        items = it ?: emptyList()
        diffResult.dispatchUpdatesTo(this@ArchivosAdapter)
        checks.value = arrayListOf()
    }

    inner class ArchivosViewHolder(
        private val binding: ArchivosItemBinding,
        onItemClickListener: OnItemClickListener
    ) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (checkMode) {
                    val checkBox = binding.checkbox
                    if (checkBox.isChecked) {
                        checks.value?.removeValue(items[adapterPosition].absolutePath)
                    } else {
                        checks.value?.addValue(items[adapterPosition].absolutePath)
                    }
                    checkBox.toggle()
                } else onItemClickListener.onItemClick(adapterPosition)
            }
            binding.root.setOnLongClickListener {
                binding.checkbox.visibility = View.VISIBLE
                binding.checkbox.toggle()
                checkMode = !checkMode
                checks.value?.addValue(items[adapterPosition].absolutePath)
                notifyDataSetChanged()
                true
            }
        }

        fun bind(item: File) {
            val glide = Glide.with(context)
            val mime = getMimeFile.getmime(item)
            val mimeSplit = mime.split("/")[0]
            val isDirectory = item.isDirectory
            val sizeValue = if (!isDirectory) getSize(item) else null
            with(binding) {
                name.text = item.name
                size.apply {
                    text = sizeValue
                    visibility = if (item.isDirectory) View.GONE else View.VISIBLE
                }
                when {
                    mimeSplit == "image" || mimeSplit == "video" -> {
                        glide.load(item).transition(DrawableTransitionOptions.withCrossFade())
                            .placeholder(R.color.black)
                            .error(android.R.color.black).into(icon)
                    }

                    mime == "application/vnd.android.package-archive" -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            val drawable = getMimeFile.getApkIcon(item)
                            withContext(Dispatchers.Main) {
                                glide.load(drawable).into(icon)
                            }
                        }
                    }

                    isDirectory -> {
                        glide.load(R.drawable.ic_folder).into(icon)
                    }

                    else -> {
                        val image = getMimeFile.getImageFromExtension()
                        glide.load(image).error(R.drawable.ic_document).into(icon)
                    }
                }
                if (checkMode) {
                    checkbox.visibility = View.VISIBLE
                    if (item.absolutePath in checks.value!!) {
                        if (!checkbox.isChecked) checkbox.toggle()
                    } else if (checkbox.isChecked) checkbox.toggle()
                } else checkbox.visibility = View.INVISIBLE
            }
        }
    }

    private fun getSize(file: File): String = formatSize(file.length())

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
        checks.value = if (value) {
            ArrayList(items.map { it.absolutePath })
        } else arrayListOf()
        checkMode = value
        notifyDataSetChanged()
    }
}