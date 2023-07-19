package com.hallen.zender.ui.adapters.diffs

import androidx.recyclerview.widget.DiffUtil
import com.hallen.zender.model.History

class HistoryDiffCallback(
    private val oldList: List<History>,
    private val newList: List<History>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldHistory = oldList[oldItemPosition]
        val newHistory = newList[newItemPosition]
        return oldHistory.file.path == newHistory.file.path
    }
}