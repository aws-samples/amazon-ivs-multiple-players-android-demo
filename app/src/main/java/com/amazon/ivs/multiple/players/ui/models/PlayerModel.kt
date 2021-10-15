package com.amazon.ivs.multiple.players.ui.models

import com.amazonaws.ivs.player.Player

data class PlayerModel(
    val index: Int,
    val player: Player,
    val listener: Player.Listener,
    var width: Int = 0,
    var height: Int = 0
)
