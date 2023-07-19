package com.hallen.zender.ui.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.hallen.zender.R
import com.hallen.zender.databinding.DetailsDialogBinding
import com.hallen.zender.databinding.HistoryFragmentBinding
import com.hallen.zender.model.interfaces.OnItemClickListener
import com.hallen.zender.ui.adapters.HistoryAdapter
import com.hallen.zender.utils.ActionView
import com.hallen.zender.utils.GetMimeFile
import com.hallen.zender.utils.ZDialog
import com.hallen.zender.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@AndroidEntryPoint
class HistoryFragment : Fragment(), OnItemClickListener {
    private val appsViewModels: AppViewModel by activityViewModels()
    private lateinit var binding: HistoryFragmentBinding
    private lateinit var adapter: HistoryAdapter
    private val actionView by lazy { ActionView() }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HistoryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        appsViewModels.historyLiveData.observe(viewLifecycleOwner) {
            adapter.updateHistory(it)
        }
        appsViewModels.getAllHistory()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(requireContext()) {
            binding.bottomLayout.isVisible = it
            binding.allCb.isVisible = it
        }
        adapter.setItemClickListener(this)
        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        binding.back.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.cancel.setOnClickListener {
            if (binding.allCb.isChecked) binding.allCb.toggle()
            binding.allCb.visibility = View.GONE
            adapter.checkAll(false)
        }
        binding.details.setOnClickListener {
            showFileDetails()
        }
        binding.delete.setOnClickListener {
            deleteHistories()
        }
        binding.allCb.setOnCheckedChangeListener { _, b ->
            adapter.checkAll(b)
        }
    }

    private fun showVideo(file: File, mime: String) {
        val uri = appsViewModels.videosLiveData.value?.find {
            it.path == file.absolutePath
        }?.mediaUri
        uri?.let {
            actionView.showVideo(it, requireContext())
        } ?: actionView.showItem(file, requireContext(), mime)
    }

    override fun onItemClick(position: Int) {
        val file = adapter.items[position].file
        if (!file.exists()) return
        val mime: String = GetMimeFile(requireContext()).getmime(file)
        when (mime.split("/")[0]) {
            "image" -> actionView.showImage(file, requireContext())
            "video" -> showVideo(file, mime)
            else -> actionView.showItem(file, requireContext(), mime)
        }
    }

    private fun deleteHistories() {
        val historiesId = adapter.checks
        if (historiesId.isNotEmpty()) {
            appsViewModels.deleteHistoriesbyId(historiesId)
        }
    }


    private fun showFileDetails() {
        val itemId = adapter.checks.lastOrNull() ?: return
        val file = adapter.items.find { it.id == itemId }?.file ?: return
        if (!file.exists()) {
            Toast.makeText(requireContext(), "El archivo ya no existe", Toast.LENGTH_SHORT).show()
            return
        }

        val detailsArray = details(file)
        val dialog = ZDialog(requireContext())
        val binding = DetailsDialogBinding.inflate(dialog.layoutInflater)
        dialog.apply {
            setContentView(binding.root)
            with(binding) {
                val listAdapter = ArrayAdapter(
                    requireContext(),
                    R.layout.details_item,
                    detailsArray
                )
                listView.adapter = listAdapter
            }
            show()
        }
    }

    private fun formatSize(size: Long): String {
        return when (size) {
            in 0L..1000L -> "$size Bytes"
            in 1000L..1000000L -> String.format("%.2f", (size / 1000F)) + "Kb"
            in 1000000L..1000000000L -> {
                String.format("%.2f", (size / 1000000F)) + "Mb"
            }

            in 1000000000L..1000000000000L -> {
                String.format("%.2f", (size / 1000000000F)) + "Gb"
            }

            else -> "0 Bytes"
        }
    }

    private fun getText(text1: String, text2: String): SpannableString {
        val spannable = SpannableString("$text1: $text2")
        val colorTexto1 = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        val colorTexto2 = ContextCompat.getColor(requireContext(), R.color.darkgray2)
        val boldSpan = StyleSpan(Typeface.BOLD)
        val normalSpan = StyleSpan(Typeface.NORMAL)
        spannable.setSpan(
            ForegroundColorSpan(colorTexto2),
            0,
            text1.length + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(colorTexto1),
            text1.length + 1,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        spannable.setSpan(
            boldSpan,
            0,
            text1.length + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            normalSpan,
            text1.length + 1,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    private fun details(file: File): Array<SpannableString> {
        val fileSize = formatSize(file.length())
        val modificado: String =
            SimpleDateFormat.getDateInstance().format(Date(file.lastModified()))
        val parentPath = file.parentFile?.absolutePath ?: file.absolutePath

        return arrayOf(
            getText("Nombre", file.name),
            getText("Ruta", parentPath),
            getText("Tipo", if (file.isDirectory) "Directorio" else "Archivo"),
            getText("Tamaño", fileSize),
            getText("Modificado", modificado),
            getText("Lectura", if (file.canRead()) "Si" else "No"),
            getText("Escritura", if (file.canWrite()) "Si" else "No"),
            getText("Oculto", if (file.isHidden) "Si" else "No")
        )
    }
}