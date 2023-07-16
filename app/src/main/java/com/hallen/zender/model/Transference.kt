package com.hallen.zender.model

import java.io.File

data class Transference(
    val file: File,
    val sender: String,
    val receiver: String
)
