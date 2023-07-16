package com.hallen.zender.model


data class Album(
    val date: Long,
    var images: ArrayList<Image> = arrayListOf()
)
