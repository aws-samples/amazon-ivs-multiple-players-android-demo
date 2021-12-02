package com.amazon.ivs.multiple.players.ui.models

import android.view.Surface
import android.view.View

data class PlayerUIModel(
    val index: Int,
    val container: View,
    val surface: Surface
)
