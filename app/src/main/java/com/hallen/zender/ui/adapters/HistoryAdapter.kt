package com.hallen.zender.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hallen.zender.R
import com.hallen.zender.databinding.HistoryItemBinding
import com.hallen.zender.model.History
import com.hallen.zender.utils.GetMimeFile
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    context: Context,
    private var items: List<History> = emptyList()
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val getMimeFile = GetMimeFile(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            HistoryItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: HistoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(item: History) {
            val file = item.file
            val glide = Glide.with(binding.root.context)
            val mime = getMimeFile.getmime(file).split("/")[0]

            binding.name.text = file.name
            binding.size.text = formatSize(file.length())
            binding.date.text = dateFormat.format(item.date)

            when (mime) {
                "image", "video" -> {
                    glide.load(item).transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.color.black)
                        .error(android.R.color.black).into(binding.icon)
                }

                else -> {
                    val image = getMimeFile.getImageFromExtension()
                    glide.load(image).error(R.drawable.ic_document).into(binding.icon)
                }
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

}