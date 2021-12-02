package com.amazon.ivs.multiple.players.ui.firstlayout

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.amazon.ivs.multiple.players.App
import com.amazon.ivs.multiple.players.R
import com.amazon.ivs.multiple.players.common.*
import com.amazon.ivs.multiple.players.databinding.FragmentFirstBinding
import com.amazon.ivs.multiple.players.ui.models.FirstLayoutStream
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import kotlin.math.roundToInt

class FirstLayoutFragment : Fragment() {

    private lateinit var binding: FragmentFirstBinding
    private val viewModel: FirstLayoutViewModel by lazyViewModel(
        { requireActivity().application as App },
        { FirstLayoutViewModel() }
    )

    private val videoSurfaces get() = listOf(
        binding.surfaceViewA,
        binding.surfaceViewB,
        binding.surfaceViewC
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchUI {
            viewModel.onBuffering.collect { bufferingStates ->
                bufferingStates.forEach { state ->
                    when (state.playerId) {
                        FirstLayoutStream.STREAM_A.index -> {
                            binding.surfaceBufferingA = state.buffering
                        }
                        FirstLayoutStream.STREAM_B.index -> {
                            binding.surfaceBufferingB = state.buffering
                        }
                        FirstLayoutStream.STREAM_C.index -> {
                            binding.surfaceBufferingC = state.buffering
                        }
                    }
                }
            }
        }

        launchUI {
            viewModel.onError.collect { error ->
                binding.root.showSnackBar(error.errorMessage)
            }
        }

        launchUI {
            viewModel.onSizeChanged.collect { playerIndex ->
                videoSurfaces[playerIndex].onReady(playerIndex) { playerView ->
                    Timber.d("Player size changed: $playerView")
                    viewModel.updatePlayerView(playerView)
                }
            }
        }

        launchUI {
            viewModel.onPlaying.collect { playing ->
                binding.controls.isPLaying = playing
            }
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
            launchMain {
                delay(SURFACE_FADE_OUT_DELAY)
                activity?.openFragment(R.id.navigation_fragment_second)
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

    private fun updateOnRotation() = launchMain {
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
        val layoutParamsC = streamContainerC.layoutParams as ConstraintLayout.LayoutParams

        layoutParamsA.clearAllAnchors()
        layoutParamsB.clearAllAnchors()
        layoutParamsC.clearAllAnchors()

        binding.root.onDrawn {
            if (isLandscape) {
                layoutParamsA.startToStart = streamHolder.id
                layoutParamsA.topToTop = streamHolder.id
                layoutParamsA.endToStart = streamContainerB.id
                layoutParamsA.bottomToBottom = streamHolder.id

                layoutParamsB.startToEnd = streamContainerA.id
                layoutParamsB.endToEnd = streamHolder.id
                layoutParamsB.topToTop = streamContainerA.id
                layoutParamsB.bottomToTop = streamContainerC.id

                layoutParamsC.startToStart = streamContainerB.id
                layoutParamsC.topToBottom = streamContainerB.id
                layoutParamsC.endToEnd = streamContainerB.id
                layoutParamsC.bottomToBottom = streamContainerA.id
            } else {
                verticalGuideline.setGuidelinePercent(0.5f)
                layoutParamsA.startToStart = streamHolder.id
                layoutParamsA.endToEnd = streamHolder.id
                layoutParamsA.bottomToTop = horizontalGuideline.id

                layoutParamsB.startToStart = streamHolder.id
                layoutParamsB.topToBottom = horizontalGuideline.id
                layoutParamsB.endToStart = verticalGuideline.id

                layoutParamsC.startToEnd = verticalGuideline.id
                layoutParamsC.topToBottom = horizontalGuideline.id
                layoutParamsC.endToEnd = streamHolder.id
            }

            updateViewDimensions(layoutParamsA, layoutParamsB, layoutParamsC, isLandscape)
            streamContainerA.layoutParams = layoutParamsA
            streamContainerB.layoutParams = layoutParamsB
            streamContainerC.layoutParams = layoutParamsC
        }
    }

    private fun updateViewDimensions(
        streamA: ConstraintLayout.LayoutParams,
        streamB: ConstraintLayout.LayoutParams,
        streamC: ConstraintLayout.LayoutParams,
        isLandscape: Boolean
    ) {
        val screenWidth = binding.root.width
        val screenHeight = binding.root.height
        var largeStreamWidth = MIN_CONTAINER_SIZE
        var largeStreamHeight = MIN_CONTAINER_SIZE
        var smallStreamHeight = MIN_CONTAINER_SIZE
        var smallStreamWidth = MIN_CONTAINER_SIZE

        if (isLandscape) {
            while (largeStreamWidth + smallStreamWidth < screenWidth - 24.dp && largeStreamHeight < screenHeight - 8.dp) {
                largeStreamWidth += 1
                largeStreamHeight = largeStreamWidth * WIDE_SCREEN_PROPORTION

                smallStreamHeight = (largeStreamHeight - 8.dp) / 2
                smallStreamWidth = smallStreamHeight / WIDE_SCREEN_PROPORTION
            }
        } else {
            largeStreamWidth = screenWidth - 16.dp
            largeStreamHeight = largeStreamWidth * WIDE_SCREEN_PROPORTION

            smallStreamWidth = (largeStreamWidth - 8.dp) / 2
            smallStreamHeight = smallStreamWidth * WIDE_SCREEN_PROPORTION
        }

        streamA.width = (largeStreamWidth).roundToInt()
        streamA.height = (largeStreamHeight).roundToInt()
        streamB.width = (smallStreamWidth).roundToInt()
        streamB.height = (smallStreamHeight).roundToInt()
        streamC.width = (smallStreamWidth).roundToInt()
        streamC.height = (smallStreamHeight).roundToInt()
    }
}
