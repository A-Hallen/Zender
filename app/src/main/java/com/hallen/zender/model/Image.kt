package com.hallen.zender.model

import android.net.Uri

data class Image(
    val imagePath: String = "",
    val mediaUri: Uri = Uri.EMPTY,
    val date: Long = 0L,
    val dayDate: Long = 0L,
    val header: Boolean = false
)