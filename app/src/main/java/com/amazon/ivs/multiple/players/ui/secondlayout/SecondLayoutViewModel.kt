package com.amazon.ivs.multiple.players.ui.secondlayout

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.amazon.ivs.multiple.players.common.init
import com.amazon.ivs.multiple.players.common.zoomToFit
import com.amazon.ivs.multiple.players.ui.models.Error
import com.amazon.ivs.multiple.players.ui.models.PlayerModel
import com.amazon.ivs.multiple.players.ui.models.PlayerUIModel
import com.amazon.ivs.multiple.players.ui.models.SecondLayoutStream
import com.amazon.ivs.multiple.players.ui.models.VideoBufferingState
import com.amazonaws.ivs.player.MediaPlayer
import com.amazonaws.ivs.player.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SecondLayoutViewModel @Inject constructor() : ViewModel() {
    private var playerSet = mutableListOf<PlayerModel>()
    private var bufferingStates = mutableListOf<VideoBufferingState>()
    private val _onSizeChanged = Channel<Int>()
    private val _onBuffering = Channel<List<VideoBufferingState>>()
    private val _onPlaying = MutableStateFlow(false)
    private val _onError = Channel<Error>()

    val onSizeChanged = _onSizeChanged.receiveAsFlow()
    val onBuffering = _onBuffering.receiveAsFlow()
    val onError = _onError.receiveAsFlow()
    val onPlaying = _onPlaying.asStateFlow()
    val isPlaying get() = _onPlaying.replayCache.lastOrNull() ?: false

    fun initPlayers(context: Context, playerViews: List<PlayerUIModel>) {
        if (playerSet.isNotEmpty()) return
        SecondLayoutStream.entries.forEachIndexed { index, streamModel ->
            updateBufferingState(index, true)
            val player = MediaPlayer.Builder(context).build()
            val listener = player.init(
                index,
                { videoSizeState ->
                    playerSet.find { it.index == videoSizeState.index }?.let { player ->
                        if (player.width != videoSizeState.width || player.height != videoSizeState.height) {
                            player.width = videoSizeState.width
                            player.height = videoSizeState.height
                            _onSizeChanged.trySend(player.index)
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
                            _onPlaying.update { true }
                        }
                        else -> { /* Ignored */ }
                    }

                    if (playerSet.all { playerModel -> playerModel.player.state != Player.State.PLAYING }) {
                        _onPlaying.update { false }
                    }
                },
                { exception ->
                    _onError.trySend(exception)
                }
            )

            player.setSurface(playerViews.getOrNull(index)?.surface)
            player.load(streamModel.uri.toUri())
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
        _onBuffering.trySend(bufferingStates.map { it.copy() })
    }
}
