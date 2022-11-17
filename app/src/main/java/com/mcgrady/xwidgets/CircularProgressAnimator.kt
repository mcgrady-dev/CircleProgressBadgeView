package com.mcgrady.xwidgets

import android.animation.ValueAnimator

interface CircularProgressAnimator {
    val baseAnimator: ValueAnimator

    fun onValueUpdate(animatorInterface: CircularProgressImageView.AnimatorInterface)
}