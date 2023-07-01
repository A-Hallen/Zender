package com.hallen.zender.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.hallen.zender.MainActivity
import com.hallen.zender.databinding.FragmentAppsBinding
import com.hallen.zender.ui.adapters.AppAdapter
import com.hallen.zender.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class AppsFragment : Fragment() {
    private lateinit var binding: FragmentAppsBinding
    private val appsViewModels: AppViewModel by viewModels()
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
        appAdapter.checkeds.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                with(binding) {
                    appFab.hide()
                    bottomAppBar.performHide()
                    allContainer.visibility = View.GONE
                    // Establecer el valor del atributo como margen inferior para el BottomAppBar
                    recyclerview.layoutParams =
                        layoutParams.apply { bottomMargin = 0; topMargin = 0 }
                    counter.text = ""
                }
            } else {
                // Obtener el valor del atributo actionBarSize en píxeles
                val actionBarSize =
                    resources.getDimensionPixelSize(com.google.android.material.R.dimen.abc_action_bar_default_height_material)
                with(binding) {
                    appFab.show()
                    bottomAppBar.visibility = View.VISIBLE
                    bottomAppBar.performShow()
                    allContainer.visibility = View.VISIBLE
                    recyclerview.layoutParams = layoutParams.apply {
                        bottomMargin = actionBarSize; topMargin = binding.allCb.height
                    }; counter.text = it.size.toString()
                }
            }
        }

        binding.recyclerview.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.recyclerview.adapter = appAdapter
        appsViewModels.appModel.observe(viewLifecycleOwner) {
            appAdapter.apps.addAll(it)
            appAdapter.notifyDataSetChanged()
        }
        appsViewModels.getApps(requireContext())

        binding.allCb.setOnCheckedChangeListener { _, b ->
            appAdapter.checkAll(b)
        }
        binding.appFab.setOnClickListener {
            val files = appsViewModels.appModel.value
                ?.mapNotNull { it.takeIf { appAdapter.checks.contains(it.packageName) } }
                ?.map { File(it.sourceDir) } ?: emptyList()

            (requireActivity() as MainActivity).sendFile(files)
        }
    }

}