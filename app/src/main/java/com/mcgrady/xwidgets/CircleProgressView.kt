package com.mcgrady.xwidgets

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView

/**
 * Created by mcgrady on 2022/11/17.
 */
open class CircleProgressView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleInt: Int = 0
) : AppCompatImageView(context, attributeSet, defStyleInt) {

    private val borderRect = RectF()
    private var borderRadius = 0f
    private var borderWidth = DEFAULT_BORDER_WIDTH
    private var borderColor = DEFAULT_BORDER_COLOR

    private val drawableRect = RectF()
    private var drawableBorderWidth = 15f
    private var drawableRadius = 0f

    private var circleBackgroundColor = Color.TRANSPARENT
    private val circleBackgroundPaint = Paint()

    private var disableCircularTransformation = false

    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null

    init {
        attributeSet?.let { initAttribute(it, defStyleInt) }
        super.setScaleType(ScaleType.CENTER_CROP)
        outlineProvider = OutlineProvider()
    }

    private fun initAttribute(attrs: AttributeSet, defStyle: Int = 0) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.CircleProgressView, defStyle, 0)

        borderWidth = attributes.getDimensionPixelSize(
            R.styleable.CircleProgressView_cpv_border_width,
            DEFAULT_BORDER_WIDTH
        )

        attributes.recycle()
    }

    private inner class OutlineProvider : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            if (disableCircularTransformation) {
                BACKGROUND.getOutline(view, outline)
            } else {
                val bounds = Rect()
                borderRect.roundOut(bounds)
                outline.setRoundRect(bounds, bounds.width() / 2.0f)
            }
        }
    }

    companion object {
        private const val DEFAULT_BORDER_WIDTH = 0
        private const val DEFAULT_BORDER_COLOR = Color.BLACK
        private const val DEFAULT_CIRCLE_BACKGROUND_COLOR = Color.TRANSPARENT
        private const val DEFAULT_IMAGE_ALPHA = 255
        private const val DEFAULT_BORDER_OVERLAY = false
    }
}
