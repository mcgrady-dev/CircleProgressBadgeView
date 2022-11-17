package com.mcgrady.xwidgets

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.*


/**
 * Created by mcgrady on 2021/6/4.
 */
open class CircularProgressImageView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleInt: Int = 0
) : View(context, attributeSet, defStyleInt) {

    private val decFormat = DecimalFormat("#.##")

    //是否锁定
    var isBlock = false
        set(value) {
            field = value
            setup()
        }

    private val bitmapPaint = Paint()

    private val mRectF = RectF()

    //中心点
    private var mCenterPoint = Point()

    private val innerTextRect = Rect()

    private val innerTextPaint = TextPaint(ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 13f
        setup()
    }

    private var defaultSize: Int = 0
    private var mRadius = 0f

    var distanceToBorder = 25
        set(value) {
            field = if (value <= 0) 0 else value
            setup()
        }

    var borderThickness = 12
        set(value) {
            field = if (value <= 0) 0 else value
            setup()
        }

    var highlightedBorderThickness = 16
        set(value) {
            field = if (value <= 0) 0 else value
            setup()
        }

    private val boundsRect = RectF()
    private val boardMatrix = Matrix()
    private var boundsRadius = 0f


    /*********************** Inner Circle Start ***********************/

    /**
     * Text for which the initial(s) has to be displayed
     */
    var progressText: String? = null
        set(value) {
            field = value
            setup()
        }

    var courseTypeName: String? = null
        set(value) {
            field = value
            setup()
        }

    var courseName: String? = null
        set(value) {
            field = value
            setup()
        }

    private val innerInset
        get() = distanceToBorder + max(
            borderThickness.toFloat(),
            highlightedBorderThickness.toFloat()
        )

    /**
     * 圆形的背景画笔
     */
    private val innerBgPaint = Paint()

    /**
     * 内圆背景颜色
     */
    var innerBackgroundColor = Color.TRANSPARENT
        set(value) {
            field = value
            setup()
        }

    /**
     * 初始化图片的矩形区域
     */
    private val innerDrawableRect = RectF()

    private var innerDrawableRadius = 0f

    private var innerDrawable: Bitmap? = null

    /*********************** Inner Circle End ***********************/


    /*********************** Out Ring Start ***********************/

    private var circleAlpha = 0f

    /**
     * 外部圆环背景的画笔
     */
    private var outRingPaint = Paint()

    private var startAngle: Float = 29f
    private var sweepAngle: Float = -327f

    var outRingBgColor = Color.parseColor("#33000000")
        set(value) {
            field = value
            setup()
        }

    var outRingWidth = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        15f,
        resources.displayMetrics
    )
        set(value) {
            field = value
            setup()
        }

    private var progressPaint = Paint()

    //配置颜色的渐变至
    private var progressGradientColors =
        intArrayOf(
            Color.parseColor("#ffb900"),
            Color.parseColor("#FFFF50"),
            Color.parseColor("#FFB900")
        )

    var progressMaxValue = 0f
        set(value) {
            field = value
            logWarningOnArcLengthIfNeeded()
            setup()
        }

    var progressCurValue = 0f
        set(value) {
            field = value
            logWarningOnArcLengthIfNeeded()
            setup()
        }

    var progressWidth = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        15f,
        resources.displayMetrics
    )
        set(value) {
            field = value
            setup()
        }

    var progressTextSize = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        15f,
        resources.displayMetrics
    )
        set(value) {
            field = value
            setup()
        }

    var progressColor = Color.parseColor("#66000000")
        set(value) {
            field = value
            setup()
        }

    private var percent = 0f
    var animTime: Int? = null

    private val animatorInterface = AnimatorInterface()

    private var animationDrawingState = AnimationDrawingState(0f, 0f)

    private val AnimationDrawingState.rotationInDegrees: Float
        get() {
            return coercedRotationProgress * 360
        }
    private var animator: ValueAnimator? = null
    private val scaleAnimator = ValueAnimator.ofFloat(1f, 0.9f, 1f).apply {
        addUpdateListener {

            this@CircularProgressImageView.scaleX = it.animatedValue as Float
            this@CircularProgressImageView.scaleY = it.animatedValue as Float
        }
    }
    private var animationOrchestrator: CircularProgressAnimationOrchestrator =
        DefaultAnimationOrchestrator.create().also {
            attachOrchestrator(it)
        }
        set(value) {
            if (field == value) return
            field.cancel()
            field = value
            attachOrchestrator(field)
        }

    private fun attachOrchestrator(animationOrchestrator: CircularProgressAnimationOrchestrator) {
        animationOrchestrator.attach(animatorInterface) {
            if (isReversedAnimating) {
                animationDrawingState = animationDrawingState.copy(
                    rotationProgress = 0f
                )
                isReversedAnimating = false
            }
        }
    }

    private var isReversedAnimating = false
    var isAnimating = false
        set(value) {
            if (value && !field) {
                if (isReversedAnimating) {
                    animationOrchestrator.reverse()
                }
                animationOrchestrator.start()
            } else if (!value && field) {
                isReversedAnimating = true
                animationOrchestrator.cancel()
                animationOrchestrator.reverse()
            }
            field = value
            setup()
        }

    /*********************** Out Ring End ***********************/


    /*********************** Badge Start ***********************/

    //右下角圆圈的alpha
    private var badgerAlpha = 0f

    var badgeDrawableId = -1
        set(value) {
            field = value
            setup()
        }

    var innerDrawableId = -1
        set(value) {
            field = value
            setup()
        }

    private var badgeShader: BitmapShader? = null
    private val badgeIconPaint = Paint()

    private val badgePaint = Paint()
