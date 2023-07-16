package com.hallen.zender.ui.adapters.diffs

import androidx.recyclerview.widget.DiffUtil
import com.hallen.zender.model.Video

class VideoDiffCallback(
    private val oldList: List<Video>,
    private val newList: List<Video>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].path == newList[newItemPosition].path
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldVideo = oldList[oldItemPosition]
        val newVideo = newList[newItemPosition]
        return oldVideo.path == newVideo.path
    }
}