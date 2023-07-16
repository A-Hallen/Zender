package com.hallen.zender.model.interfaces

interface OnProgressUpdateListener {
    abstract fun onProgressUpdate(progress: Int)
    abstract fun onSetProgressBarVisibility(visibility: Boolean)
}