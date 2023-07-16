package com.hallen.zender.ui.adapters.diffs

import androidx.recyclerview.widget.DiffUtil
import com.hallen.zender.model.Image

class ImageDiffCallback(
    private val oldList: List<Image>,
    private val newList: List<Image>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].imagePath == newList[newItemPosition].imagePath && oldList[oldItemPosition].dayDate == newList[newItemPosition].dayDate
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldImage = oldList[oldItemPosition]
        val newImage = newList[newItemPosition]
        return oldImage.imagePath == newImage.imagePath && oldImage.dayDate == newImage.dayDate
    }
}
