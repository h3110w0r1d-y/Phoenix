package com.h3110w0r1d.phoenix.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.compose.ui.util.fastSumBy
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

operator fun WideNavigationRailValue.not(): WideNavigationRailValue =
    if (this == WideNavigationRailValue.Collapsed) {
        WideNavigationRailValue.Expanded
    } else {
        WideNavigationRailValue.Collapsed
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeFlexibleTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    collapsedHeight: Dp = TopAppBarDefaults.LargeAppBarCollapsedHeight,
    expandedHeight: Dp = TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior,
) = TwoRowsTopAppBar(
    title = title,
    titleTextStyle = typography.headlineMedium,
    smallTitleTextStyle = typography.titleLarge,
    titleBottomPadding = LargeTitleBottomPadding,
    modifier = modifier,
    subtitle = subtitle,
    subtitleTextStyle = typography.titleLarge,
    smallSubtitleTextStyle = typography.titleMedium,
    titleHorizontalAlignment = titleHorizontalAlignment,
    navigationIcon = navigationIcon,
    actions = actions,
    collapsedHeight =
        if (collapsedHeight == Dp.Unspecified || collapsedHeight == Dp.Infinity) {
            TopAppBarDefaults.LargeAppBarCollapsedHeight
        } else {
            collapsedHeight
        },
    expandedHeight =
        if (expandedHeight == Dp.Unspecified || expandedHeight == Dp.Infinity) {
            TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight
//            if (subtitle != null) {
//                TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight
//            } else {
//                TopAppBarDefaults.LargeFlexibleAppBarWithoutSubtitleExpandedHeight
//            }
        } else {
            expandedHeight
        },
    windowInsets = windowInsets,
    colors = colors,
    scrollBehavior = scrollBehavior,
)

@Composable
fun rememberTopAppBarState(
    initialHeightOffsetLimit: Float = -Float.MAX_VALUE,
    initialHeightOffset: Float = 0f,
    initialContentOffset: Float = 0f,
): TopAppBarState =
    rememberSaveable(saver = TopAppBarState.Saver) {
        TopAppBarState(initialHeightOffsetLimit, initialHeightOffset, initialContentOffset)
    }

