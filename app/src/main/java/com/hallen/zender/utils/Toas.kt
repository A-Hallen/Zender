package com.hallen.zender.utils

import android.content.Context
import android.widget.Toast

class Toas(
    context: Context,
    message: String,
    duration: Int = Toast.LENGTH_SHORT
) {
    init {
        Toast.makeText(context, message, duration).show()
    }
}