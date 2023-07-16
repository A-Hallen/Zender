package com.hallen.zender.model

import android.net.Uri

data class Storage(
    val name: String,
    val path: String,
    val uri: Uri,
    val total: Long,
    val available: Long
)