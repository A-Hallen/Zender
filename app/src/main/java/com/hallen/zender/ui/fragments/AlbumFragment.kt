package com.hallen.zender.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.hallen.zender.MainActivity
import com.hallen.zender.R
import com.hallen.zender.databinding.FragmentAppsBinding
import com.hallen.zender.model.Image
import com.hallen.zender.ui.adapters.ImageAdapter
import com.hallen.zender.utils.actions.ActionDelete
import com.hallen.zender.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class AlbumFragment : Fragment() {
    private lateinit var binding: FragmentAppsBinding
    private val appsViewModels: AppViewModel by activityViewModels()
    private lateinit var imagesAdapter: ImageAdapter
    private val actionDelete by lazy { ActionDelete(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imagesAdapter = ImageAdapter()
        setObservers()
        setupRecyclerview()
        setupListeners()
        binding.fastScroll.setUpRecyclerView(binding.recyclerview)
    }

    private fun sendImages() {
        val selectedImages = appsViewModels.albumesLiveData.value?.filter {
            it.imagePath in imagesAdapter.checks
        }
        val selectedFiles = selectedImages?.map { File(it.imagePath) } ?: listOf()
        val mainActivity = (requireActivity() as MainActivity)
        if (selectedFiles.isNotEmpty()) {
            mainActivity.files.addAll(selectedFiles)
        }
        mainActivity.wifiClass.discoverDevices(false)
    }

    private fun setupListeners() {
        binding.appFab.setOnClickListener {
            sendImages()
        }
        binding.close.setOnClickListener {
            imagesAdapter.checkAll(false)
        }
        binding.delete.setOnClickListener {
            if (imagesAdapter.checks.isNotEmpty()) deleteFile()
        }
    }

    private fun setupRecyclerview() {
        val gridLayoutManager = GridLayoutManager(requireContext(), 3)
        gridLayoutManager.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val header = imagesAdapter.images[position].header
                return if (header) 3 else 1
            }
        }
        binding.recyclerview.layoutManager = gridLayoutManager
        binding.recyclerview.adapter = imagesAdapter
    }

    private fun setObservers() {
        imagesAdapter.checkeds.observe(viewLifecycleOwner) {
            with(binding) {
                if (it.isEmpty()) {
                    appFab.setImageResource(R.drawable.bottom_fav_bg_search)
                    allContainer.visibility = View.GONE
                    counter.visibility = View.GONE
                    counter.text = ""
                } else {
                    appFab.setImageResource(R.drawable.bottom_fav_bg)
                    counter.visibility = View.VISIBLE
                    counter.text = it.size.toString()
                }
                bottomView.isVisible = imagesAdapter.checkedMode
            }
        }
        appsViewModels.albumesLiveData.observe(viewLifecycleOwner) {
            binding.fastScroll.size = it.size
            val orderImages: ArrayList<Image> =
                ArrayList(it.sortedByDescending { image -> image.date * 1000L })
            imagesAdapter.updateImages(orderImages)
        }
    }

    private fun deleteFile() {
        // Search selected files and his uri and deleted
        val checks: List<Pair<String, Uri?>> = imagesAdapter.checks.map { path ->
            Pair(
                path,
                imagesAdapter.images.firstOrNull { image ->
                    image.imagePath == path
                }?.mediaUri
            )
        }
        actionDelete.deleteFile(checks) {
            imagesAdapter.checkAll(false)
            appsViewModels.getAllImages(requireContext())
        }
    }
}