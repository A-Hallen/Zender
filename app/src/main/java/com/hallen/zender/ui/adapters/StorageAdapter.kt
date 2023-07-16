package com.hallen.zender.ui.adapters

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hallen.zender.R
import com.hallen.zender.databinding.StorageItemBinding
import com.hallen.zender.model.Storage
import com.hallen.zender.model.interfaces.OnItemClickListener

class StorageAdapter : RecyclerView.Adapter<StorageAdapter.StorageViewHolder>() {
    var items: List<Storage> = emptyList()
    private lateinit var onItemClickListener: OnItemClickListener

    fun setItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StorageViewHolder {
        val binding =
            StorageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StorageViewHolder(binding, onItemClickListener)
    }

    override fun onBindViewHolder(holder: StorageViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size


    private fun calcPercent(available: Long, total: Long): Int {
        return 100 - (100 * available / total).toInt()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update(it: List<Storage>) {
        items = it
        notifyDataSetChanged()
    }

    inner class StorageViewHolder(
        private val binding: StorageItemBinding,
        onItemClickListener: OnItemClickListener
    ) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.container.setOnClickListener {
                onItemClickListener.onItemClick(adapterPosition)
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: Storage) {
            val availableText = formatSize(item.total - item.available)
            val spannableSize = SpannableString("$availableText /${formatSize(item.total)}")
            val textColor1 = ContextCompat.getColor(binding.root.context, R.color.colorPrimary)
            val textColor2 = ContextCompat.getColor(binding.root.context, R.color.gray)
            spannableSize.setSpan(
                ForegroundColorSpan(textColor1),
                0, availableText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableSize.setSpan(
                ForegroundColorSpan(textColor2),
                availableText.length + 1,
                spannableSize.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            with(binding) {
                val percent = calcPercent(item.available, item.total)
                progressBar.progress = percent
                progressText.text = "$percent%"
                name.text = item.name
                size.text = spannableSize
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
}