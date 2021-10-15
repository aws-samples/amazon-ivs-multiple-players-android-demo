package com.amazon.ivs.multiple.players.ui.models

data class Error(
    val playerIndex: Int,
    val errorCode: String,
    val errorMessage: String
)
