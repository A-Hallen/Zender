package com.hallen.zender.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import com.hallen.zender.MainActivity
import com.hallen.zender.R
import com.hallen.zender.databinding.FragmentArchivosBinding
import com.hallen.zender.model.interfaces.OnItemClickListener
import com.hallen.zender.ui.adapters.ArchivosAdapter
import com.hallen.zender.ui.adapters.StorageAdapter
import com.hallen.zender.utils.ActionView
import com.hallen.zender.utils.GetMimeFile
import com.hallen.zender.utils.actions.ActionDelete
import com.hallen.zender.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class ArchivosFragment : Fragment(), OnItemClickListener {
    private lateinit var binding: FragmentArchivosBinding
    private val appsViewModels: AppViewModel by activityViewModels()
    private val storageAdapter by lazy { StorageAdapter() }
    private lateinit var fileAdapter: ArchivosAdapter
    private val actionDelete by lazy { ActionDelete(requireContext()) }
    private val actionView by lazy { ActionView() }

    private val storageClickListener = object : OnItemClickListener {
        override fun onItemClick(position: Int) {
            appsViewModels.loadFiles(storageAdapter.items[position].path)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentArchivosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerViews() {
        with(binding) {
            val gridLayoutManager = GridLayoutManager(requireContext(), 2)
            gridLayoutManager.spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (storageAdapter.items.size == 1) 2 else 1
                }
            }
            storageRecyclerview.layoutManager = gridLayoutManager
            recyclerview.layoutManager = LinearLayoutManager(requireContext())
            storageAdapter.setItemClickListener(storageClickListener)
            storageRecyclerview.adapter = storageAdapter

            fileAdapter = ArchivosAdapter(requireContext()) {
                close.isInvisible = !it
                delete.isInvisible = !it
                allCb.isVisible = it
                if (allCb.isChecked && !it) allCb.toggle()
                back.isInvisible = it
                actualPath.isVisible = !it
            }
        }
        fileAdapter.setItemClickListener(this)
        binding.recyclerview.adapter = fileAdapter
    }

    private fun setupObservers() {
        appsViewModels.storageLiveData.observe(viewLifecycleOwner) {
            storageAdapter.update(it)
        }
        appsViewModels.fileLiveData.observe(viewLifecycleOwner) {
            fileAdapter.update(it)
        }
        appsViewModels.actualPath.observe(viewLifecycleOwner) {
            binding.actualPath.text = it
        }
        fileAdapter.checks.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.appFab.setImageResource(R.drawable.bottom_fav_bg_search)
                binding.counter.text = ""
                binding.counter.visibility = View.GONE
            } else {
                binding.appFab.setImageResource(R.drawable.bottom_fav_bg)
                binding.counter.visibility = View.VISIBLE
                binding.counter.text = it.size.toString()
            }
        }
    }

    private fun setupListeners() {
        with(binding) {
            back.setOnClickListener { appsViewModels.back() }
            delete.setOnClickListener { deleteFile() }
            close.setOnClickListener {
                fileAdapter.checkAll(false)
                delete.visibility = View.INVISIBLE
                close.visibility = View.INVISIBLE
                back.visibility = View.VISIBLE
            }
            appFab.setOnClickListener {
                val selectedFiles = fileAdapter.items.filter {
                    it.absolutePath in fileAdapter.checks.value!! && !it.isDirectory
                }
                val mainActivity = (requireActivity() as MainActivity)
                if (selectedFiles.isNotEmpty()) {
                    mainActivity.files.addAll(selectedFiles)
                }
                mainActivity.wifiClass.discoverDevices(false)
            }
            allCb.setOnCheckedChangeListener { _, b ->
                fileAdapter.checkAll(b)
            }
        }
    }

    private fun showVideo(file: File, mime: String) {
        val uri = appsViewModels.videosLiveData.value?.find {
            it.path == file.absolutePath
        }?.mediaUri
        if (uri != null) {
            actionView.showVideo(uri, requireContext())
        } else actionView.showItem(file, requireContext(), mime)
    }

    override fun onItemClick(position: Int) {
        val file = fileAdapter.items[position]
        if (file.isDirectory && file.canRead()) {
            appsViewModels.loadFiles(file.absolutePath)
            return
        }
        val mime: String = GetMimeFile(requireContext()).getmime(file)
        when (mime.split("/")[0]) {
            "image" -> actionView.showImage(file, requireContext())
            "video" -> showVideo(file, mime)
            else -> actionView.showItem(file, requireContext(), mime)
        }
    }

    private fun deleteFile() {
        val files = fileAdapter.checks.value?.map { File(it) } ?: return
        actionDelete.deleteFiles(files) {
            appsViewModels.reloadFiles()
            fileAdapter.checkAll(false)
        }
    }
}