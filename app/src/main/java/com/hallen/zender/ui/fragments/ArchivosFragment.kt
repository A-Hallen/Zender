package com.hallen.zender.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hallen.zender.MainActivity
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
class ArchivosFragment : Fragment(), OnItemClickListener, View.OnClickListener {
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

        binding.storageRecyclerview.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        storageAdapter.setItemClickListener(storageClickListener)
        binding.storageRecyclerview.adapter = storageAdapter
        fileAdapter = ArchivosAdapter(requireContext()) {
            with(binding) {
                if (it) {
                    close.visibility = View.VISIBLE
                    delete.visibility = View.VISIBLE
                    back.visibility = View.GONE
                } else {
                    close.visibility = View.INVISIBLE
                    delete.visibility = View.INVISIBLE
                    back.visibility = View.VISIBLE
                }
            }
        }
        fileAdapter.setItemClickListener(this)
        binding.recyclerview.adapter = fileAdapter
        appsViewModels.storageLiveData.observe(viewLifecycleOwner) {
            storageAdapter.update(it)
        }
        appsViewModels.fileLiveData.observe(viewLifecycleOwner) {
            fileAdapter.update(it)
        }
        appsViewModels.actualPath.observe(viewLifecycleOwner) {
            binding.actualPath.text = it
        }
        val views = arrayOf(binding.back, binding.close, binding.delete, binding.appFab)
        views.forEach { it.setOnClickListener(this) }

        binding.appFab.setOnClickListener {
            val files = fileAdapter.items
                .mapNotNull { it.takeIf { fileAdapter.checks.value!!.contains(it.absolutePath) } }
            val mainActivity = (requireActivity() as MainActivity)
            val wifiClass = mainActivity.wifiClass
            if (files.isEmpty()) {
                wifiClass.discoverDevices(false)
            } else {
                mainActivity.files.addAll(files)
                wifiClass.discoverDevices()
            }
        }
    }

    override fun onItemClick(position: Int) {
        val file = fileAdapter.items[position]
        if (file.isDirectory && file.canRead()) {
            appsViewModels.loadFiles(file.absolutePath)
        } else {
            val mime: String = GetMimeFile(requireContext()).getmime(file)
            when (mime.split("/")[0]) {
                "image" -> {
                    val uri = appsViewModels.albumesLiveData.value?.find {
                        it.imagePath == file.absolutePath
                    }?.mediaUri
                    if (uri != null) {
                        actionView.showImage(uri, requireContext())
                    } else actionView.showItem(file, requireContext(), mime)
                }

                "video" -> {
                    val uri = appsViewModels.videosLiveData.value?.find {
                        it.path == file.absolutePath
                    }?.mediaUri
                    if (uri != null) {
                        actionView.showVideo(uri, requireContext())
                    } else actionView.showItem(file, requireContext(), mime)
                }

                else -> {
                    actionView.showItem(file, requireContext(), mime)
                }
            }
        }
    }

    override fun onClick(view: View?) {
        with(binding) {
            when (view?.id) {
                back.id -> appsViewModels.back()
                close.id -> {
                    fileAdapter.sellectAll(false)
                    delete.visibility = View.INVISIBLE
                    close.visibility = View.INVISIBLE
                    back.visibility = View.VISIBLE
                }

                delete.id -> {
                    deleteFile()
                }
            }
        }
    }

    private fun deleteFile() {
        val files = fileAdapter.checks.value?.map { File(it) } ?: return
        actionDelete.deleteFiles(files) {
            fileAdapter.checks.value?.clear()
            appsViewModels.actualPath
            appsViewModels.reloadFiles()
        }
    }
}