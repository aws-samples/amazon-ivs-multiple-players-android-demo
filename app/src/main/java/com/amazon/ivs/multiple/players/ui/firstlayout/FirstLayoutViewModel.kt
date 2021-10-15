package com.amazon.ivs.multiple.players.ui.firstlayout

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.amazon.ivs.multiple.players.common.ConsumableLiveData
import com.amazon.ivs.multiple.players.common.init
import com.amazon.ivs.multiple.players.common.zoomToFit
import com.amazon.ivs.multiple.players.ui.models.*
import com.amazonaws.ivs.player.MediaPlayer
import com.amazonaws.ivs.player.Player
import timber.log.Timber

class FirstLayoutViewModel : ViewModel() {

    private var playerSet = mutableListOf<PlayerModel>()
    private var bufferingStates = mutableListOf<VideoBufferingState>()
    val onSizeChanged = ConsumableLiveData<Int>()
    val onBuffering = ConsumableLiveData<List<VideoBufferingState>>()
    val onError = ConsumableLiveData<Error>()
    val isPlaying = ConsumableLiveData<Boolean>()

    fun initPlayers(context: Context, playerViews: List<PlayerViewModel>) {
        if (playerSet.isNotEmpty()) return
        FirstLayoutStream.values().forEachIndexed { index, streamModel ->
            updateBufferingState(index, true)
            val player = MediaPlayer(context)
            val listener = player.init(
                index,
                { videoSizeState ->
                    playerSet.find { it.index == videoSizeState.index }?.let { player ->
                        if (player.width != videoSizeState.width || player.height != videoSizeState.height) {
                            player.width = videoSizeState.width
                            player.height = videoSizeState.height
                            onSizeChanged.postConsumable(player.index)
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
                            isPlaying.postConsumable(true)
                        }
                        else -> { /* Ignored */ }
                    }

                    if (playerSet.all { playerModel -> playerModel.player.state != Player.State.PLAYING }) {
                        isPlaying.postConsumable(false)
                    }
                },
                { exception ->
                    onError.postConsumable(exception)
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
            playerModel.player.setSurface(null)
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

    fun updatePlayerViews(playerViews: List<PlayerViewModel>) {
        playerViews.forEach { playerView ->
            updatePlayerView(playerView)
        }
    }

    fun updatePlayerView(playerView: PlayerViewModel) {
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
        onBuffering.postConsumable(bufferingStates.map { it.copy() })
    }
}
