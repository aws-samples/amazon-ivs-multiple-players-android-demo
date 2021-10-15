package com.amazon.ivs.multiple.players.common

import android.content.res.Resources

const val WIDE_SCREEN_PROPORTION = 0.5625f
const val MIN_CONTAINER_SIZE = 10f
const val MEASURE_REPEAT_DELAY = 100L
const val SURFACE_FADE_OUT_DELAY = 100L
val MEASURE_REPEAT_COUNT = (0..3).count()

val Int.dp: Float get() = (this * Resources.getSystem().displayMetrics.density + 0.5f)
