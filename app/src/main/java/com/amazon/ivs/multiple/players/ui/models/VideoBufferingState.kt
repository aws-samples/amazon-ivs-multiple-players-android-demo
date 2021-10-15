package com.amazon.ivs.multiple.players.ui.models

data class VideoBufferingState(
    val playerId: Int,
    var buffering: Boolean
)
