package com.hallen.zender.ui.adapters.diffs

import androidx.recyclerview.widget.DiffUtil
import com.hallen.zender.model.Audio

class AudioDiffCallback(
    private val oldList: List<Audio>,
    private val newList: List<Audio>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].path == newList[newItemPosition].path
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldAudio = oldList[oldItemPosition]
        val newAudio = newList[newItemPosition]
        return oldAudio.path == newAudio.path
    }
}
