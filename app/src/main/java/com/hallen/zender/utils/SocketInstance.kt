package com.hallen.zender.utils

import java.net.Socket

class SocketInstance {
    companion object {
        private var socket: Socket? = null

        fun setSocket(skt: Socket) {
            socket = skt
        }

        fun getSocket(): Socket? {
            return socket
        }
    }
}