private fun TopAppBarColors.containerColor(colorTransitionFraction: Float): Color =
    lerp(
        containerColor,
        scrolledContainerColor,
        FastOutLinearInEasing.transform(colorTransitionFraction),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwoRowsTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    titleTextStyle: TextStyle,
    titleBottomPadding: Dp,
    smallTitleTextStyle: TextStyle,
    subtitle: (@Composable () -> Unit)?,
    subtitleTextStyle: TextStyle,
    smallSubtitleTextStyle: TextStyle,
    titleHorizontalAlignment: Alignment.Horizontal,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    collapsedHeight: Dp,
    expandedHeight: Dp,
    windowInsets: WindowInsets,
    colors: TopAppBarColors,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val scope =
        TwoRowsTopAppBarOverrideScope(
            modifier = modifier,
            title = title,
            titleTextStyle = titleTextStyle,
            titleBottomPadding = titleBottomPadding,
            smallTitleTextStyle = smallTitleTextStyle,
            subtitle = subtitle,
            subtitleTextStyle = subtitleTextStyle,
            smallSubtitleTextStyle = smallSubtitleTextStyle,
            titleHorizontalAlignment = titleHorizontalAlignment,
            navigationIcon = navigationIcon,
            actions = actions,
            collapsedHeight = collapsedHeight,
            expandedHeight = expandedHeight,
            windowInsets = windowInsets,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    with(LocalTwoRowsTopAppBarOverride.current) { scope.TwoRowsTopAppBar() }
}

@OptIn(ExperimentalMaterial3Api::class)
internal object DefaultTwoRowsTopAppBarOverride : TwoRowsTopAppBarOverride {
    @Composable
    override fun TwoRowsTopAppBarOverrideScope.TwoRowsTopAppBar() {
        require(collapsedHeight.isSpecified && collapsedHeight.isFinite) {
            "The collapsedHeight is expected to be specified and finite"
        }
        require(expandedHeight.isSpecified && expandedHeight.isFinite) {
            "The expandedHeight is expected to be specified and finite"
        }
        require(expandedHeight >= collapsedHeight) {
            "The expandedHeight is expected to be greater or equal to the collapsedHeight"
        }

        // Obtain the container Color from the TopAppBarColors using the `collapsedFraction`, as the
        // bottom part of this TwoRowsTopAppBar changes color at the same rate the app bar expands
        // or collapse.
        // This will potentially animate or interpolate a transition between the container color and
        // the container's scrolled color according to the app bar's scroll state.
        val collapsedFraction = scrollBehavior.state.collapsedFraction
        val expandedFraction = 1 - scrollBehavior.state.collapsedFraction
        val appBarContainerColor = { colors.containerColor(collapsedFraction) }

        // Wrap the given actions in a Row.
        val actionsRow =
            @Composable {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        val density = LocalDensity.current
        val maxExpandableHeightPx =
            with(density) {
                (expandedHeight).roundToPx() - collapsedHeight.roundToPx()
            }

        // Set up support for resizing the top app bar when vertically dragging the bar itself.
        val appBarDragModifier =
            if (!scrollBehavior.isPinned) {
                Modifier.draggable(
                    orientation = Orientation.Vertical,
                    state =
                        rememberDraggableState { delta ->
                            scrollBehavior.state.heightOffset += delta
                        },
                    onDragStopped = { velocity ->
                        settleAppBar(
                            scrollBehavior.state,
                            velocity,
                            scrollBehavior.flingAnimationSpec,
                            scrollBehavior.snapAnimationSpec,
                        )
                    },
                )
            } else {
                Modifier
            }

        Box(
            modifier =
                modifier
                    .then(appBarDragModifier)
                    .drawBehind { drawRect(color = appBarContainerColor()) }
                    .semantics { isTraversalGroup = true }
                    .pointerInput(Unit) {},
        ) {
            val finalTitleStyle =
                lerp(
                    smallTitleTextStyle,
                    titleTextStyle,
                    expandedFraction,
                )
            val finalSubtitleStyle =
                lerp(
                    smallSubtitleTextStyle,
                    subtitleTextStyle,
                    expandedFraction,
                )
            val smallTitleBottomPadding =
                if (subtitle == null) {
                    25.dp
                } else {
                    13.dp
                }
            val titleBottomPaddingPx =
                with(LocalDensity.current) {
                    (
                        smallTitleBottomPadding +
                            (titleBottomPadding - smallTitleBottomPadding) * expandedFraction
                    ).roundToPx()
                }

            val easedFraction by remember(collapsedFraction) {
                derivedStateOf {
                    FastOutSlowInEasing.transform(collapsedFraction)
                }
            }
            TopAppBarLayout(
                modifier =
                    Modifier
                        .windowInsetsPadding(windowInsets)
                        .clipToBounds()
                        .let { modifier ->
                            modifier.onSizeChanged { size ->
                                // 在 onSizeChanged 中使用预计算的值
                                scrollBehavior.state.let { state ->
                                    state.heightOffsetLimit = -maxExpandableHeightPx.toFloat()
                                }
                            }
                        },
//                    scrolledOffset = { scrollBehavior.state?.heightOffset ?: 0f },
                scrolledOffset = { 0f },
                navigationIconContentColor = colors.navigationIconContentColor,
                titleContentColor = colors.titleContentColor,
                actionIconContentColor = colors.actionIconContentColor,
                subtitleContentColor = colors.subtitleContentColor,
                title = title,
                titleTextStyle = finalTitleStyle,
                subtitle = subtitle,
                subtitleTextStyle = finalSubtitleStyle,
//                    titleAlpha = bottomTitleAlpha,
                titleAlpha = { 1f },
                titleVerticalArrangement = Arrangement.Bottom,
                titleHorizontalAlignment = titleHorizontalAlignment,
                titleBottomPadding = titleBottomPaddingPx,
                navigationIcon = navigationIcon,
                actions = actionsRow,
//                    height = expandedHeight - collapsedHeight,
                collapsedHeight = collapsedHeight,
                height = calculateDynamicAppBarHeight(expandedHeight, collapsedHeight, scrollBehavior),
                collapsedFraction = easedFraction,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun calculateDynamicAppBarHeight(
    expandedHeight: Dp,
    collapsedHeight: Dp,
    scrollBehavior: TopAppBarScrollBehavior,
): Dp =
    with(LocalDensity.current) {
        // 获取当前滚动偏移值（原本的 topOffset）
        val currentHeightOffset = scrollBehavior.state.heightOffset
        val heightOffsetPx = currentHeightOffset.roundToInt()

        // 计算基础可扩展高度
        val baseExpandableHeight = (expandedHeight).roundToPx()

        // 将原本的 topOffset 值减去，实现固定效果
        val fixedHeightPx = baseExpandableHeight + heightOffsetPx

        // 确保高度不为负数
        fixedHeightPx.toDp().coerceAtLeast(collapsedHeight)
    }

@OptIn(ExperimentalMaterial3Api::class)
internal interface TwoRowsTopAppBarOverride {
    @Composable fun TwoRowsTopAppBarOverrideScope.TwoRowsTopAppBar()
}

@OptIn(ExperimentalMaterial3Api::class)
internal class TwoRowsTopAppBarOverrideScope
    internal constructor(
        val modifier: Modifier,
        val title: @Composable () -> Unit,
        val titleTextStyle: TextStyle,
        val titleBottomPadding: Dp,
        val smallTitleTextStyle: TextStyle,
        val subtitle: (@Composable () -> Unit)?,
        val subtitleTextStyle: TextStyle,
        val smallSubtitleTextStyle: TextStyle,
        val titleHorizontalAlignment: Alignment.Horizontal,
        val navigationIcon: @Composable () -> Unit,
        val actions: @Composable RowScope.() -> Unit,
        val collapsedHeight: Dp,
        val expandedHeight: Dp,
        val windowInsets: WindowInsets,
        val colors: TopAppBarColors,
        val scrollBehavior: TopAppBarScrollBehavior,
    )

@OptIn(ExperimentalMaterial3Api::class)
internal val LocalTwoRowsTopAppBarOverride: ProvidableCompositionLocal<TwoRowsTopAppBarOverride> =
    compositionLocalOf {
        DefaultTwoRowsTopAppBarOverride
    }

@Composable
fun ProvideContentColorTextStyle(
    contentColor: Color,
    textStyle: TextStyle,
    content: @Composable () -> Unit,
) {
    val mergedStyle = LocalTextStyle.current.merge(textStyle)
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides mergedStyle,
        content = content,
    )
}

fun interface FloatProducer {
    operator fun invoke(): Float
}

@Composable
private fun TopAppBarLayout(
    modifier: Modifier,
    scrolledOffset: FloatProducer,
    navigationIconContentColor: Color,
    titleContentColor: Color,
    subtitleContentColor: Color,
    actionIconContentColor: Color,
    title: @Composable () -> Unit = {},
    titleTextStyle: TextStyle,
    subtitle: (@Composable () -> Unit)? = null,
    subtitleTextStyle: TextStyle,
    titleAlpha: () -> Float = { 1f },
    titleVerticalArrangement: Arrangement.Vertical,
    titleHorizontalAlignment: Alignment.Horizontal,
    titleBottomPadding: Int,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    height: Dp,
    collapsedHeight: Dp,
    collapsedFraction: Float = 0f,
) {
    Layout(
        {
            Box(
                Modifier
                    .layoutId("navigationIcon")
                    .padding(start = TopAppBarHorizontalPadding),
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides navigationIconContentColor,
                    content = navigationIcon,
                )
            }
            if (subtitle != null) {
                Column(
                    modifier =
                        Modifier
                            .layoutId("title")
                            .padding(horizontal = TopAppBarHorizontalPadding)
                            .graphicsLayer { alpha = titleAlpha() },
                    horizontalAlignment = titleHorizontalAlignment,
                ) {
                    ProvideContentColorTextStyle(
                        contentColor = titleContentColor,
                        textStyle = titleTextStyle,
                        content = title,
                    )
                    ProvideContentColorTextStyle(
                        contentColor = subtitleContentColor,
                        textStyle = subtitleTextStyle,
                        content = subtitle,
                    )
                }
            } else { // TODO(b/352770398): Workaround to maintain compatibility
                Box(
                    modifier =
                        Modifier
                            .layoutId("title")
                            .padding(horizontal = TopAppBarHorizontalPadding)
                            .graphicsLayer { alpha = titleAlpha() },
                ) {
                    ProvideContentColorTextStyle(
                        contentColor = titleContentColor,
                        textStyle = titleTextStyle,
                        content = title,
                    )
                }
            }
            Box(
                Modifier
                    .layoutId("actionIcons")
                    .padding(end = TopAppBarHorizontalPadding),
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides actionIconContentColor,
                    content = actions,
                )
            }
        },
        modifier = modifier,
        measurePolicy =
            remember(
                scrolledOffset,
                titleVerticalArrangement,
                titleHorizontalAlignment,
                titleBottomPadding,
                height,
                collapsedHeight,
                collapsedFraction,
            ) {
                TopAppBarMeasurePolicy(
                    scrolledOffset,
                    titleVerticalArrangement,
                    titleHorizontalAlignment,
                    titleBottomPadding,
                    height,
                    collapsedHeight,
                    collapsedFraction,
                )
            },
    )
}

private class TopAppBarMeasurePolicy(
    val scrolledOffset: FloatProducer,
    val titleVerticalArrangement: Arrangement.Vertical,
    val titleHorizontalAlignment: Alignment.Horizontal,
    val titleBottomPadding: Int,
    val height: Dp,
    val collapsedHeight: Dp,
    val collapsedFraction: Float,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val navigationIconPlaceable =
            measurables
                .fastFirst { it.layoutId == "navigationIcon" }
                .measure(constraints.copy(minWidth = 0))
        val actionIconsPlaceable =
            measurables
                .fastFirst { it.layoutId == "actionIcons" }
                .measure(constraints.copy(minWidth = 0))

        val maxTitleWidth =
            if (constraints.maxWidth == Constraints.Infinity) {
                constraints.maxWidth
            } else {
                (constraints.maxWidth - navigationIconPlaceable.width - actionIconsPlaceable.width)
                    .coerceAtLeast(0)
            }
        val titlePlaceable =
            measurables
                .fastFirst { it.layoutId == "title" }
                .measure(constraints.copy(minWidth = 0, maxWidth = maxTitleWidth))

        // Locate the title's baseline.
        val titleBaseline =
            if (titlePlaceable[LastBaseline] != AlignmentLine.Unspecified) {
                titlePlaceable[LastBaseline]
            } else {
                0
            }
        // Subtract the scrolledOffset from the maxHeight. The scrolledOffset is expected to be
        // equal or smaller than zero.
        val scrolledOffsetValue = scrolledOffset()
        val heightOffset = if (scrolledOffsetValue.isNaN()) 0 else scrolledOffsetValue.roundToInt()
        val minLayoutHeight = collapsedHeight.roundToPx()
        val maxLayoutHeight = max(height.roundToPx(), titlePlaceable.height)
        val layoutHeight =
            if (constraints.maxHeight == Constraints.Infinity) {
                maxLayoutHeight
            } else {
                (maxLayoutHeight + heightOffset).coerceAtLeast(0)
            }

        return placeTopAppBar(
            constraints,
            layoutHeight,
            minLayoutHeight,
            maxLayoutHeight,
            navigationIconPlaceable,
            titlePlaceable,
            actionIconsPlaceable,
            titleBaseline,
            collapsedFraction,
        )
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ) = measurables.fastSumBy { it.minIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int =
        max(
            height.roundToPx(),
            measurables.fastMaxOfOrNull { it.minIntrinsicHeight(width) } ?: 0,
        )

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ) = measurables.fastSumBy { it.maxIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int =
        max(
            height.roundToPx(),
            measurables.fastMaxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0,
        )

    private fun MeasureScope.placeTopAppBar(
        constraints: Constraints,
        layoutHeight: Int,
        minLayoutHeight: Int,
        maxLayoutHeight: Int,
        navigationIconPlaceable: Placeable,
        titlePlaceable: Placeable,
        actionIconsPlaceable: Placeable,
        titleBaseline: Int,
        collapsedFraction: Float,
    ): MeasureResult =
        layout(constraints.maxWidth, layoutHeight) {
            // Navigation icon
            navigationIconPlaceable.placeRelative(
                x = 0,
                y = (minLayoutHeight - navigationIconPlaceable.height) / 2,
            )
            titlePlaceable.let {
                var start = max(TopAppBarTitleInset.roundToPx(), navigationIconPlaceable.width)
                start = (TopAppBarTitleInset.roundToPx() + (start - TopAppBarTitleInset.roundToPx()) * collapsedFraction).toInt()
                val end = actionIconsPlaceable.width

                // Align using the maxWidth. We will adjust the position later according to the
                // start and end. This is done to ensure that a center alignment is still maintained
                // when the start and end have different widths. Note that the title is centered
                // relative to the entire app bar width, and not just centered between the
                // navigation icon and the actions.
                var titleX =
                    titleHorizontalAlignment.align(
                        size = titlePlaceable.width,
                        space = constraints.maxWidth,
                        // Using Ltr as we call placeRelative later on.
                        layoutDirection = LayoutDirection.Ltr,
                    )
                // Reposition the title based on the start and the end (i.e. the navigation and
                // action widths).
                if (titleX < start) {
                    titleX += (start - titleX)
                } else if (titleX + titlePlaceable.width > constraints.maxWidth - end) {
                    titleX += ((constraints.maxWidth - end) - (titleX + titlePlaceable.width))
                }

                // The titleVerticalArrangement is always one of Center or Bottom.
                val titleY =
                    when (titleVerticalArrangement) {
                        Arrangement.Center -> (layoutHeight - titlePlaceable.height) / 2
                        // Apply bottom padding from the title's baseline only when the Arrangement
                        // is "Bottom".
                        Arrangement.Bottom ->
                            if (titleBottomPadding == 0) {
                                layoutHeight - titlePlaceable.height
                            } else {
                                // Calculate the actual padding from the bottom of the title, taking
                                // into account its baseline.
                                val paddingFromBottom =
                                    titleBottomPadding - (titlePlaceable.height - titleBaseline)
                                // Adjust the bottom padding to a smaller number if there is no room
                                // to fit the title.
                                val heightWithPadding = paddingFromBottom + titlePlaceable.height
                                val adjustedBottomPadding =
                                    if (heightWithPadding > maxLayoutHeight) {
                                        paddingFromBottom - (heightWithPadding - maxLayoutHeight)
                                    } else {
                                        paddingFromBottom
                                    }

                                layoutHeight - titlePlaceable.height - max(0, adjustedBottomPadding)
                            }
                        // Arrangement.Top
                        else -> 0
                    }

                it.placeRelative(titleX, titleY)
            }

            // Action icons
            actionIconsPlaceable.placeRelative(
                x = constraints.maxWidth - actionIconsPlaceable.width,
                y = (minLayoutHeight - actionIconsPlaceable.height) / 2,
            )
        }
}

@OptIn(ExperimentalMaterial3Api::class)
private suspend fun settleAppBar(
    state: TopAppBarState,
    velocity: Float,
    flingAnimationSpec: DecayAnimationSpec<Float>?,
    snapAnimationSpec: AnimationSpec<Float>?,
): Velocity {
    // Check if the app bar is completely collapsed/expanded. If so, no need to settle the app bar,
    // and just return Zero Velocity.
    // Note that we don't check for 0f due to float precision with the collapsedFraction
    // calculation.
    if (state.collapsedFraction < 0.01f || state.collapsedFraction == 1f) {
        return Velocity.Zero
    }
    var remainingVelocity = velocity
    // In case there is an initial velocity that was left after a previous user fling, animate to
    // continue the motion to expand or collapse the app bar.
    if (flingAnimationSpec != null && abs(velocity) > 1f) {
        var lastValue = 0f
        AnimationState(initialValue = 0f, initialVelocity = velocity).animateDecay(
            flingAnimationSpec,
        ) {
            val delta = value - lastValue
            val initialHeightOffset = state.heightOffset
            state.heightOffset = initialHeightOffset + delta
            val consumed = abs(initialHeightOffset - state.heightOffset)
            lastValue = value
            remainingVelocity = this.velocity
            // avoid rounding errors and stop if anything is unconsumed
            if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
        }
    }
    // Snap if animation specs were provided.
    if (snapAnimationSpec != null) {
        if (state.heightOffset < 0 && state.heightOffset > state.heightOffsetLimit) {
            AnimationState(initialValue = state.heightOffset).animateTo(
                if (state.collapsedFraction < 0.5f) {
                    0f
                } else {
                    state.heightOffsetLimit
                },
                animationSpec = snapAnimationSpec,
            ) {
                state.heightOffset = value
            }
        }
    }

    return Velocity(0f, remainingVelocity)
}

private val LargeTitleBottomPadding = 28.dp
private val TopAppBarHorizontalPadding = 4.dp

// A title inset when the App-Bar is a Medium or Large one. Also used to size a spacer when the
// navigation icon is missing.
private val TopAppBarTitleInset = 16.dp - TopAppBarHorizontalPadding
