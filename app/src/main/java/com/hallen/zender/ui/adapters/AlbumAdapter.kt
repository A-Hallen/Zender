package com.hallen.zender.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hallen.zender.databinding.AlbumItemBinding
import com.hallen.zender.model.Album
import java.text.SimpleDateFormat

class AlbumAdapter(var albumes: List<Album> = arrayListOf()) :
    RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {
    private val dateFormat = SimpleDateFormat.getDateInstance()
    val checks: ArrayList<Long> = arrayListOf()
    val checkeds: MutableLiveData<ArrayList<Long>> =
        MutableLiveData<ArrayList<Long>>().apply { value = arrayListOf() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = AlbumItemBinding.inflate(LayoutInflater.from(parent.context))
        return AlbumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(albumes[position])
    }

    override fun getItemCount(): Int = albumes.size

    fun checkAll(b: Boolean) {
        TODO("Not yet implemented")
    }

    inner class AlbumViewHolder(private val binding: AlbumItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(album: Album) {
            with(binding) {
                textView.text = dateFormat.format(album.date * 1000)
                checkedTextView.isChecked = album.date in checks
                checkedTextView.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        checks.add(album.date)
                    } else checks.remove(album.date)
                    checkeds.value = checks
                }
                recyclerView.layoutManager = GridLayoutManager(itemView.context, 3)
//                val adapter = ImageAdapter(album.images)
//                recyclerView.adapter = adapter
            }
        }
    }
}