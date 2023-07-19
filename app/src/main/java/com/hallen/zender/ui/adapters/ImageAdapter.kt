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
import com.hallen.zender.databinding.GalleryHeaderItemBinding
import com.hallen.zender.databinding.GalleryItemBinding
import com.hallen.zender.model.Image
import com.hallen.zender.ui.adapters.diffs.ImageDiffCallback
import com.hallen.zender.utils.ActionView
import java.io.File
import java.text.SimpleDateFormat

class ImageAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1
    private val dateFormat = SimpleDateFormat.getDateInstance()
    var checkedMode = false
    var images: ArrayList<Image> = arrayListOf()
    val checkeds: MutableLiveData<ArrayList<String>> =
        MutableLiveData<ArrayList<String>>().apply { value = arrayListOf() }
    val checks: ArrayList<String> = arrayListOf()

    private var headerCheck: ArrayList<Long> = arrayListOf()
    private val actionView = ActionView()

    override fun getItemViewType(position: Int): Int {
        return if (images[position].header) TYPE_HEADER else TYPE_ITEM
    }

    fun updateImages(newImages: ArrayList<Image>) {
        val assistanceDiffCallback = ImageDiffCallback(images, newImages)
        val diffResult = DiffUtil.calculateDiff(assistanceDiffCallback)
        images = newImages
        diffResult.dispatchUpdatesTo(this@ImageAdapter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val lp = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return if (viewType == TYPE_ITEM) {
            val binding = GalleryItemBinding.inflate(LayoutInflater.from(parent.context))
            binding.root.layoutParams = lp
            GalleryViewHolder(binding)
        } else {
            val binding = GalleryHeaderItemBinding.inflate(LayoutInflater.from(parent.context))
            GalleryHeaderHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is GalleryHeaderHolder) {
            holder.bind(images[position])
        } else {
            val image = images[position]
            (holder as GalleryViewHolder).bind(image)
        }
    }

    override fun getItemCount(): Int = images.size

    @SuppressLint("NotifyDataSetChanged")
    fun checkAll(value: Boolean, date: Long = 0L) {
        if (date == 0L) {
            if (value) {
                val items: List<Image> = images.filter { !it.header }
                checks.addAll(items.map { it.imagePath })
            } else {
                headerCheck.clear()
                checks.clear()
            }
            checkedMode = value
        } else {
            if (value) {
                val items: List<String> =
                    images.filter { it.dayDate == date && !it.header }.map { it.imagePath }
                checks.addAll(items)
                checkedMode = true
            } else {
                images.filter { it.dayDate == date && it.imagePath in checks }
                    .map { checks.remove(it.imagePath) }
            }
        }
        checkeds.value = checks
        notifyDataSetChanged()
    }

    inner class GalleryViewHolder(private val binding: GalleryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Define a function called bind that sets the gallery data to the views in the layout
        fun bind(image: Image) {
            val path = image.imagePath
            val uri = image.mediaUri
            Glide.with(binding.root.context).load(uri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(android.R.color.black)
                .into(binding.icon)
            with(binding) {
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
                        actionView.showImage(File(path), binding.root.context)
                        //actionView.showImage(uri, binding.root.context)
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

    inner class GalleryHeaderHolder(private val binding: GalleryHeaderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(image: Image) {

            binding.textView.text = dateFormat.format(image.date * 1000)
            binding.checkedTextView.isChecked = image.date in headerCheck

            binding.checkedTextView.setOnClickListener {
                val checkBox = binding.checkedTextView
                checkBox.toggle()
                if (checkBox.isChecked) {
                    headerCheck.remove(image.date)
                } else {
                    headerCheck.add(image.date)
                }
                checkAll(!checkBox.isChecked, image.dayDate)
            }

        }
    }

}