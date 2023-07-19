package com.hallen.zender.model.interfaces

import java.io.File

interface OnProgressUpdateListener {
    abstract fun onProgressUpdate(progress: Int)
    abstract fun onSetProgressBarVisibility(visibility: Boolean)

    abstract fun fileTransfered(file: File, target: String, send: Boolean)
}