package com.hallen.zender.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hallen.zender.databinding.GalleryItemBinding
import java.io.File

class GalleryAdapter :
    RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {


    val files: ArrayList<String> = arrayListOf()
    val checkeds: MutableLiveData<ArrayList<String>> =
        MutableLiveData<ArrayList<String>>().apply { value = arrayListOf() }
    val checks: ArrayList<String> = arrayListOf()
    private var checkAll = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding = GalleryItemBinding.inflate(LayoutInflater.from(parent.context))
        val lp = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.root.layoutParams = lp
        return GalleryViewHolder(binding)
    }

    // Override the onBindViewHolder method to bind the gallery data to the PhotoViewHolder
    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val file = files[position]
        holder.bind(file)
    }

    override fun getItemCount(): Int = files.size

    @SuppressLint("NotifyDataSetChanged")
    fun checkAll(value: Boolean) {
        checkAll = value
        if (checkAll) {
            checks.clear()
            files.map { checks.add(it) }
        } else checks.clear()
        checkeds.value = checks
        notifyDataSetChanged()
    }

    // Define an inner class called PhotoViewHolder that takes in a binding object
    inner class GalleryViewHolder(private val binding: GalleryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Define a function called bind that sets the gallery data to the views in the layout
        fun bind(file: String) {
            Glide.with(binding.root.context).load(File(file))
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(android.R.color.black)
                .into(binding.icon)
            with(binding) {
                if (file in checks) {
                    if (!checkbox.isChecked) checkbox.toggle()
                } else if (checkbox.isChecked) checkbox.toggle()
                parent.setOnClickListener {
                    checkbox.toggle()
                    if (checkbox.isChecked) {
                        checks.add(file)
                    } else checks.remove(file)
                    checkeds.value = checks
                }
            }
        }
    }
}