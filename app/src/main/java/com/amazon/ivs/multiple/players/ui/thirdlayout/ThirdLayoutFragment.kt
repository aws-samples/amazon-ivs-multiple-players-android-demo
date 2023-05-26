package com.amazon.ivs.multiple.players.ui.thirdlayout

import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.amazon.ivs.multiple.players.R
import com.amazon.ivs.multiple.players.common.*
import com.amazon.ivs.multiple.players.databinding.FragmentThirdBinding
import com.amazon.ivs.multiple.players.ui.models.ThirdLayoutStream
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.roundToInt

@AndroidEntryPoint
class ThirdLayoutFragment : Fragment() {
    private lateinit var binding: FragmentThirdBinding
    private val viewModel by activityViewModels<ThirdLayoutViewModel>()

    private val videoSurfaces get() = listOf(
        binding.surfaceViewA,
        binding.surfaceViewB,
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentThirdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collect(viewModel.onBuffering) { bufferingStates ->
            bufferingStates.forEach { state ->
                when (state.playerId) {
                    ThirdLayoutStream.STREAM_A.index -> {
                        binding.surfaceBufferingA = state.buffering
                    }
                    ThirdLayoutStream.STREAM_B.index -> {
                        binding.surfaceBufferingB = state.buffering
                    }
                }
            }
        }

        collect(viewModel.onError) { error ->
            binding.root.showSnackBar(error.errorMessage)
        }

        collect(viewModel.onSizeChanged) { playerIndex ->
            videoSurfaces[playerIndex].onReady(playerIndex) { playerView ->
                Timber.d("Player size changed: $playerView")
                viewModel.updatePlayerView(playerView)
            }
        }

        collect(viewModel.onPlaying) { playing ->
            binding.controls.isPLaying = playing
        }

        binding.controls.play.setOnClickListener {
            if (viewModel.isPlaying) {
                viewModel.pause()
            } else {
                viewModel.play()
            }
        }

        binding.controls.switchStream.setOnClickListener {
            videoSurfaces.forEach { playerView ->
                playerView.animate().alpha(0f).setDuration(SURFACE_FADE_OUT_DELAY).start()
            }
            launchUI {
                delay(SURFACE_FADE_OUT_DELAY)
                activity?.openFragment(R.id.navigation_fragment_first)
            }
        }

        updateOnRotation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOnRotation()
    }

    override fun onResume() {
        super.onResume()
        videoSurfaces.onReady { playerViews ->
            updateOnRotation()
            Timber.d("Initializing players")
            viewModel.initPlayers(requireContext(), playerViews)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.release()
    }

    private fun updateOnRotation() = launchUI {
        repeat(MEASURE_REPEAT_COUNT) {
            binding.root.onDrawn {
                videoSurfaces.onReady { playerViews ->
                    updatePlayerConstraints(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                    viewModel.updatePlayerViews(playerViews)
                }
            }
            delay(MEASURE_REPEAT_DELAY)
        }
    }

    private fun updatePlayerConstraints(isLandscape: Boolean) = with(binding) {
        val layoutParamsA = streamContainerA.layoutParams as ConstraintLayout.LayoutParams
        val layoutParamsB = streamContainerB.layoutParams as ConstraintLayout.LayoutParams

        layoutParamsA.clearAllAnchors()
        layoutParamsB.clearAllAnchors()

        binding.root.onDrawn {
            if (isLandscape) {
                layoutParamsA.startToStart = streamHolder.id
                layoutParamsA.topToTop = streamHolder.id
                layoutParamsA.endToStart = streamContainerB.id
                layoutParamsA.bottomToBottom = streamHolder.id

                layoutParamsB.setMargins(4.dp.roundToInt())
                layoutParamsB.startToEnd = streamContainerA.id
                layoutParamsB.topToTop = streamHolder.id
                layoutParamsB.endToEnd = streamHolder.id
                layoutParamsB.bottomToBottom = streamHolder.id
            } else {
                layoutParamsA.startToStart = streamHolder.id
                layoutParamsA.endToEnd = streamHolder.id
                layoutParamsA.topToTop = horizontalGuideline.id
                layoutParamsA.bottomToTop = horizontalGuideline.id

                layoutParamsB.setMargins(36.dp.roundToInt())
                layoutParamsB.topToBottom = horizontalGuideline.id
                layoutParamsB.endToEnd = streamHolder.id
            }

            updateViewDimensions(layoutParamsA, layoutParamsB, isLandscape)
            streamContainerA.layoutParams = layoutParamsA
            streamContainerB.layoutParams = layoutParamsB
        }
    }

    private fun updateViewDimensions(
        streamA: ConstraintLayout.LayoutParams,
        streamB: ConstraintLayout.LayoutParams,
        isLandscape: Boolean
    ) {
        val screenWidth = binding.root.width
        val screenHeight = binding.root.height
        var largeStreamWidth = MIN_CONTAINER_SIZE
        var largeStreamHeight = MIN_CONTAINER_SIZE
        var smallStreamWidth = MIN_CONTAINER_SIZE
        var smallStreamHeight = MIN_CONTAINER_SIZE

        if (isLandscape) {
            while (largeStreamWidth < (screenWidth - 24.dp) * SMALL_VIDEO_ANCHOR && largeStreamHeight < screenHeight - 8.dp) {
                largeStreamWidth += 1
                largeStreamHeight = largeStreamWidth * WIDE_SCREEN_PROPORTION
            }

            while (smallStreamWidth < screenWidth - largeStreamWidth - 24.dp && smallStreamHeight < screenHeight - 8.dp) {
                smallStreamWidth += 1
                smallStreamHeight = smallStreamWidth / WIDE_SCREEN_PROPORTION
            }
        } else {
            largeStreamWidth = screenWidth - 16.dp
            largeStreamHeight = largeStreamWidth * WIDE_SCREEN_PROPORTION

            smallStreamHeight = screenHeight / SMALL_VIDEO_SCALE_FACTOR
            smallStreamWidth = smallStreamHeight * WIDE_SCREEN_PROPORTION
        }

        streamA.width = largeStreamWidth.roundToInt()
        streamA.height = largeStreamHeight.roundToInt()
        streamB.width = smallStreamWidth.roundToInt()
        streamB.height = smallStreamHeight.roundToInt()
    }

    private companion object {
        private const val SMALL_VIDEO_ANCHOR = 0.78
        private const val SMALL_VIDEO_SCALE_FACTOR = 3f
    }
}
