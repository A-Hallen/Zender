package com.hallen.zender.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
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

    private val checkedsObserver = Observer<ArrayList<String>> {
        if (it.isEmpty()) {
            binding.appFab.setImageResource(R.drawable.bottom_fav_bg_search)
            binding.counter.text = ""

        } else {
            binding.appFab.setImageResource(R.drawable.bottom_fav_bg)
            binding.counter.text = it.size.toString()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        addListeners()
    }

    private fun setupRecyclerView() {
        with(binding) {
            audioAdapter = AudioAdapter {
                close.isVisible = it
                delete.isVisible = it
                allContainer.isVisible = it
                bottomView.isVisible = it
                counter.isVisible = it
                if (!it && allCb.isChecked && audioAdapter.checks.isEmpty()) allCb.toggle()
            }
            recyclerview.layoutManager = LinearLayoutManager(requireContext())
            fastScroll.setUpRecyclerView(recyclerview)
        }
    }

    private fun setupObservers() {
        audioAdapter.checkeds.observe(viewLifecycleOwner, checkedsObserver)
        binding.recyclerview.adapter = audioAdapter
        appsViewModels.audiosLiveData.observe(viewLifecycleOwner) {
            binding.fastScroll.size = it.size
            audioAdapter.updateImages(ArrayList(it))
        }
    }

    private fun addListeners() {
        with(binding) {
            close.setOnClickListener { audioAdapter.checkAll(false) }
            appFab.setOnClickListener { appFabListener() }
            allCb.setOnCheckedChangeListener { _, b -> audioAdapter.checkAll(b) }
            delete.setOnClickListener {
                if (audioAdapter.checks.isNotEmpty()) deleteFile()
            }
        }
    }

    private fun deleteFile() {
        val checks: List<Pair<String, Uri?>> = audioAdapter.checks.map { path ->
            val uri = audioAdapter.audios.firstOrNull { audio ->
                audio.path == path
            }?.uri
            Pair(path, uri)
        }
        actionDelete.deleteFile(checks) {
            audioAdapter.checkAll(false)
            appsViewModels.getAllAudios(requireContext())
        }
    }

    private fun appFabListener() {
        val audios = appsViewModels.audiosLiveData.value
            ?.mapNotNull { it.takeIf { audioAdapter.checks.contains(it.path) } }
            ?.map { File(it.path) } ?: emptyList()
        val mainActivity = (requireActivity() as MainActivity)
        mainActivity.wifiClass.discoverDevices(audios.isNotEmpty())
        mainActivity.files.addAll(audios)
    }
}