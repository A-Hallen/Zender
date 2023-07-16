package com.hallen.zender.utils

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class ServerClass(private val callback: (Socket, ServerSocket) -> Unit) : Thread() {
    private lateinit var socket: Socket
    private lateinit var serverSocket: ServerSocket

    override fun run() {
        try {
            serverSocket = ServerSocket(8888)
            socket = serverSocket.accept()
            callback(socket, serverSocket)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}