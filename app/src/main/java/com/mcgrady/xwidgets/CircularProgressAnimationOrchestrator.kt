package com.mcgrady.xwidgets

import android.animation.AnimatorSet

/**
 * @param setupAnimators These animators will run before the progressAnimators.
 * When stopping the animations, these animators will be played in reverse, to animate back to the original state.
 * It's really useful to expand/collapse the arches, manipulating the [CircularProgressImageView.AnimationDrawingState.archesExpansionProgress] parameter during animation
 * @param progressAnimators These will e run after the setup animators.
 * They will not be called in reverse, as they expected to repeat infinitely.
 */
class CircularProgressAnimationOrchestrator(
    private val setupAnimators: List<CircularProgressAnimator> = listOf(),
    private val progressAnimators: List<CircularProgressAnimator> = listOf(),
) {

    constructor(setupAnimator: CircularProgressAnimator, progressAnimator: CircularProgressAnimator) : this(
        listOf(setupAnimator),
        listOf(progressAnimator)
    )

    private val setupSet = AnimatorSet().apply {
        val baseAnimators = setupAnimators.map { it.baseAnimator }
        if (baseAnimators.isNotEmpty()) {
            playTogether(*baseAnimators.toTypedArray())
        }
    }

    /**
     * These animators will be called
     */
    private val progressSet = AnimatorSet().apply {
        val baseAnimators = progressAnimators.map { it.baseAnimator }
        if (baseAnimators.isNotEmpty()) {
            playTogether(*baseAnimators.toTypedArray())
        }
    }

    private val animatorSet = AnimatorSet().apply {
        playSequentially(setupSet, progressSet)
    }

    internal fun cancel() {
        animatorSet.cancel()
    }

    internal fun start() {
        animatorSet.start()
    }

    internal fun reverse() {
        setupAnimators.forEach { animator ->
            animator.baseAnimator.reverse()
        }
    }

    internal fun attach(
        animatorInterface: CircularProgressImageView.AnimatorInterface,
        onSetupEnd: () -> Unit = {}
    ) {
        (setupAnimators + progressAnimators).forEach { animator ->
            animator.baseAnimator.addUpdateListener {
                animator.onValueUpdate(animatorInterface)
            }
        }
        setupSet.addOnAnimationEndListener {
            onSetupEnd()
        }
    }
}