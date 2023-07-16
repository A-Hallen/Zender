package com.hallen.zender.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class ZDialog(context: Context) : Dialog(context) {
    override fun setContentView(view: View) {
        super.setContentView(view)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.apply {
            copyFrom(window?.attributes)
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER
        }
        window?.attributes = layoutParams
    }
}