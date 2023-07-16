package com.hallen.zender.utils

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class ClientClass(hostAddress: InetAddress, private val callback: (Socket) -> Unit) : Thread() {
    private val socket: Socket = Socket()
    private val hostAdd: String? = hostAddress.hostAddress

    override fun run() {
        try {
            socket.connect(InetSocketAddress(hostAdd, 8888), 500)
            callback(socket)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}