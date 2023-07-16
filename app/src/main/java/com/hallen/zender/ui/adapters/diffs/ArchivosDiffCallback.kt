package com.hallen.zender.ui.adapters.diffs

import androidx.recyclerview.widget.DiffUtil
import java.io.File

class ArchivosDiffCallback(
    private val oldList: List<File>,
    private val newList: List<File>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].path == newList[newItemPosition].path
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldImage = oldList[oldItemPosition]
        val newImage = newList[newItemPosition]
        return oldImage.name == newImage.name && oldImage.isDirectory == newImage.isDirectory
    }
}
