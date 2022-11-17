package com.mcgrady.xwidgets

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView
import com.mcgrady.xwidgets.CircleProgressView.OutlineProvider

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
    private var borderWidth = 15f

    private val drawableRect = RectF()
    private var drawableBorderWidth = 15f
    private var drawableRadius = 0f

    private var circleBackgroundColor = Color.TRANSPARENT
    private val circleBackgroundPaint = Paint()

    private var disableCircularTransformation = false


    init {
        super.setScaleType(ScaleType.CENTER_CROP)

        outlineProvider = OutlineProvider()
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
}