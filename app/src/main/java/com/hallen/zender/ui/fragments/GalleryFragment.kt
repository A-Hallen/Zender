package com.hallen.zender.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.hallen.zender.MainActivity
import com.hallen.zender.R
import com.hallen.zender.databinding.FragmentAppsBinding
import com.hallen.zender.ui.adapters.GalleryAdapter
import com.hallen.zender.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class GalleryFragment : Fragment() {
    private lateinit var binding: FragmentAppsBinding
    private val appsViewModels: AppViewModel by activityViewModels()
    private lateinit var galleryAdapter: GalleryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        galleryAdapter = GalleryAdapter()
        val matchParent = CoordinatorLayout.LayoutParams.MATCH_PARENT
        val layoutParams = CoordinatorLayout.LayoutParams(matchParent, matchParent)
        galleryAdapter.checkeds.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                with(binding) {
                    appFab.setImageResource(R.drawable.bottom_fav_bg_search)
                    allContainer.visibility = View.GONE
                    // Establecer el valor del atributo como margen inferior para el BottomAppBar
                    recyclerview.layoutParams =
                        layoutParams.apply { topMargin = 0 }
                    counter.text = ""
                    counter.visibility = View.GONE
                }
            } else {
                // Obtener el valor del atributo actionBarSize en píxeles
                with(binding) {
                    appFab.setImageResource(R.drawable.bottom_fav_bg)
                    allContainer.visibility = View.VISIBLE
                    recyclerview.layoutParams = layoutParams.apply {
                        topMargin = binding.allCb.height
                    }
                    recyclerview.layoutParams = layoutParams.apply {
                        topMargin = binding.allCb.height
                    }
                    counter.visibility = View.VISIBLE
                    counter.text = it.size.toString()
                }
            }
        }
        binding.recyclerview.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerview.adapter = galleryAdapter
        appsViewModels.imagesLiveData.observe(viewLifecycleOwner) {
            galleryAdapter.files.addAll(it)
            galleryAdapter.notifyDataSetChanged()
        }

        binding.allCb.setOnCheckedChangeListener { _, b ->
            galleryAdapter.checkAll(b)
        }
        binding.appFab.setOnClickListener {
            val files = appsViewModels.imagesLiveData.value
                ?.mapNotNull { it.takeIf { galleryAdapter.checks.contains(it) } }
                ?.map { File(it) } ?: emptyList()

            val mainActivity = (requireActivity() as MainActivity)
            val wifiClass = mainActivity.wifiClass
            if (files.isEmpty()) {
                wifiClass.discoverDevices(false)
            } else {
                mainActivity.files.addAll(files)
                wifiClass.discoverDevices()
            }
        }
        handleBackPressed()
    }

    private fun handleBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (galleryAdapter.checks.isEmpty()) {
                        isEnabled = false
                    } else galleryAdapter.checkAll(false)
                }
            })
    }
}