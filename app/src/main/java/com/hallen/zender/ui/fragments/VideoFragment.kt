package com.hallen.zender.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.hallen.zender.MainActivity
import com.hallen.zender.R
import com.hallen.zender.databinding.FragmentAppsBinding
import com.hallen.zender.ui.adapters.VideoAdapter
import com.hallen.zender.utils.actions.ActionDelete
import com.hallen.zender.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class VideoFragment : Fragment() {

    private lateinit var binding: FragmentAppsBinding
    private val appsViewModels: AppViewModel by activityViewModels()
    private lateinit var videoAdapter: VideoAdapter
    private val actionDelete by lazy { ActionDelete(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoAdapter = VideoAdapter()
        val matchParent = CoordinatorLayout.LayoutParams.MATCH_PARENT
        val layoutParams = CoordinatorLayout.LayoutParams(matchParent, matchParent)
        videoAdapter.checkeds.observe(viewLifecycleOwner) {
            with(binding) {
                if (it.isEmpty()) {
                    appFab.setImageResource(R.drawable.bottom_fav_bg_search)
                    allContainer.visibility = View.GONE
                    // Establecer el valor del atributo como margen inferior para el BottomAppBar
                    recyclerview.layoutParams =
                        layoutParams.apply { bottomMargin = 0; topMargin = 0 }
                    counter.visibility = View.GONE
                    counter.text = ""
                } else {
                    appFab.setImageResource(R.drawable.bottom_fav_bg)
                    allContainer.visibility = View.VISIBLE
                    recyclerview.layoutParams = layoutParams.apply {
                        topMargin = binding.allCb.height
                    }
                    counter.visibility = View.VISIBLE
                    counter.text = it.size.toString()
                }
                bottomNavigationView.isVisible = it.isNotEmpty()
            }
        }
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerview.adapter = videoAdapter
        appsViewModels.videosLiveData.observe(viewLifecycleOwner) {
            binding.fastScroll.size = it.size
            videoAdapter.updateVideos(ArrayList(it))
        }

        binding.allCb.setOnCheckedChangeListener { _, b ->
            videoAdapter.checkAll(b)
        }
        binding.appFab.setOnClickListener {
            val files = appsViewModels.videosLiveData.value
                ?.mapNotNull { it.takeIf { videoAdapter.checks.contains(it.path) } }
                ?.map { File(it.path) } ?: emptyList()

            val mainActivity = (requireActivity() as MainActivity)
            val wifiClass = mainActivity.wifiClass
            if (files.isEmpty()) {
                wifiClass.discoverDevices(false)
            } else {
                mainActivity.files.addAll(files)
                wifiClass.discoverDevices()
            }
        }
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.close -> {
                    videoAdapter.checkAll(false)
                }

                R.id.delete -> {
                    if (videoAdapter.checks.isNotEmpty()) deleteFile()
                }
            }
            return@setOnItemSelectedListener true
        }
        binding.fastScroll.setUpRecyclerView(binding.recyclerview)
    }

    private fun deleteFile() {
        val checks: List<Pair<String, Uri?>> = videoAdapter.checks.map { path ->
            Pair(
                path,
                videoAdapter.videos.firstOrNull { video ->
                    video.path == path
                }?.mediaUri
            )
        }
        actionDelete.deleteFile(checks) {
            videoAdapter.checks.clear()
            videoAdapter.checkeds.value = arrayListOf()
            appsViewModels.getAllVideos(requireContext())
        }
    }
}