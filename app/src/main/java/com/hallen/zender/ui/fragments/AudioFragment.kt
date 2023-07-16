package com.hallen.zender.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.hallen.zender.MainActivity
import com.hallen.zender.R
import com.hallen.zender.databinding.FragmentAppsBinding
import com.hallen.zender.ui.adapters.AudioAdapter
import com.hallen.zender.utils.actions.ActionDelete
import com.hallen.zender.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class AudioFragment : Fragment() {
    private lateinit var binding: FragmentAppsBinding
    private val appsViewModels: AppViewModel by activityViewModels()
    private lateinit var audioAdapter: AudioAdapter
    private val actionDelete by lazy { ActionDelete(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        audioAdapter = AudioAdapter()
        audioAdapter.checkeds.observe(viewLifecycleOwner) {
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
                bottomNavigationView.isVisible = audioAdapter.checkedMode
            }
        }

        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerview.adapter = audioAdapter
        appsViewModels.audiosLiveData.observe(viewLifecycleOwner) {
            binding.fastScroll.size = it.size
            audioAdapter.updateImages(ArrayList(it))
        }
        binding.appFab.setOnClickListener {
            val audios = appsViewModels.audiosLiveData.value
                ?.mapNotNull { it.takeIf { audioAdapter.checks.contains(it.path) } }
                ?.map { File(it.path) } ?: emptyList()

            val mainActivity = (requireActivity() as MainActivity)
            val wifiClass = mainActivity.wifiClass
            if (audios.isEmpty()) {
                wifiClass.discoverDevices(false)
            } else {
                mainActivity.files.addAll(audios)
                wifiClass.discoverDevices()
            }
        }
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.close -> {
                    audioAdapter.checkAll(false)
                }

                R.id.delete -> {
                    if (audioAdapter.checks.isNotEmpty()) deleteFile()
                }
            }
            return@setOnItemSelectedListener true
        }
        binding.fastScroll.setUpRecyclerView(binding.recyclerview)
    }

    private fun deleteFile() {
        val checks: List<Pair<String, Uri?>> = audioAdapter.checks.map { path ->
            Pair(
                path,
                audioAdapter.audios.firstOrNull { audio ->
                    audio.path == path
                }?.uri
            )
        }
        actionDelete.deleteFile(checks) {
            audioAdapter.checks.clear()
            audioAdapter.checkeds.value = arrayListOf()
            appsViewModels.getAllAudios(requireContext())
        }
    }
}