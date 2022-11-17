package com.mcgrady.xwidgets

import android.content.Context
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import kotlin.math.abs

/**
 * Created by mcgrady on 2022/11/17.
 */
object Utils {

    fun measure(measureSpec: Int, defaultSize: Int): Int {
        var result = defaultSize
        val specMode = View.MeasureSpec.getMode(measureSpec)
        val specSize = View.MeasureSpec.getSize(measureSpec)
        if (specMode == View.MeasureSpec.EXACTLY) {
            result = specSize
        } else if (specMode == View.MeasureSpec.AT_MOST) {
            result = result.coerceAtMost(specSize)
        }
        return result
    }

    fun dp2px(context: Context, value: Float): Int {
        return (TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics
        ) + 0.5f).toInt()
    }

    fun sp2px(context: Context, value: Float): Int {
        return (TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            value,
            context.resources.displayMetrics
        ) + 0.5f).toInt()
    }

    /**
     * 测量文字高度
     * @param paint
     * @return
     */
    fun measureTextHeight(paint: Paint): Float {
        val fontMetrics = paint.fontMetrics
        return abs(fontMetrics.ascent) - fontMetrics.descent
    }
}