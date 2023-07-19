package com.hallen.zender.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.hallen.zender.MainActivity
import com.hallen.zender.R
import com.hallen.zender.databinding.FragmentAppsBinding
import com.hallen.zender.ui.adapters.AppAdapter
import com.hallen.zender.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class AppsFragment : Fragment() {
    private lateinit var binding: FragmentAppsBinding
    private val appsViewModels: AppViewModel by activityViewModels()
    private lateinit var appAdapter: AppAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appAdapter = AppAdapter()

        val matchParent = CoordinatorLayout.LayoutParams.MATCH_PARENT
        val layoutParams = CoordinatorLayout.LayoutParams(matchParent, matchParent)
        layoutParams.topMargin = 0
        val marginLayoutParams = CoordinatorLayout.LayoutParams(matchParent, matchParent)
        marginLayoutParams.topMargin = binding.allContainer.height

        binding.delete.visibility = View.INVISIBLE

        appAdapter.checkeds.observe(viewLifecycleOwner) {
            with(binding) {
                bottomView.isVisible = it.isNotEmpty()
                if (it.isEmpty()) {
                    allContainer.visibility = View.GONE
                    appFab.setImageResource(R.drawable.bottom_fav_bg_search)
                    recyclerview.layoutParams = layoutParams
                    counter.visibility = View.GONE
                    counter.text = ""
                    return@with
                }
                appFab.setImageResource(R.drawable.bottom_fav_bg)
                allContainer.visibility = View.VISIBLE
                recyclerview.layoutParams = marginLayoutParams
                counter.visibility = View.VISIBLE
                counter.text = it.size.toString()
            }
        }

        binding.recyclerview.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.recyclerview.adapter = appAdapter
        appsViewModels.appModel.observe(viewLifecycleOwner) {
            binding.fastScroll.size = it.size
            appAdapter.apps.addAll(it)
            appAdapter.notifyDataSetChanged()
        }

        binding.allCb.setOnCheckedChangeListener { _, b ->
            appAdapter.checkAll(b)
        }
        binding.appFab.setOnClickListener {
            val files = appsViewModels.appModel.value
                ?.mapNotNull { it.takeIf { appAdapter.checks.contains(it.packageName) } }
                ?.map { File(it.sourceDir) } ?: emptyList()

            val mainActivity = (requireActivity() as MainActivity)
            val wifiClass = mainActivity.wifiClass
            if (files.isEmpty()) {
                wifiClass.discoverDevices(false)
            } else {
                mainActivity.files.addAll(files)
                wifiClass.discoverDevices()
            }
        }
        binding.close.setOnClickListener {
            if (binding.allCb.isChecked) binding.allCb.toggle()
            appAdapter.checkAll(false)
        }
        binding.fastScroll.setUpRecyclerView(binding.recyclerview)
    }
}