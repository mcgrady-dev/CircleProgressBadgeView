package com.mcgrady.xwidgets

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

object DefaultAnimationOrchestrator {

    private const val DEFAULT_ROTATION_DURATION = 2000L
    private const val DEFAULT_EXPANSION_DURATION = 250L

    fun create(
        rotationDurationInMillis: Long = DEFAULT_ROTATION_DURATION,
        expandDurationInMillis: Long = DEFAULT_EXPANSION_DURATION
    ): CircularProgressAnimationOrchestrator {
        val expansionAnimator = createDefaultExpansionAnimator(expandDurationInMillis)
        val rotationAnimator = createDefaultRotationAnimator(rotationDurationInMillis)

        return CircularProgressAnimationOrchestrator(expansionAnimator, rotationAnimator)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun createDefaultExpansionAnimator(expandDurationInMillis: Long): CircularProgressAnimator {
        return object : CircularProgressAnimator {
            override val baseAnimator = ValueAnimator.ofFloat(
                CircularProgressImageView.AnimationDrawingState.MIN_VALUE,
                CircularProgressImageView.AnimationDrawingState.MAX_VALUE
            ).apply {
                interpolator = LinearInterpolator()
                duration = expandDurationInMillis
            }!!

            override fun onValueUpdate(animatorInterface: CircularProgressImageView.AnimatorInterface) {
                animatorInterface.updateAnimationState { state ->
                    val animatedValue = baseAnimator.animatedValue as Float
                    state.copy(archesExpansionProgress = animatedValue)
                }
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun createDefaultRotationAnimator(rotationDurationInMillis: Long): CircularProgressAnimator {
        return object : CircularProgressAnimator {
            override val baseAnimator: ValueAnimator = ValueAnimator.ofFloat(
                CircularProgressImageView.AnimationDrawingState.MIN_VALUE,
                CircularProgressImageView.AnimationDrawingState.MAX_VALUE
            ).apply {
                repeatCount = ValueAnimator.INFINITE
                duration = rotationDurationInMillis
                interpolator = LinearInterpolator()
            }!!

            override fun onValueUpdate(animatorInterface: CircularProgressImageView.AnimatorInterface) {
                animatorInterface.updateAnimationState { state ->
                    val animatedValue = baseAnimator.animatedValue as Float
                    state.copy(rotationProgress = animatedValue)
                }
            }
        }
    }
}