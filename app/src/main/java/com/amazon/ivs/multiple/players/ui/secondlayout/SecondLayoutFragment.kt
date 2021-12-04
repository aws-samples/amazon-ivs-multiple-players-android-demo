package com.amazon.ivs.multiple.players.ui.secondlayout

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
import com.amazon.ivs.multiple.players.databinding.FragmentSecondBinding
import com.amazon.ivs.multiple.players.ui.models.SecondLayoutStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import kotlin.math.roundToInt

class SecondLayoutFragment : Fragment() {

    private lateinit var binding: FragmentSecondBinding
    private val viewModel: SecondLayoutViewModel by lazyViewModel(
        { requireActivity().application as App },
        { SecondLayoutViewModel() }
    )

    private val videoSurfaces get() = listOf(
        binding.surfaceViewA,
        binding.surfaceViewB,
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchUI {
            viewModel.onBuffering.collect { bufferingStates ->
                bufferingStates.forEach { state ->
                    when (state.playerId) {
                        SecondLayoutStream.STREAM_A.index -> {
                            binding.surfaceBufferingA = state.buffering
                        }
                        SecondLayoutStream.STREAM_B.index -> {
                            binding.surfaceBufferingB = state.buffering
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
                activity?.openFragment(R.id.navigation_fragment_third)
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

        layoutParamsA.clearAllAnchors()
        layoutParamsB.clearAllAnchors()

        binding.root.onDrawn {
            if (isLandscape) {
                layoutParamsA.topToTop = streamHolder.id
                layoutParamsA.endToStart = verticalGuideline.id
                layoutParamsA.bottomToBottom = streamHolder.id

                layoutParamsB.startToEnd = verticalGuideline.id
                layoutParamsB.topToTop = streamHolder.id
                layoutParamsB.bottomToBottom = streamHolder.id
            } else {
                layoutParamsA.startToStart = streamHolder.id
                layoutParamsA.endToEnd = streamHolder.id
                layoutParamsA.bottomToTop = horizontalGuideline.id

                layoutParamsB.startToStart = streamHolder.id
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
        var containerSide = MIN_CONTAINER_SIZE

        if (isLandscape) {
            while (containerSide < screenHeight - 16.dp && containerSide * 2 < screenWidth - 24.dp) {
                containerSide += 1
            }
        } else {
            while (containerSide < screenWidth - 16.dp && containerSide * 2 < screenHeight - 24.dp) {
                containerSide += 1
            }
        }

        streamA.width = containerSide.roundToInt()
        streamA.height = containerSide.roundToInt()
        streamB.width = containerSide.roundToInt()
        streamB.height = containerSide.roundToInt()
    }
}
