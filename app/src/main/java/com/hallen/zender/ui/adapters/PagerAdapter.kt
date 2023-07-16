package com.hallen.zender.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hallen.zender.ui.fragments.*

class PagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ArchivosFragment()
            1 -> AppsFragment()
            2 -> AlbumFragment()
            3 -> VideoFragment()
            else -> AudioFragment()
        }
    }


}