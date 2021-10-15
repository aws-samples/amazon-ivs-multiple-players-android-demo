package com.amazon.ivs.multiple.players.common

import com.amazon.ivs.multiple.players.ui.models.Error
import com.amazon.ivs.multiple.players.ui.models.VideoSizeState
import com.amazonaws.ivs.player.Cue
import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.PlayerException
import com.amazonaws.ivs.player.Quality
import java.nio.ByteBuffer

inline fun Player.setListener(
    crossinline onAnalyticsEvent: (key: String, value: String) -> Unit = { _, _ -> },
    crossinline onRebuffering: () -> Unit = {},
    crossinline onSeekCompleted: (value: Long) -> Unit = { _ -> },
    crossinline onQualityChanged: (quality: Quality) -> Unit = { _ -> },
    crossinline onVideoSizeChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
    crossinline onCue: (cue: Cue) -> Unit = { _ -> },
    crossinline onDurationChanged: (duration: Long) -> Unit = { _ -> },
    crossinline onStateChanged: (state: Player.State) -> Unit = { _ -> },
    crossinline onError: (exception: PlayerException) -> Unit = { _ -> },
    crossinline onMetadata: (data: String, buffer: ByteBuffer) -> Unit = { _, _ -> },
    crossinline onNetworkUnavailable: () -> Unit = {}
): Player.Listener {
    val listener = playerListener(
        onAnalyticsEvent, onRebuffering, onSeekCompleted, onQualityChanged, onVideoSizeChanged,
        onCue, onDurationChanged, onStateChanged, onError, onMetadata, onNetworkUnavailable
    )

    addListener(listener)
    return listener
}

fun Player.init(
    playerIndex: Int,
    onVideoSizeChanged: (playerParamsChanged: VideoSizeState) -> Unit,
    onStateChanged: (state: Player.State, playerIndex: Int) -> Unit,
    onError: (exception: Error) -> Unit
) = setListener(
    onVideoSizeChanged = { width, height ->
        onVideoSizeChanged(VideoSizeState(playerIndex, width >= height, width, height))
    },
    onStateChanged = { state ->
        onStateChanged(state, playerIndex)
    },
    onError = { exception ->
        if (exception.code != 0) {
            onError(Error(playerIndex, exception.code.toString(), exception.errorMessage))
        }
    }
)

inline fun playerListener(
    crossinline onAnalyticsEvent: (key: String, value: String) -> Unit = { _, _ -> },
    crossinline onRebuffering: () -> Unit = {},
    crossinline onSeekCompleted: (value: Long) -> Unit = { _ -> },
    crossinline onQualityChanged: (quality: Quality) -> Unit = { _ -> },
    crossinline onVideoSizeChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
    crossinline onCue: (cue: Cue) -> Unit = { _ -> },
    crossinline onDurationChanged: (duration: Long) -> Unit = { _ -> },
    crossinline onStateChanged: (state: Player.State) -> Unit = { _ -> },
    crossinline onError: (exception: PlayerException) -> Unit = { _ -> },
    crossinline onMetadata: (data: String, buffer: ByteBuffer) -> Unit = { _, _ -> },
    crossinline onNetworkUnavailable: () -> Unit = {}
): Player.Listener = object : Player.Listener() {
    override fun onAnalyticsEvent(key: String, value: String) = onAnalyticsEvent(key, value)
    override fun onRebuffering() = onRebuffering()
    override fun onSeekCompleted(value: Long) = onSeekCompleted(value)
    override fun onQualityChanged(quality: Quality) = onQualityChanged(quality)
    override fun onVideoSizeChanged(width: Int, height: Int) = onVideoSizeChanged(width, height)
    override fun onCue(cue: Cue) = onCue(cue)
    override fun onDurationChanged(duration: Long) = onDurationChanged(duration)
    override fun onStateChanged(state: Player.State) = onStateChanged(state)
    override fun onError(exception: PlayerException) = onError(exception)
    override fun onMetadata(data: String, buffer: ByteBuffer) = onMetadata(data, buffer)
    override fun onNetworkUnavailable() = onNetworkUnavailable()
}
