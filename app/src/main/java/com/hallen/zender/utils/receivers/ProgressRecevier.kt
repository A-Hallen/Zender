package com.hallen.zender.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hallen.zender.model.interfaces.OnProgressUpdateListener
import java.io.File

/**
 * Progress recevier
 *
 * @constructor Create empty Progress recevier
 */
class ProgressRecevier : BroadcastReceiver() {
    private var progressListener: OnProgressUpdateListener? = null
    override fun onReceive(context: Context?, intent: Intent?) {
        progressListener = context as OnProgressUpdateListener
        when (intent?.action) {
            "PROGRESS_BROADCAST" -> {
                val progress = intent.getIntExtra("progress", 0)
                progressListener?.onProgressUpdate(progress)
            }

            "VISIBILITY_BROADCAST" -> {
                val visibility = intent.getBooleanExtra("visibility", false)
                progressListener?.onSetProgressBarVisibility(visibility)
            }

            "FILE_TRANSFERRED" -> {
                val path = intent.getStringExtra("FILE_PATH") ?: ""
                val target = intent.getStringExtra("TARGET") ?: ""
                val send = intent.getBooleanExtra("SEND", false)
                progressListener?.fileTransfered(File(path), target, send)
            }
        }
    }
}