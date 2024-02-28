package com.mutkuensert.basicbottomsheet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlin.math.roundToInt

private val SheetShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

private val DefaultEnterTransition = slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(easing = LinearOutSlowInEasing)
)

private val DefaultExitTransition = slideOutVertically(
    targetOffsetY = { it },
    animationSpec = tween(easing = LinearOutSlowInEasing)
)

@Composable
fun BasicBottomSheet(
    onCloseSheet: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean,
    containerColor: Color = Color.Black.copy(0.4f),
    sheetColor: Color = MaterialTheme.colors.surface,
    closeSheetThreshold: Dp = 150.dp,
    shape: Shape = SheetShape,
    enterTransition: EnterTransition = DefaultEnterTransition,
    exitTransition: ExitTransition = DefaultExitTransition,
    dragHandle: @Composable (() -> Unit)? = { Handle() },
    content: @Composable ColumnScope.() -> Unit
) {
    var shouldCoverScreen by remember { mutableStateOf(visible) }

    LaunchedEffect(visible) { if (visible) shouldCoverScreen = true }

    if (shouldCoverScreen) {
        Popup {
            Box(modifier = modifier) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(containerColor)
                        .clickable(onClick = onCloseSheet)
                )

                Sheet(
                    modifier = Modifier.pointerInput(Unit) {
                        /*To prevent triggering ripple indication
                        of other elements in box on tap gestures.*/
                        detectTapGestures()
                    },
                    shouldBeVisible = visible,
                    onCloseSheet = onCloseSheet,
                    sheetColor = sheetColor,
                    closeSheetThreshold = closeSheetThreshold,
                    shape = shape,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    onSheetClosedCallback = { shouldCoverScreen = false },
                    dragHandle = dragHandle,
                    content = content
                )
            }
        }
    }
}

@Composable
private fun BoxScope.Sheet(
    modifier: Modifier = Modifier,
    shouldBeVisible: Boolean,
    onCloseSheet: () -> Unit,
    sheetColor: Color = MaterialTheme.colors.surface,
    closeSheetThreshold: Dp,
    shape: Shape = SheetShape,
    enterTransition: EnterTransition,
    exitTransition: ExitTransition,
    onSheetClosedCallback: () -> Unit,
    dragHandle: @Composable (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    var sheetVerticalOffset by remember { mutableFloatStateOf(0f) }
    val sheetVisibilityTransitionState by remember {
        mutableStateOf(MutableTransitionState(false))
    }

    LaunchedEffect(shouldBeVisible) {
        if (shouldBeVisible) {
            sheetVerticalOffset = 0f
        }

        sheetVisibilityTransitionState.targetState = shouldBeVisible
    }

    AnimatedVisibility(
        visibleState = sheetVisibilityTransitionState,
        enter = enterTransition,
        exit = exitTransition,
        modifier = modifier
            .align(Alignment.BottomCenter)
            .offset {
                IntOffset(0, sheetVerticalOffset.roundToInt())
            }
    ) {
        BackHandler { onCloseSheet.invoke() }

        Box(
            modifier = Modifier.background(sheetColor, shape)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (dragHandle != null) {
                    SheetHandle(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        sheetOffset = sheetVerticalOffset,
                        onVerticalDrag = { verticalDragDiff ->
                            sheetVerticalOffset = getSheetVerticalPositionOffset(
                                currentPosition = sheetVerticalOffset,
                                positionChange = verticalDragDiff
                            )
                        },
                        onVerticalDragReachedLimit = onCloseSheet,
                        onDragEnd = { sheetVerticalOffset = 0f },
                        onDragCancel = { sheetVerticalOffset = 0f },
                        closeSheetThreshold = closeSheetThreshold,
                    ) {
                        dragHandle.invoke()
                    }
                }

                content.invoke(this)
            }
        }
    }

    if (!shouldBeVisible && isSheetReadyToClose(sheetVisibilityTransitionState)) {
        onSheetClosedCallback.invoke()
    }
}

private fun isSheetReadyToClose(sheetVisibilityTransitionState: MutableTransitionState<Boolean>): Boolean {
    return !sheetVisibilityTransitionState.targetState && sheetVisibilityTransitionState.isIdle
}


@Composable
private fun SheetHandle(
    modifier: Modifier = Modifier,
    sheetOffset: Float,
    onVerticalDrag: (verticalDragDiff: Float) -> Unit,
    onVerticalDragReachedLimit: () -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    closeSheetThreshold: Dp,
    shape: Shape = RoundedCornerShape(4.dp),
    content: @Composable (() -> Unit)?,
) {
    val configuration = LocalConfiguration.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(shape)
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(Unit) {
                var totalDragAmount = 0f
                var currentSheetOffset = sheetOffset

                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        onVerticalDrag.invoke(change.positionChange().y)

                        currentSheetOffset = getSheetVerticalPositionOffset(
                            currentSheetOffset,
                            change.positionChange().y
                        )
                        totalDragAmount += dragAmount

                        if (shouldSheetClose(
                                screenHeight = configuration.screenHeightDp.dp.toPx(),
                                currentSheetOffset = currentSheetOffset,
                                totalDragAmount = totalDragAmount,
                                closeSheetThreshold = closeSheetThreshold.toPx()
                            )
                        ) {
                            onVerticalDragReachedLimit.invoke()
                        }
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(PressInteraction.Press(offset))

                    tryAwaitRelease()

                    interactionSource.emit(PressInteraction.Release(press))
                })
            }
    ) {
        content?.invoke()
    }
}

@Composable
private fun Handle(modifier: Modifier = Modifier) {
    Divider(
        modifier = modifier
            .padding(vertical = 17.dp)
            .width(50.dp)
            .height(2.dp)
            .background(MaterialTheme.colors.onSurface)
    )
}

private fun getSheetVerticalPositionOffset(currentPosition: Float, positionChange: Float): Float {
    return if (currentPosition + positionChange > 0) {
        currentPosition + positionChange
    } else {
        currentPosition
    }
}


private fun shouldSheetClose(
    screenHeight: Float,
    currentSheetOffset: Float,
    totalDragAmount: Float,
    closeSheetThreshold: Float,
): Boolean {
    return currentSheetOffset > screenHeight * 0.3 || totalDragAmount > closeSheetThreshold
}