package com.amazon.ivs.multiple.players.ui.secondlayout

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.amazon.ivs.multiple.players.common.ConsumableSharedFlow
import com.amazon.ivs.multiple.players.common.init
import com.amazon.ivs.multiple.players.common.zoomToFit
import com.amazon.ivs.multiple.players.ui.models.*
import com.amazonaws.ivs.player.MediaPlayer
import com.amazonaws.ivs.player.Player
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

class SecondLayoutViewModel : ViewModel() {

    private var playerSet = mutableListOf<PlayerModel>()
    private var bufferingStates = mutableListOf<VideoBufferingState>()
    private val _onSizeChanged = ConsumableSharedFlow<Int>(canReplay = true)
    private val _onBuffering = ConsumableSharedFlow<List<VideoBufferingState>>()
    private val _onError = ConsumableSharedFlow<Error>()
    private val _onPlaying = ConsumableSharedFlow<Boolean>()

    val onSizeChanged = _onSizeChanged.asSharedFlow()
    val onBuffering = _onBuffering.asSharedFlow()
    val onError = _onError.asSharedFlow()
    val onPlaying = _onPlaying.asSharedFlow()
    val isPlaying get() = _onPlaying.replayCache.lastOrNull() ?: false

    fun initPlayers(context: Context, playerViews: List<PlayerUIModel>) {
        if (playerSet.isNotEmpty()) return
        SecondLayoutStream.values().forEachIndexed { index, streamModel ->
            updateBufferingState(index, true)
            val player = MediaPlayer(context)
            val listener = player.init(
                index,
                { videoSizeState ->
                    playerSet.find { it.index == videoSizeState.index }?.let { player ->
                        if (player.width != videoSizeState.width || player.height != videoSizeState.height) {
                            player.width = videoSizeState.width
                            player.height = videoSizeState.height
                            _onSizeChanged.tryEmit(player.index)
                        }
                    }
                },
                { state, playerIndex ->
                    when (state) {
                        Player.State.BUFFERING -> {
                            updateBufferingState(playerIndex, true)
                        }
                        Player.State.READY -> {
                            player.qualities.firstOrNull { it.name == streamModel.maxQuality }?.let { quality ->
                                player.setAutoMaxQuality(quality)
                            }
                        }
                        Player.State.PLAYING -> {
                            updateBufferingState(playerIndex, false)
                            _onPlaying.tryEmit(true)
                        }
                        else -> { /* Ignored */ }
                    }

                    if (playerSet.all { playerModel -> playerModel.player.state != Player.State.PLAYING }) {
                        _onPlaying.tryEmit(false)
                    }
                },
                { exception ->
                    _onError.tryEmit(exception)
                }
            )

            player.setSurface(playerViews.getOrNull(index)?.surface)
            player.load(Uri.parse(streamModel.uri))
            player.play()
            playerSet.add(PlayerModel(index, player, listener))
        }
    }

    fun release() {
        Timber.d("Releasing player")
        playerSet.forEach { playerModel ->
            playerModel.player.removeListener(playerModel.listener)
            playerModel.player.release()
        }
        playerSet.clear()
    }

    fun pause() {
        playerSet.forEach { playerModel ->
            playerModel.player.pause()
        }
    }

    fun play() {
        playerSet.forEach { playerModel ->
            playerModel.player.play()
        }
    }

    fun updatePlayerViews(playerViews: List<PlayerUIModel>) {
        playerViews.forEach { playerView ->
            updatePlayerView(playerView)
        }
    }

    fun updatePlayerView(playerView: PlayerUIModel) {
        playerSet.getOrNull(playerView.index)?.let { playerModel ->
            Timber.d("Updating player view: $playerModel")
            playerView.container.zoomToFit(playerModel.width, playerModel.height)
        }
    }

    private fun updateBufferingState(playerIndex: Int, isBuffering: Boolean) {
        Timber.d("Updating buffering state: $playerIndex, $isBuffering")
        bufferingStates.find { it.playerId == playerIndex }?.let { state ->
            state.buffering = isBuffering
        } ?: run {
            bufferingStates.add(VideoBufferingState(playerIndex, isBuffering))
        }
        _onBuffering.tryEmit(bufferingStates.map { it.copy() })
    }
}
