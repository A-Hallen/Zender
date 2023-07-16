package com.hallen.zender.model

import android.net.Uri

data class Audio(
    val name: String,
    val path: String,
    val artist: String,
    val uri: Uri,
    val icon: Uri,
    val size: Int,
    val duration: Int
)