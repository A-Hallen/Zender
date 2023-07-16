package com.hallen.zender.ui.adapters

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.bumptech.glide.Glide
import com.hallen.zender.R
import com.hallen.zender.databinding.DialogDeviceItemBinding

class DeviceAdapter(
    var items: ArrayList<WifiP2pDevice> = arrayListOf()
) : BaseAdapter() {
    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding = DialogDeviceItemBinding.inflate(
            LayoutInflater.from(parent?.context), null, false
        )

        Glide.with(binding.icon.context).load(R.drawable.ic_wifi).into(binding.icon)
        binding.text.text = items[position].deviceName
        return binding.root
    }

    fun newItems(it: MutableCollection<WifiP2pDevice>) {
        items.clear(); items.addAll(it)
        notifyDataSetChanged()
    }
}