package com.mcgrady.xwidgets

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Created by mcgrady on 2021/6/4.
 * Circle progress imageview with badge
 */
open class CircleProgressBadgeView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleInt: Int = 0
) : AppCompatImageView(context, attributeSet, defStyleInt) {

    private var initialized: Boolean = false

    //Border
    private val borderRect = RectF()
    private var borderRadius = 0f
    var borderWidth = DEFAULT_BORDER_WIDTH
        set(value) {
            if (value == borderWidth) {
                return
            }

            field = value
            borderPaint.strokeWidth = value.toFloat()
            updateDimensions()
            invalidate()
        }

    @ColorInt
    var borderColor: Int = DEFAULT_BORDER_COLOR
        set(value) {
            if (value == borderColor) {
                return
            }

            field = value
            borderPaint.color = value
            invalidate()
        }
    private val borderPaint = Paint()

    //Drawable
    private val drawableRect = RectF()
    private var drawableBorderWidth = 15f
    private var drawableRadius = 0f
    private var drawableDirty: Boolean = false

    //Background
    @ColorInt
    var circleBackgroundColor: Int = Color.TRANSPARENT
        set(value) {
            if (value == circleBackgroundColor) {
                return
            }

            field = value
            circleBackgroundPaint.color = value
            invalidate()
        }
    private val circleBackgroundPaint = Paint()
    private var disableCircularTransformation = false

    //Bitmap
    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null
    private val bitmapPaint = Paint()
    private val shaderMatrix = Matrix()
    private var rebuildShader: Boolean = false
    private var imageAlphaWrapper = DEFAULT_IMAGE_ALPHA
    private var imageColorFilter: ColorFilter? = null

    //Badge
    private val badgeRect = RectF()
    private var badgeCx = 0f
    private var badgeCy = 0f
    private var badgeBitmap: Bitmap? = null

    @ColorInt
    private var badgeColor: Int = DEFAULT_BADGE_COLOR
    private var badgeWidth: Int = DEFAULT_BADGE_WIDTH
    private var badgeRadius = 0f
    private val badgePaint = Paint()

    private val debugPaint = Paint()

    init {
        initialized = true

        attributeSet?.let { initAttribute(it, defStyleInt) }

        super.setScaleType(ScaleType.CENTER_CROP)

        bitmapPaint.run {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            alpha = imageAlphaWrapper
            colorFilter = imageColorFilter
        }
        borderPaint.run {
            style = Paint.Style.STROKE
            isAntiAlias = true
            color = borderColor
            strokeWidth = borderWidth.toFloat()
        }
        circleBackgroundPaint.run {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = circleBackgroundColor
        }
        badgePaint.run {
//            style = Paint.Style.FILL
            isAntiAlias = true
            color = badgeColor
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        if (BuildConfig.DEBUG) {
            debugPaint.run {
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(5f, 5f, 5f, 5f), 0f)
                color = Color.RED
            }
        }

        outlineProvider = OutlineProvider()
    }

    private fun initAttribute(attrs: AttributeSet, defStyle: Int = 0) {
        val attributes =
            context.obtainStyledAttributes(attrs, R.styleable.CircleProgressBadgeView, defStyle, 0)

        borderWidth = attributes.getDimensionPixelSize(
            R.styleable.CircleProgressBadgeView_cpbv_border_width,
            DEFAULT_BORDER_WIDTH
        )
        borderColor = attributes.getColor(
            R.styleable.CircleProgressBadgeView_cpbv_border_color,
            DEFAULT_BORDER_COLOR
        )
        circleBackgroundColor = attributes.getColor(
            R.styleable.CircleProgressBadgeView_cpbv_circle_background_color,
            DEFAULT_CIRCLE_BACKGROUND_COLOR
        )
        badgeWidth = attributes.getDimensionPixelSize(
            R.styleable.CircleProgressBadgeView_cpbv_badge_width,
            DEFAULT_BADGE_WIDTH
        )

        attributes.recycle()
    }

    override fun onDraw(canvas: Canvas?) {
        if (disableCircularTransformation || canvas == null) {
            super.onDraw(canvas)
            return
        }

        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        //background
        if (circleBackgroundColor != Color.TRANSPARENT) {
            canvas.drawCircle(
                drawableRect.centerX(),
                drawableRect.centerY(),
                drawableRadius,
                circleBackgroundPaint
            )
        }

        //bitmap
        if (bitmap != null) {
            if (drawableDirty && bitmapCanvas != null) {
                drawableDirty = false
                drawable.setBounds(0, 0, bitmapCanvas!!.width, bitmapCanvas!!.height)
                drawable.draw(bitmapCanvas!!)
            }

            if (rebuildShader) {
                rebuildShader = false
                bitmapPaint.shader =
                    BitmapShader(bitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                        setLocalMatrix(shaderMatrix)
                    }
            }

            canvas.drawCircle(
                drawableRect.centerX(),
                drawableRect.centerY(),
                drawableRadius,
                bitmapPaint
            )
        }

        //border
        if (borderWidth > 0) {
            canvas.drawCircle(borderRect.centerX(), borderRect.centerY(), borderRadius, borderPaint)
        }

        //badge
        canvas.drawCircle(badgeCx, badgeCy, badgeRadius, badgePaint)
        canvas.restoreToCount(layerId)


        val totalDegrees = (270f + animationDrawingState.rotationInDegrees) % 360
        drawArches(totalDegrees, canvas)
        val startOfMainArch = totalDegrees + (animationDrawingState.archesAreaInDegrees)
        canvas.drawArc(
            arcBorderRect,
            startOfMainArch,
            360 - animationDrawingState.archesAreaInDegrees,
            false,
            borderPaint
        )


        if (BuildConfig.DEBUG) {
            canvas.drawLine(
                borderRect.left,
                borderRect.centerY(),
                borderRect.right,
                borderRect.centerY(),
                debugPaint
            )
            canvas.drawLine(
                borderRect.centerX(),
                borderRect.top,
                borderRect.centerX(),
                borderRect.bottom,
                debugPaint
            )
            canvas.drawLine(
                borderRect.centerX(),
                borderRect.centerY(),
                borderRect.right,
                borderRect.bottom,
                debugPaint
            )
            canvas.drawLine(
                borderRect.centerX(),
                borderRect.bottom,
                borderRect.right,
                borderRect.centerY(),
                debugPaint
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateDimensions()
        invalidate()
    }

    private fun updateDimensions() {
        borderRect.set(calculateBounds())
        borderRadius =
            ((borderRect.height() - borderWidth) / 2.0f).coerceAtMost((borderRect.width() - borderWidth) / 2.0f)

        drawableRect.set(borderRect)
        drawableRect.inset(borderWidth + drawableBorderWidth, borderWidth + drawableBorderWidth)
        drawableRadius = (drawableRect.height() / 2f).coerceAtLeast((drawableRect.width() / 2f))

        updateShaderMatrix()

        /**
         * X坐标= a + Math.sin(角度 * (Math.PI / 180)) * r
         * Y坐标= b + Math.cos(角数 * (Math.PI / 180)) * r
         * 默认为右下角，即45°
         */
        badgeCx = borderRect.centerX() + sin(45 * (Math.PI / 180)).toFloat() * borderRadius
        badgeCy = borderRect.centerY() + cos(45 * (Math.PI / 180)).toFloat() * borderRadius
        val maxBadgeWidth = (borderRect.right - badgeCx).coerceAtMost(borderRect.bottom - badgeCy)
        badgeRadius = if (badgeWidth <= 0 || badgeWidth > maxBadgeWidth) maxBadgeWidth else badgeWidth.toFloat()
    }

    private fun updateShaderMatrix() {
        bitmap?.let {
            val scale: Float
            var dx = 0f
            var dy = 0f

            shaderMatrix.set(null)

            val bitmapHeight: Float = it.height.toFloat()
            val bitmapWidth: Float = it.width.toFloat()

            if (bitmapWidth * drawableRect.height() > drawableRect.width() * bitmapHeight) {
                scale = drawableRect.height() / bitmapHeight
                dx = (drawableRect.width() - bitmapWidth * scale) * 0.5f
            } else {
                scale = drawableRect.width() / bitmapWidth
                dy = (drawableRect.height() - bitmapHeight * scale) * 0.5f
            }

            shaderMatrix.setScale(scale, scale)
            shaderMatrix.postTranslate(
                (dx + 0.5f).toInt() + drawableRect.left,
                (dy + 0.5f).toInt() + drawableRect.top
            )

            rebuildShader = true
        }
    }

    private fun calculateBounds(): RectF {
        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom

        val sideLength = availableWidth.coerceAtMost(availableHeight)

        val left = paddingLeft + (availableWidth - sideLength) / 2f
        val top = paddingTop + (availableHeight - sideLength) / 2f

        return RectF(left, top, left + sideLength, top + sideLength)
    }

    private fun initializeBitmap() {
        bitmap = getBitmapFromDrawable(drawable)

        bitmapCanvas = bitmap?.let {
            if (it.isMutable) Canvas(it) else null
        }

        if (!initialized) {
            return
        }

        if (bitmap == null) {
            bitmapPaint.shader = null
        } else {
            updateShaderMatrix()
        }
    }

    private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
        if (drawable == null) {
            return null
        }

        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        return try {
            val bitmap: Bitmap = if (drawable is ColorDrawable) {
                Bitmap.createBitmap(COLORDRAWABLE_DIMENSION, COLORDRAWABLE_DIMENSION, BITMAP_CONFIG)
            } else {
                Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    BITMAP_CONFIG
                )
            }

            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        initializeBitmap()
        invalidate()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        initializeBitmap()
        invalidate()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        initializeBitmap()
        invalidate()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        updateDimensions()
        invalidate()
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        super.setPaddingRelative(start, top, end, bottom)
        updateDimensions()
        invalidate()
    }

    override fun invalidateDrawable(dr: Drawable) {
        drawableDirty = true
        invalidate()
    }

    override fun setScaleType(scaleType: ScaleType?) {
        require(scaleType == SCALE_TYPE) { "ScaleType $scaleType not supported." }
    }

    override fun setAdjustViewBounds(adjustViewBounds: Boolean) {
        require(!adjustViewBounds) { "adjustViewBounds not supported." }
    }

    override fun setColorFilter(cf: ColorFilter?) {
        if (cf == imageColorFilter) {
            return
        }
        imageColorFilter = cf
        if (initialized) {
            bitmapPaint.colorFilter = cf
            invalidate()
        }
    }

    override fun getColorFilter(): ColorFilter? {
        return imageColorFilter
    }

    override fun setImageAlpha(alpha: Int) {
        val a = alpha and 0xFF
        if (a == imageAlphaWrapper) {
            return
        }

        imageAlphaWrapper = a

        // This might be called during ImageView construction before
        // member initialization has finished on API level >= 16.
        if (initialized) {
            bitmapPaint.alpha = a
            invalidate()
        }
    }

    override fun getImageAlpha(): Int {
        return imageAlphaWrapper
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return inTouchableArea(event?.x ?: 0f, event?.y ?: 0f) && super.onTouchEvent(event)
    }

    private fun inTouchableArea(x: Float, y: Float): Boolean {
        if (x <= 0f || y <= 0f || borderRect.isEmpty) {
            return true
        }

        return (x - borderRect.centerX()).pow(2) + (y - borderRect.centerY()).pow(2) <= borderRadius.pow(
            2
        )
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
        private val SCALE_TYPE = ScaleType.CENTER_CROP
        private const val DEFAULT_BORDER_WIDTH = 0
        private const val DEFAULT_BORDER_COLOR = Color.BLACK
        private const val DEFAULT_CIRCLE_BACKGROUND_COLOR = Color.TRANSPARENT
        private const val DEFAULT_IMAGE_ALPHA = 255
        private const val DEFAULT_BORDER_OVERLAY = false
        private const val COLORDRAWABLE_DIMENSION = 2
        private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888
        private const val DEFAULT_BADGE_COLOR = Color.TRANSPARENT
        private const val DEFAULT_BADGE_WIDTH = 0
    }
}
