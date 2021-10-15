package com.amazon.ivs.multiple.players.common

import android.app.Activity
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.navigation.findNavController
import com.amazon.ivs.multiple.players.R
import com.amazon.ivs.multiple.players.ui.models.PlayerViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber

fun Activity.openFragment(id: Int) {
    findNavController(R.id.navigation_host_fragment).run {
        popBackStack()
        navigate(id)
    }
}

fun View.showSnackBar(message: String) {
    val snackBar = Snackbar.make(this, message, Snackbar.LENGTH_INDEFINITE)
    snackBar.view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_bg_color))
    snackBar.setActionTextColor(ContextCompat.getColor(context, R.color.snackbar_action_color))
    snackBar.setTextColor(ContextCompat.getColor(context, R.color.snackbar_action_color))
    snackBar.duration = Snackbar.LENGTH_LONG
    snackBar.setAction(context.getString(R.string.close)) {
        snackBar.dismiss()
    }
    snackBar.show()
}

fun View.zoomToFit(videoWidth: Int, videoHeight: Int) {
    (parent as View).doOnLayout { parentView ->
        val cardWidth = parentView.measuredWidth
        val cardHeight = parentView.measuredHeight
        Timber.d("Zoom to fit: $cardWidth, $cardHeight measured: ${parentView.width}, ${parentView.height}, $parent")
        val size = calculateSurfaceSize(cardWidth, cardHeight, videoWidth, videoHeight)
        layoutParams = FrameLayout.LayoutParams(size.first, size.second)
    }
}

fun List<TextureView>.onReady(onReady: (playerViews: List<PlayerViewModel>) -> Unit) = launchMain {
    val surfaceReadyFlows = mapIndexed { index, textureView ->
        textureView.waitForSurface(index)
    }
    combine(surfaceReadyFlows) { playerViews ->
        playerViews.toList()
    }.collectLatest { playerViews ->
        onReady(playerViews.toList())
    }
}

fun TextureView.onReady(index: Int, onReady: (playerView: PlayerViewModel) -> Unit) = launchMain {
    waitForSurface(index).collectLatest { playerView ->
        onReady(playerView)
    }
}

fun ConstraintLayout.LayoutParams.clearAllAnchors() {
    startToStart = ConstraintLayout.LayoutParams.UNSET
    startToEnd = ConstraintLayout.LayoutParams.UNSET
    topToTop = ConstraintLayout.LayoutParams.UNSET
    topToBottom = ConstraintLayout.LayoutParams.UNSET
    endToEnd = ConstraintLayout.LayoutParams.UNSET
    endToStart = ConstraintLayout.LayoutParams.UNSET
    bottomToBottom = ConstraintLayout.LayoutParams.UNSET
    bottomToTop = ConstraintLayout.LayoutParams.UNSET
    matchConstraintPercentHeight = 1f
    matchConstraintPercentWidth = 1f
    matchConstraintDefaultHeight = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD
    matchConstraintDefaultWidth = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD
}

fun View.onDrawn(onDrawn: () -> Unit) {
    invalidate()
    requestLayout()
    doOnLayout { onDrawn() }
}

private fun TextureView.waitForSurface(index: Int) = channelFlow {
    if (surfaceTexture != null) {
        Timber.d("Player view already ready: $index")
        offer(PlayerViewModel(index, this@waitForSurface, Surface(surfaceTexture)))
        return@channelFlow
    }
    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            Timber.d("Player view just became ready: $index")
            surfaceTextureListener = null
            offer(PlayerViewModel(index, this@waitForSurface, Surface(surfaceTexture)))
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            /* Ignored */
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = false

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            /* Ignored */
        }
    }
    awaitClose()
}

private fun calculateSurfaceSize(surfaceWidth: Int, surfaceHeight: Int, videoWidth: Int, videoHeight: Int): Pair<Int, Int> {
    val ratioHeight = videoHeight.toFloat() / videoWidth.toFloat()
    val ratioWidth = videoWidth.toFloat() / videoHeight.toFloat()
    val isPortrait = videoWidth < videoHeight
    val calculatedHeight = if (isPortrait) (surfaceWidth / ratioWidth).toInt() else (surfaceWidth * ratioHeight).toInt()
    val calculatedWidth = if (isPortrait) (surfaceHeight / ratioHeight).toInt() else (surfaceHeight * ratioWidth).toInt()
    Timber.d("CALCULATED: ($surfaceWidth, $calculatedHeight) OR ($calculatedWidth, $surfaceHeight) FOR SURFACE: ($surfaceWidth, $surfaceHeight), VIDEO: ($videoWidth, $videoHeight)")
    return if (calculatedWidth >= surfaceWidth) {
        Pair(calculatedWidth, surfaceHeight)
    } else {
        Pair(surfaceWidth, calculatedHeight)
    }
}
