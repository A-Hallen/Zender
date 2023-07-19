package com.hallen.zender.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hallen.zender.R
import com.hallen.zender.databinding.AudioItemBinding
import com.hallen.zender.model.Audio
import com.hallen.zender.ui.adapters.diffs.AudioDiffCallback
import com.hallen.zender.utils.ActionView
import java.text.SimpleDateFormat
import java.util.*

class AudioAdapter(private val checkModeCallback: (Boolean) -> Unit) :
    RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {
    private val formatter = SimpleDateFormat("mm:ss", Locale.ROOT)

    var checkedMode = false
        set(value) {
            field = value
            checkModeCallback(value)
        }
    var audios: ArrayList<Audio> = arrayListOf()
    val checkeds: MutableLiveData<ArrayList<String>> =
        MutableLiveData<ArrayList<String>>().apply { value = arrayListOf() }
    val checks: ArrayList<String> = arrayListOf()

    private val actionView = ActionView()


    fun updateImages(newAudios: ArrayList<Audio>) {
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        val audioDiffCallback = AudioDiffCallback(audios, newAudios)
        val diffResult = DiffUtil.calculateDiff(audioDiffCallback)
        audios = newAudios
        diffResult.dispatchUpdatesTo(this@AudioAdapter)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AudioAdapter.AudioViewHolder {
        val lp = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val binding = AudioItemBinding.inflate(LayoutInflater.from(parent.context))
        binding.root.layoutParams = lp
        return AudioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        holder.bind(audios[position])
    }

    override fun getItemCount(): Int = audios.size

    @SuppressLint("NotifyDataSetChanged")
    fun checkAll(value: Boolean) {
        if (value) {
            val items: List<String> = audios.map { it.path }
            checks.addAll(items)
        } else checks.clear()
        checkedMode = value
        checkeds.value = checks
        notifyDataSetChanged()
    }

    inner class AudioViewHolder(private val binding: AudioItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Define a function called bind that sets the gallery data to the views in the layout
        fun bind(audio: Audio) {
            val path = audio.path
            val uri = audio.uri
            with(binding) {
                Glide.with(root.context).load(audio.icon)
                    .thumbnail(Glide.with(root.context).load(R.drawable.icon_music))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.icon_music).into(binding.icon)
                tvName.text = audio.name
                tvArtist.text = audio.artist
                val formattedTime = formatter.format(audio.duration)
                tvDuration.text = formattedTime


                if (checkedMode) {
                    checkbox.visibility = View.VISIBLE
                    if (path in checks) {
                        if (!checkbox.isChecked) checkbox.toggle()
                    } else if (checkbox.isChecked) checkbox.toggle()
                } else {
                    checkbox.visibility = View.GONE
                }
                parent.setOnClickListener {
                    if (checkedMode) {
                        checkbox.toggle()
                        if (checkbox.isChecked) {
                            checks.add(path)
                        } else checks.remove(path)
                        checkeds.value = checks
                    } else {
                        actionView.showAudio(uri, binding.root.context)
                    }
                }
                parent.setOnLongClickListener {
                    checkedMode = !checkedMode
                    if (checkedMode) {
                        checks.add(path)
                        checkeds.value = checks
                        notifyDataSetChanged()
                    } else checkAll(false)
                    return@setOnLongClickListener true
                }
            }
        }
    }

}