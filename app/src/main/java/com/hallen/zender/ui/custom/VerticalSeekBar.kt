package com.hallen.zender.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import androidx.recyclerview.widget.RecyclerView
import com.hallen.zender.R
import kotlin.math.roundToInt

class VerticalSeekBar : androidx.appcompat.widget.AppCompatSeekBar {
    private var isFromUser: Boolean = false
    var size: Int = 0

    private val fadeOutAnimation =
        AnimationUtils.loadAnimation(this.context, R.anim.fade_out).apply {
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    visibility = View.GONE
                }

            })
        }
    var handler: Thread? = null

    private fun scrollListener(recyclerView: RecyclerView) {
        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, prog: Int, fromUser: Boolean) {
                if (isFromUser) {
                    val p = (prog - 100) * -1
                    val percentage: Int =
                        if (p == 0) 0 else (p / 100f * size).toInt()
                    recyclerView.scrollToPosition(percentage)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }

    private val recyclerViewScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> { // si el RecyclerView ha terminado de desplazarse
                    try {
                        if (handler == null) {
                            handler = object : Thread() {
                                override fun run() {
                                    try {
                                        sleep(3000)
                                        startAnimation(fadeOutAnimation)
                                    } catch (e: InterruptedException) {
                                    }
                                }
                            }
                        }
                        handler?.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                RecyclerView.SCROLL_STATE_DRAGGING -> {
                    fadeOutAnimation.cancel()
                    handler?.interrupt(); handler = null
                    visibility = View.VISIBLE
                }
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val offset = recyclerView.computeVerticalScrollOffset()
            val extent = recyclerView.computeVerticalScrollExtent()
            val range = recyclerView.computeVerticalScrollRange()

            if (offset != 0) {
                val percentage: Float = 100.0f * offset / (range - extent).toFloat() - 100
                // Actualiza el SeekBar con el porcentaje de scroll
                progress = percentage.roundToInt() * -1
            }
            isFromUser = false
        }

    }

    fun setUpRecyclerView(recyclerView: RecyclerView) {
        scrollListener(recyclerView)
        recyclerView.addOnScrollListener(recyclerViewScrollListener)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context, attrs, defStyle
    )

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(h, w, oldh, oldw)
    }

    @Synchronized
    override fun setProgress(progress: Int) // it is necessary for calling setProgress on click of a button
    {
        super.setProgress(progress)
        onSizeChanged(width, height, 0, 0)
    }

    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(c: Canvas) {
        c.rotate(-90f)
        c.translate(-height.toFloat(), 0f)
        super.onDraw(c)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                progress = max - (max * event.y / height).toInt()
                onSizeChanged(width, height, 0, 0)
                isFromUser = true
            }
            MotionEvent.ACTION_CANCEL -> {}
        }
        return true
    }
}