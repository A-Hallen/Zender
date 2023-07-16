package com.hallen.zender.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.hallen.zender.R
import com.hallen.zender.databinding.VideoItemBinding
import com.hallen.zender.model.Video
import com.hallen.zender.ui.adapters.diffs.VideoDiffCallback
import com.hallen.zender.utils.ActionView
import java.io.File

class VideoAdapter :
    RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    var videos: ArrayList<Video> = arrayListOf()
    val checkeds: MutableLiveData<ArrayList<String>> =
        MutableLiveData<ArrayList<String>>().apply { value = arrayListOf() }
    val checks: ArrayList<String> = arrayListOf()
    private var checkAll = false
    private val actionView = ActionView()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = VideoItemBinding.inflate(LayoutInflater.from(parent.context))
        val lp = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.root.layoutParams = lp
        return VideoViewHolder(binding)
    }

    // Override the onBindViewHolder method to bind the gallery data to the PhotoViewHolder
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.bind(video)
    }

    override fun getItemCount(): Int = videos.size

    fun formatBytes(size: Long): String {
        val units = arrayOf("b", "Kb", "Mb", "Gb")
        var bytes = size
        var index = 0
        while (bytes > 1024 && index < units.size - 1) {
            bytes /= 1024
            index++
        }
        return "$bytes ${units[index]}"
    }

    fun updateVideos(newVideos: ArrayList<Video>) {
        val assistanceDiffCallback = VideoDiffCallback(videos, newVideos)
        val diffResult = DiffUtil.calculateDiff(assistanceDiffCallback)
        videos = newVideos
        diffResult.dispatchUpdatesTo(this@VideoAdapter)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun checkAll(value: Boolean) {
        checkAll = value
        if (checkAll) {
            checks.clear()
            videos.map { checks.add(it.path) }
        } else checks.clear()
        checkeds.value = checks
        notifyDataSetChanged()
    }

    // Define an inner class called PhotoViewHolder that takes in a binding object
    inner class VideoViewHolder(private val binding: VideoItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        // Define a function called bind that sets the gallery data to the views in the layout
        fun bind(video: Video) {
            val file = File(video.path)
            Glide.with(binding.root.context).load(file)
                .error(android.R.color.black)
                .thumbnail(
                    Glide.with(binding.root.context).load(R.drawable.animated_vector_loading)
                        .apply(RequestOptions().override(24, 24))
                )
                .transition(DrawableTransitionOptions.withCrossFade()).into(binding.icon)
            val len = file.length()
            with(binding) {
                size.text = formatBytes(len); name.text = file.name
                if (video.path in checks) {
                    if (!checkbox.isChecked) checkbox.toggle()
                } else if (checkbox.isChecked) checkbox.toggle()
                parent.setOnClickListener {
                    checkbox.toggle()
                    if (checkbox.isChecked) {
                        checks.add(video.path)
                    } else checks.remove(video.path)
                    checkeds.value = checks
                }
                icon.setOnClickListener {
                    actionView.showVideo(video.mediaUri, binding.root.context)
                }
            }
        }
    }
}