//    private val badgeStrokePaint = Paint()

    /**
     * Flag to toggle visibility of the badge.
     */
    var showBadge = false
        set(value) {
            field = value
            setup()
        }

    /**
     * The color of the badge.
     */
    private var badgeColor = Color.parseColor("#66000000")
        set(value) {
            field = value
            setup()
        }

    /**
     * The color of the stroke of the badge.
     */
//    var badgeStrokeColor = Defaults.BADGE_STROKE_COLOR
//        set(value) {
//            field = value
//            setup()
//        }

    /**
     * The stroke width (in pixels) of the badge.
     */
    var badgeStrokeWidth = 0
        set(value) {
            field = if (value <= 0) 0 else value
            setup()
        }

    //右下角圆圈的半径
    var badgeRadius = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        15f,
        resources.displayMetrics
    )
        set(value) {
            field = value
            setup()
        }

    /**
     * 进度文字的画笔
     */
//    private val badgeTextPaint = Paint(ANTI_ALIAS_FLAG).apply {
//        color = Color.WHITE
//        textSize = 20f
//        setup()
//    }

    private var badgeDrawable: Bitmap? = null

    /*********************** Badge End ***********************/

    var shouldBounceOnClick = true

    private var isReadingAttributes = false

    var isHighlighted = false
        set(value) {
            field = value
            setup()
        }

    init {
        attributeSet?.let { readAttrs(it, defStyleInt) }

        decFormat.roundingMode = RoundingMode.CEILING
        outlineProvider = OutlineProvider()
        defaultSize = Utils.dp2px(context, 150f)
        animator = ValueAnimator()
        setValue(progressCurValue)
        setup()
    }

    private fun readAttrs(attrs: AttributeSet, defStyle: Int = 0) {
        isReadingAttributes = true
        val a = context.obtainStyledAttributes(attrs, R.styleable.CourseLessonView, defStyle, 0)

        initInnerAttrs(a)

        initOutRingAttrs(a)

        initBadgeAttrs(a)

        progressColor = a.getColor(R.styleable.CourseLessonView_cv_progress_color, Color.BLACK)
        progressTextSize = a.getDimension(R.styleable.CourseLessonView_cv_progress_size, 8f)

        circleAlpha = a.getFloat(R.styleable.CourseLessonView_cv_circle_alpha, 0f)
        animTime = a.getInt(R.styleable.CourseLessonView_cv_progress_anim_time, 1000)

        a.recycle()
        isReadingAttributes = false
    }

    private fun initInnerAttrs(a: TypedArray) {
        innerBackgroundColor = a.getColor(
            R.styleable.CourseLessonView_cv_circle_background_color,
            Color.TRANSPARENT
        )

        progressText = a.getString(R.styleable.CourseLessonView_cv_progress_text)
        courseTypeName = a.getString(R.styleable.CourseLessonView_cv_course_type_name)
        courseName = a.getString(R.styleable.CourseLessonView_cv_course_name)
        innerDrawableId = a.getResourceId(R.styleable.CourseLessonView_cv_inner_resource, -1)

        circleBackgroundColor = innerBackgroundColor
    }

    private fun initOutRingAttrs(a: TypedArray) {
        borderThickness = a.getDimensionPixelSize(
            R.styleable.CourseLessonView_cv_border_thickness,
            12
        )

        distanceToBorder = a.getDimensionPixelSize(
            R.styleable.CourseLessonView_cv_distance_to_border,
            25
        )

        highlightedBorderThickness = a.getDimensionPixelSize(
            R.styleable.CourseLessonView_cv_border_thickness_highlight,
            16
        )

        isHighlighted =
            a.getBoolean(R.styleable.CourseLessonView_cv_highlighted, false)

        outRingBgColor =
            a.getColor(R.styleable.CourseLessonView_cv_outring_bg_color, Color.parseColor("#33000000"))
        outRingWidth = a.getDimension(
            R.styleable.CourseLessonView_cv_outring_width,
            15f
        )

        progressWidth = a.getDimension(
            R.styleable.CourseLessonView_cv_progress_width,
            15f
        )

        progressCurValue =
            a.getFloat(
                R.styleable.CourseLessonView_cv_progress_cur_value,
                0f
            )

        progressMaxValue = a.getFloat(
            R.styleable.CourseLessonView_cv_progress_max_value,
            0f
        ) //100

        startAngle = a.getFloat(
            R.styleable.CourseLessonView_cv_progress_start_angle,
            29f
        )
        sweepAngle = a.getFloat(
            R.styleable.CourseLessonView_cv_sweep_angle,
            -327f
        )

        val progressGradientColors: Int =
            a.getResourceId(R.styleable.CourseLessonView_cv_progress_colors, 0) //圆弧颜色

        if (progressGradientColors != 0) {
            try {
                val gradientColors = resources.getIntArray(progressGradientColors)

                if (gradientColors.isEmpty()) { //如果渐变色为数组为0，则尝试以单色读取色值
                    val color = ContextCompat.getColor(context, progressGradientColors)
                    this.progressGradientColors = IntArray(2)
                    this.progressGradientColors[0] = color
                    this.progressGradientColors[1] = color
                } else if (gradientColors.size == 1) { //如果渐变数组只有一种颜色，默认设为两种相同颜色
                    this.progressGradientColors = IntArray(2)
                    this.progressGradientColors[0] = gradientColors[0]
                    this.progressGradientColors[1] = gradientColors[0]
                } else {
                    this.progressGradientColors = gradientColors
                }
            } catch (e: Resources.NotFoundException) {
                throw Resources.NotFoundException("the give resource not found.")
            }
        }
    }

    private fun initBadgeAttrs(a: TypedArray) {
        showBadge = a.getBoolean(R.styleable.CourseLessonView_cv_show_badge, false)
        badgeColor = a.getColor(R.styleable.CourseLessonView_cv_badge_color, Color.parseColor("#66000000"))
//        badgeStrokeColor =
//            a.getColor(R.styleable.CourseView_cv_badge_stroke_color, Defaults.BADGE_STROKE_COLOR)
        badgeStrokeWidth = a.getDimensionPixelSize(
            R.styleable.CourseLessonView_cv_badge_stroke_width,
            badgeStrokeWidth
        )
        badgeRadius = a.getDimension(R.styleable.CourseLessonView_cv_badge_radius, badgeRadius)

        badgerAlpha = a.getFloat(R.styleable.CourseLessonView_cv_badger_alpha, 0f)
        badgeDrawableId = a.getResourceId(R.styleable.CourseLessonView_cv_badge_resource, -1)
    }

    private fun setup() {
        if (isReadingAttributes) {
            return
        }
        if (width == 0 && height == 0) {
            return
        }

        bitmapPaint.isAntiAlias = true

        badgeDrawable = if (badgeDrawableId == -1) null else BitmapFactory.decodeResource(resources, badgeDrawableId)
//        if (badgeDrawable != null) {
//            badgeShader = BitmapShader(badgeDrawable!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
//            badgeIconPaint.shader = badgeShader
//        }
        val currentBorderThickness = (if (isHighlighted) highlightedBorderThickness else borderThickness).toFloat()

        boundsRect.set(calculateBounds())
        boundsRadius = min(
            (boundsRect.height() - currentBorderThickness) / 2.0f,
            (boundsRect.width() - currentBorderThickness) / 2.0f
        )

        setProgressPaint()

        setBorderBgPaint()

        innerDrawable = BitmapFactory.decodeResource(resources, innerDrawableId)

        innerDrawableRect.set(boundsRect)
        innerDrawableRect.inset(innerInset, innerInset)

        innerDrawableRadius =
            (innerDrawableRect.height() / 2.0f).coerceAtMost(innerDrawableRect.width() / 2.0f)

        mCenterPoint.x = boundsRect.centerX().toInt()
        mCenterPoint.y = boundsRect.centerY().toInt()

        //圆环背景
        innerBgPaint.apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = innerBackgroundColor
        }
        circleBackgroundPaint.apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = circleBackgroundColor
        }

        //右小角的圈圈
        badgePaint.apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = badgeColor
        }

        updateDimensions()

        invalidate()
    }

    private fun setBorderBgPaint() {
        outRingPaint.apply {
            isAntiAlias = true
            color = outRingBgColor
            style = Paint.Style.STROKE
            strokeWidth = outRingWidth
            strokeCap = Paint.Cap.ROUND
        }
    }

    private fun setProgressPaint() {
        progressPaint.apply {
            isAntiAlias = true
            style = Paint.Style.STROKE //画圆环必有,不然是扇弧
            strokeWidth = progressWidth
            strokeCap = Paint.Cap.ROUND //作用于圆环结尾
            isAntiAlias = true
            textSize = progressTextSize
            color = progressColor
            // 设置Typeface对象，即字体风格，包括粗体，斜体以及衬线体，非衬线体等
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return inTouchableArea(event.x, event.y) && super.onTouchEvent(event)
    }

    private fun animateClick() {
        if (shouldBounceOnClick) scaleAnimator.start()
    }

    override fun performClick(): Boolean {
        animateClick()
        return super.performClick()
    }

    override fun performLongClick(): Boolean {
        animateClick()
        return super.performLongClick()
    }

    private fun inTouchableArea(x: Float, y: Float): Boolean {
        return (x - boundsRect.centerX().toDouble()).pow(2.0) + (y - boundsRect.centerY().toDouble()).pow(2.0) <= boundsRadius.toDouble().pow(2.0)
    }


    private fun drawBadge(canvas: Canvas, layerId: Int) {
        if (!showBadge || badgeColor == Color.TRANSPARENT) {
            return
        }

        val radius = badgeRadius.plus(badgeStrokeWidth)

        //X坐标= a + Math.sin(角度 * (Math.PI / 180)) * r
        //Y坐标= b + Math.cos(角数 * (Math.PI / 180)) * r

        // 默认为右下角，即45°
        val badgeCx = mCenterPoint.x + sin(45 * (Math.PI / 180)).toFloat() * mRadius
        val badgeCy = mCenterPoint.y + cos(45 * (Math.PI / 180)).toFloat() * mRadius

        bitmapPaint.color = Color.WHITE
        bitmapPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        canvas.drawCircle(badgeCx, badgeCy, radius, bitmapPaint)
        canvas.restoreToCount(layerId)

        bitmapPaint.xfermode = null
        canvas.drawCircle(badgeCx, badgeCy, badgeRadius, badgePaint)

        if (badgeDrawable != null) {
            canvas.drawBitmap(
                badgeDrawable!!,
                badgeCx - badgeRadius,
                badgeCy - badgeRadius,
                badgeIconPaint
            )
        }
    }

    private val borderRect = RectF()
    private var borderRadius = 0f
    private var borderWidth = 15f

    private val drawableRect = RectF()
    private var drawableBorderWidth = 15f
    private var drawableRadius = 0f

    private var circleBackgroundColor = Color.TRANSPARENT
    private val circleBackgroundPaint = Paint()

    private fun updateDimensions() {
        borderRect.set(calculateBounds())
        borderRadius = ((borderRect.height() - outRingWidth) / 2.0f).coerceAtMost((borderRect.width() - outRingWidth) / 2.0f)

        drawableRect.set(borderRect)
        drawableRect.inset(outRingWidth + drawableBorderWidth, outRingWidth + drawableBorderWidth)
        drawableRadius = (drawableRect.height()/2f).coerceAtLeast((drawableRect.width()/2f))
    }

    override fun onDraw(canvas: Canvas) {

//        drawInner(canvas)

        if (circleBackgroundColor != Color.TRANSPARENT) {
            canvas.drawCircle(drawableRect.centerX(), drawableRect.centerY(), drawableRadius, circleBackgroundPaint)
        }

        canvas.drawCircle(mRectF.centerX(), mRectF.centerY(), borderRadius, outRingPaint)

//        drawOutRing(canvas)

//        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
//        drawBadge(canvas, layerId)
    }

//    private fun hasInnerCircleDrawable(): Boolean {
//        val drawable = this.drawable
//        val hasTransparentDrawable = drawable is ColorDrawable && drawable.alpha == 0
//        return !hasTransparentDrawable
//    }

    /**
     * 绘制内圆
     */
    private fun drawInner(canvas: Canvas) {

        //处理文字锯齿
        canvas.drawFilter = PaintFlagsDrawFilter(0, ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        if (innerBackgroundColor != Color.TRANSPARENT) {
            innerBgPaint.color =
                if (isBlock) Color.parseColor("#33000000") else innerBackgroundColor
            canvas.drawCircle(
                innerDrawableRect.centerX(),
                innerDrawableRect.centerY(),
                innerDrawableRadius,
                innerBgPaint
            )
        }

        //PROGRESS TEXT
        if (!TextUtils.isEmpty(progressText)) {
            innerTextPaint.let {
                it.reset()
                it.color = if (isBlock) Color.parseColor("#80FFFFFF") else Color.BLACK
                it.textSize = Utils.sp2px(context, 8f).toFloat()
                it.getTextBounds(progressText, 0, progressText!!.length, innerTextRect)
            }
            val progressTextPaddingTop = 30f

            canvas.drawText(
                progressText!!,
                innerDrawableRect.centerX().minus(innerTextRect.width().div(2f)),
                innerDrawableRect.top.plus(innerTextRect.height()).plus(progressTextPaddingTop),
                innerTextPaint
            )
        }

        //COURSE NAME
        var courseNameHeight = 0
        var courseNameWidth = 0F
        if (!TextUtils.isEmpty(courseName)) {
            innerTextPaint.let {
                it.reset()
                it.color = if (isBlock) Color.parseColor("#80FFFFFF") else Color.BLACK
                it.textSize = Utils.sp2px(context, 12f).toFloat()
                it.getTextBounds(courseName, 0, courseName!!.length, innerTextRect)
                courseNameWidth = it.measureText(courseName, 0, courseName!!.length)
            }

            courseNameHeight = innerTextRect.height()

            val layout =
                StaticLayout(
                    courseName,
                    innerTextPaint,
                    innerDrawableRect.width().toInt(),
                    Layout.Alignment.ALIGN_NORMAL,
                    1.0f,
                    0.0f,
                    true
                )
            canvas.save()
            canvas.translate(
                innerDrawableRect.centerX().minus(layout.getLineWidth(0).div(2f)),
                innerDrawableRect.centerY() - (layout.getLineBottom(0) - layout.getLineTop(0)) / 2
            )
            layout.draw(canvas)
            canvas.restore()
        }

        //COURSE TYPE NAME
        if (!TextUtils.isEmpty(courseTypeName) && courseNameHeight != 0) {
            innerTextPaint.let {
                it.reset()
                it.color =
                    if (isBlock) Color.parseColor("#80FFFFFF") else Color.BLACK
                it.textSize = Utils.sp2px(context, 9f).toFloat()
                it.getTextBounds(courseTypeName, 0, courseTypeName!!.length, innerTextRect)
            }

            canvas.drawText(
                courseTypeName!!,
                innerDrawableRect.centerX().minus(innerTextRect.width().div(2f)),
                innerDrawableRect.centerY().minus(courseNameHeight.div(2))
                    .minus(innerTextRect.height()),
                innerTextPaint
            )
        }

        innerDrawable?.let {
            //FIXME 图片资源未适配情况下，会按资源大小进行绘制 by mcgrady
            canvas.drawBitmap(
                it,
                innerDrawableRect.centerX().minus(it.width.div(2f))
                    .plus(Utils.dp2px(context, 2f)),
                innerDrawableRect.bottom.minus(it.height)
                    .minus(Utils.dp2px(context, 15f)),
                badgeIconPaint
            )
        }
    }

    /**
     * 绘制外环和进度条
     * 绘制背景圆弧， 圆弧给负数就是逆时针，给正数就是顺时针
     */
    private fun drawOutRing(canvas: Canvas) {
        canvas.save()

        canvas.rotate(
            startAngle,
            mCenterPoint.x.toFloat(),
            mCenterPoint.y.toFloat()
        )
        canvas.drawArc(mRectF, 0f, sweepAngle, false, outRingPaint)

        if (percent.isNaN() || percent <= 0f) {
            canvas.restore()
            return
        }

        var currentAngle: Float = sweepAngle * percent

        canvas.drawArc(mRectF, 0f, currentAngle, false, progressPaint)

        canvas.restore()
    }

    fun setProgress(current: Float, max: Float) {
        if (current == progressCurValue && max == progressMaxValue)
            return

        percent = decFormat.format(current.div(max)).toFloat()
        progressCurValue = current
        progressMaxValue = max
    }

    private fun startAnimator(start: Float, end: Float, animTime: Int?) {
        animator = ValueAnimator.ofFloat(start, end)//获取%区间 当前进度，[0.0f,1.0f]
        animator?.apply {
            duration = animTime?.toLong() ?: 0
            addUpdateListener { animation ->
                percent = animation?.animatedValue as Float
                progressCurValue = percent * progressMaxValue
                Log.d("CPIV", "onAnimationUpdate: percent = $percent progressCurrentValue = $progressCurValue progressMaxValue = $progressMaxValue")
                invalidate()
            }
            start()
        }
    }

    private fun updateArchesPaint() {
        // 设置渐变
        val mSweepGradient =
            SweepGradient(
                mCenterPoint.x.toFloat(),
                mCenterPoint.y.toFloat(),
                progressGradientColors,
                null
            )
        progressPaint.shader = mSweepGradient
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(
            Utils.measure(widthMeasureSpec, defaultSize),
            Utils.measure(heightMeasureSpec, defaultSize)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val maxArcWidth: Float = progressWidth.coerceAtLeast(outRingWidth)
        //求最小值作为实际值
        val minSize = (w - paddingLeft - paddingRight - 2 * maxArcWidth.toInt())
            .coerceAtMost(h - paddingTop - paddingBottom - 2 * maxArcWidth.toInt())

        //获取圆心,圆半径
        mRadius = minSize.div(2).toFloat()
        //获取圆的相关参数
        mCenterPoint.x = w / 2
        mCenterPoint.y = h / 2

        //绘制圆弧的边界(画圆弧(或圆环)先要画矩形)
        mRectF.left = (mCenterPoint.x - mRadius - maxArcWidth / 2)
        mRectF.top = mCenterPoint.y - mRadius - maxArcWidth / 2
        mRectF.right = mCenterPoint.x + mRadius + maxArcWidth / 2
        mRectF.bottom = mCenterPoint.y + mRadius + maxArcWidth / 2
        //计算文字绘制时的 baseline
        //由于文字的baseline、descent、ascent等属性只与textSize和typeface有关，所以此时可以直接计算
        //若value、hint、unit由同一个画笔绘制或者需要动态设置文字的大小，则需要在每次更新后再次计算
//        valueOffset = (mCenterPoint.y + getBaselineOffsetFromY(mValuePaint)).toInt() //value处在中间
        updateArchesPaint()
        setup()
    }

    /**
     * 用于点击设置随机值,用于进度显示
     */
    fun setValue(value: Float) {
        var progressValue = value

        if (progressValue > progressMaxValue) {
            progressValue = progressMaxValue
        }

        if (progressValue == progressCurValue) {
            return
        }

        progressCurValue = progressValue

//        var value = value
//        if (value > progressMaxValue) {
//            value = progressMaxValue
//        }

//        val builder = StringBuilder()
//        progressText = builder
//            .append(progressCurValue.toInt())
//            .append("/")
//            .append(progressMaxValue.toInt())
//            .toString()

        val start = percent
        val end = progressValue / progressMaxValue
        startAnimator(start, end, animTime)
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        setup()
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        super.setPaddingRelative(start, top, end, bottom)
        setup()
    }

    private inner class OutlineProvider : ViewOutlineProvider() {

        override fun getOutline(view: View, outline: Outline) = Rect().let {
            boundsRect.roundOut(it)
            outline.setRoundRect(it, it.width() / 2.0f)
        }
    }

    data class AnimationDrawingState(
        private val archesExpansionProgress: Float,
        private val rotationProgress: Float,
    ) {
        private var coercedArchesExpansionProgress: Float = 0F
        val coercedRotationProgress: Float

        init {
            this.coercedArchesExpansionProgress =
                archesExpansionProgress.coerceIn(MIN_VALUE, MAX_VALUE)
            this.coercedRotationProgress = rotationProgress.coerceIn(MIN_VALUE, MAX_VALUE)
        }

        companion object {
            const val MAX_VALUE = 1f
            const val MIN_VALUE = 0f
        }
    }

    inner class AnimatorInterface {
        private val currentAnimationState: AnimationDrawingState
            get() = animationDrawingState

        fun updateAnimationState(update: (currentState: AnimationDrawingState) -> AnimationDrawingState) {
            animationDrawingState = update(currentAnimationState)
            invalidate()
        }
    }


}

private fun logWarningOnArcLengthIfNeeded() {

}