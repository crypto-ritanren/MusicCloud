package com.max.musiccloud.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.max.musiccloud.data.local.TrackEntity
import com.max.musiccloud.playback.PlayerState
import com.max.musiccloud.ui.theme.AppTheme
import java.io.File
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class DragDropState(
    val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit,
    private val onSwap: () -> Unit = {}
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set

    var currentDraggingIndex by mutableStateOf<Int?>(null)
        private set

    var initialItemOffset by mutableFloatStateOf(0f)
        private set

    var totalDragDistance by mutableFloatStateOf(0f)
        private set

    var draggedItemHeight by mutableFloatStateOf(160f)
        private set

    private var lastKnownOffset by mutableFloatStateOf(0f)

    val isDragging: Boolean
        get() = draggingKey != null

    val currentDraggingItem: LazyListItemInfo?
        get() {
            val key = draggingKey ?: return null
            return lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        }

    val fingerPositionY: Float
        get() {
            val viewportHeight = lazyListState.layoutInfo.viewportSize.height.toFloat()
            val maxPos = (viewportHeight - draggedItemHeight).coerceAtLeast(0f)
            return (initialItemOffset + totalDragDistance).coerceIn(0f, maxPos)
        }

    val draggingDeltaY: Float
        get() {
            val key = draggingKey ?: return 0f
            val item = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
            if (item != null) {
                lastKnownOffset = item.offset.toFloat()
            }
            val currentOffset = item?.offset?.toFloat() ?: lastKnownOffset
            val rawDelta = fingerPositionY - currentOffset
            val viewportHeight = lazyListState.layoutInfo.viewportSize.height.toFloat()
            val maxDelta = (viewportHeight - currentOffset - draggedItemHeight).coerceAtLeast(0f)
            return rawDelta.coerceIn(-currentOffset, maxDelta)
        }

    fun onDragStart(fallbackIndex: Int, key: Any) {
        val item = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
            ?: lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == fallbackIndex }

        val startOffset = item?.offset?.toFloat() ?: 0f
        initialItemOffset = startOffset
        lastKnownOffset = startOffset
        draggedItemHeight = item?.size?.toFloat() ?: 160f
        totalDragDistance = 0f
        draggingKey = key
        currentDraggingIndex = item?.index ?: fallbackIndex
    }

    fun onDrag(dragAmountY: Float) {
        if (!isDragging) return
        totalDragDistance += dragAmountY
        checkTargetUnderFinger()
    }

    fun checkTargetUnderFinger() {
        val key = draggingKey ?: return
        val currentIdx = currentDraggingIndex ?: return
        val currentCenter = fingerPositionY + (draggedItemHeight / 2f)

        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return

        // Exclude the currently dragged item from candidate slots
        val otherItems = visibleItems.filter { it.key != key }
        if (otherItems.isEmpty()) return

        // Moving UP: candidate items above current position whose center is below finger
        val targetItemUp = otherItems
            .filter { it.index < currentIdx && currentCenter < (it.offset + (it.size / 2f)) }
            .minByOrNull { it.index }

        // Moving DOWN: candidate items below current position whose center is above finger
        val targetItemDown = otherItems
            .filter { it.index > currentIdx && currentCenter > (it.offset + (it.size / 2f)) }
            .maxByOrNull { it.index }

        val targetItem = targetItemUp ?: targetItemDown

        if (targetItem != null && targetItem.index != currentIdx) {
            val targetIdx = targetItem.index
            onMove(currentIdx, targetIdx)
            onSwap()
            currentDraggingIndex = targetIdx
        }
    }

    fun onDragEnd() {
        draggingKey = null
        currentDraggingIndex = null
        totalDragDistance = 0f
        initialItemOffset = 0f
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit,
    onSwap: () -> Unit = {}
): DragDropState {
    val onMoveState by rememberUpdatedState(onMove)
    val onSwapState by rememberUpdatedState(onSwap)
    return remember(lazyListState) {
        DragDropState(
            lazyListState = lazyListState,
            onMove = { from, to -> onMoveState(from, to) },
            onSwap = { onSwapState() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    playerState: PlayerState,
    onDismissRequest: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipNextClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onMoveTrackInQueue: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onRemoveTrackFromQueue: (index: Int) -> Unit = {},
    onPlayQueueItem: (index: Int) -> Unit = {}
) {
    val track = playerState.currentTrack ?: return
    val colors = AppTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val duration = playerState.durationMs.coerceAtLeast(1L)

    val currentPositionMsState by rememberUpdatedState(playerState.currentPositionMs)
    val onSeekState by rememberUpdatedState(onSeek)

    var discRotationAngle by remember { mutableFloatStateOf(0f) }
    var isDraggingVinyl by remember { mutableStateOf(false) }
    var hasDraggedEnough by remember { mutableStateOf(false) }
    var dragStartPositionMs by remember { mutableLongStateOf(0L) }
    var scratchPositionMs by remember { mutableLongStateOf(0L) }
    var accumulatedDeltaMs by remember { mutableLongStateOf(0L) }
    var totalRotatedAngle by remember { mutableFloatStateOf(0f) }
    var lastTouchAngle by remember { mutableFloatStateOf(0f) }
    var lastSeekTimestamp by remember { mutableLongStateOf(0L) }
    var lastSoughtMs by remember { mutableLongStateOf(-1L) }

    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var draggingSliderPosition by remember { mutableFloatStateOf(0f) }
    var isQueueVisible by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val queueListState = rememberLazyListState()
    val dragDropState = rememberDragDropState(
        lazyListState = queueListState,
        onMove = onMoveTrackInQueue,
        onSwap = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    )

    // Block parent bottom sheet from stealing gestures during active drag
    val dragDropNestedScrollConnection = remember(dragDropState.isDragging) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (dragDropState.isDragging) available else Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                return if (dragDropState.isDragging) available else Offset.Zero
            }
        }
    }

    val scrollThresholdPx = with(density) { 110.dp.toPx() }

    // Auto-scroll when dragging near viewport edges
    LaunchedEffect(dragDropState.draggingKey) {
        while (dragDropState.isDragging) {
            val fingerPos = dragDropState.fingerPositionY
            val viewportHeight = queueListState.layoutInfo.viewportSize.height.toFloat()

            val scrollAmount = when {
                fingerPos < scrollThresholdPx && queueListState.canScrollBackward -> {
                    -((scrollThresholdPx - fingerPos) / 3.5f).coerceIn(4f, 24f)
                }
                (fingerPos + dragDropState.draggedItemHeight) > (viewportHeight - scrollThresholdPx) && queueListState.canScrollForward -> {
                    (((fingerPos + dragDropState.draggedItemHeight) - (viewportHeight - scrollThresholdPx)) / 3.5f).coerceIn(4f, 24f)
                }
                else -> 0f
            }

            if (scrollAmount != 0f) {
                queueListState.scrollBy(scrollAmount)
                dragDropState.checkTargetUnderFinger()
            }
            delay(16)
        }
    }

    // Reset rotation angle on track change
    LaunchedEffect(track.id) {
        discRotationAngle = 0f
    }

    // Continuous smooth rotation while playing
    // When paused, the loop terminates and discRotationAngle freezes in its current position
    LaunchedEffect(playerState.isPlaying, isDraggingVinyl) {
        if (playerState.isPlaying && !isDraggingVinyl) {
            var lastFrameNanos = 0L
            while (isActive) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameNanos != 0L) {
                        val dtSeconds = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                        // 30 degrees per second (smooth 12s full rotation)
                        val degreesPerSecond = 30f
                        discRotationAngle = (discRotationAngle + degreesPerSecond * dtSeconds) % 360f
                    }
                    lastFrameNanos = frameTimeNanos
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.bg,
        dragHandle = null,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    if (isQueueVisible) {
                        isQueueVisible = false
                    } else {
                        onDismissRequest()
                    }
                }) {
                    Icon(
                        imageVector = if (isQueueVisible) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isQueueVisible) "Назад к плееру" else "Свернуть",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = if (isQueueVisible) "ОЧЕРЕДЬ ТРЕКОВ" else "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textSecondary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onToggleFavorite(track) }) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) colors.accentPink else colors.textSecondary
                        )
                    }
                    IconButton(onClick = { isQueueVisible = !isQueueVisible }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Очередь воспроизведения",
                            tint = if (isQueueVisible) colors.accentPurple else colors.textSecondary
                        )
                    }
                }
            }

            if (isQueueVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp)
                ) {
                    // Queue Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Текущий список воспроизведения",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${playerState.playlist.size} ${getTrackCountPlural(playerState.playlist.size)} • Удерживайте трек или потяните ≡",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Now Playing Mini Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.accentPurple.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, colors.accentPurple.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val activeArtModel = track.coverArtPath?.let { File(it) } ?: track.thumbnailUrl
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (activeArtModel != null) {
                                    AsyncImage(
                                        model = activeArtModel,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = colors.accentPurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = colors.accentCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "СЕЙЧАС ИГРАЕТ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = colors.accentCyan
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = onPlayPauseClick,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.accentPurple)
                            ) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Queue tracks list
                    if (playerState.playlist.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Очередь воспроизведения пуста",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            state = queueListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .nestedScroll(dragDropNestedScrollConnection),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = playerState.playlist,
                                key = { _, item -> item.id }
                            ) { index, queueTrack ->
                                val isCurrent = queueTrack.id == track.id
                                val isDragging = dragDropState.draggingKey == queueTrack.id
                                val itemArt = queueTrack.coverArtPath?.let { File(it) } ?: queueTrack.thumbnailUrl

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        isDragging -> colors.surfaceVariant
                                        isCurrent -> colors.accentPurple.copy(alpha = 0.12f)
                                        else -> colors.surfaceVariant.copy(alpha = 0.45f)
                                    },
                                    border = BorderStroke(
                                        width = if (isDragging || isCurrent) 1.5.dp else 0.5.dp,
                                        color = when {
                                            isDragging -> colors.accentCyan
                                            isCurrent -> colors.accentPurple.copy(alpha = 0.6f)
                                            else -> colors.border.copy(alpha = 0.25f)
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(if (isDragging) 10f else 0f)
                                        .graphicsLayer {
                                            translationY = if (isDragging) dragDropState.draggingDeltaY else 0f
                                            scaleX = if (isDragging) 1.03f else 1f
                                            scaleY = if (isDragging) 1.03f else 1f
                                            shadowElevation = if (isDragging) 16.dp.toPx() else 0f
                                        }
                                        .then(if (!isDragging) Modifier.animateItem() else Modifier)
                                        .clickable(enabled = !dragDropState.isDragging) {
                                            onPlayQueueItem(index)
                                        }
                                        .pointerInput(queueTrack.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    dragDropState.onDragStart(index, queueTrack.id)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragDropState.onDrag(dragAmount.y)
                                                },
                                                onDragEnd = {
                                                    dragDropState.onDragEnd()
                                                },
                                                onDragCancel = {
                                                    dragDropState.onDragEnd()
                                                }
                                            )
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Number or Equalizer
                                        Box(
                                            modifier = Modifier.width(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Default.GraphicEq,
                                                    contentDescription = "Playing",
                                                    tint = colors.accentPurple,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                                    color = colors.textTertiary,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }

                                        // Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (itemArt != null) {
                                                AsyncImage(
                                                    model = itemArt,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = if (isCurrent) colors.accentPurple else colors.textTertiary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        // Title & Artist
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = queueTrack.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isCurrent) colors.accentPurple else colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${queueTrack.artist} • ${formatMs(queueTrack.durationMs)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Drag handle icon (touch and drag immediately)
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .pointerInput(queueTrack.id) {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            dragDropState.onDragStart(index, queueTrack.id)
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragDropState.onDrag(dragAmount.y)
                                                        },
                                                        onDragEnd = {
                                                            dragDropState.onDragEnd()
                                                        },
                                                        onDragCancel = {
                                                            dragDropState.onDragEnd()
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "Перетащить",
                                                tint = if (isDragging) colors.accentCyan else colors.textTertiary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        // Remove button
                                        IconButton(
                                            onClick = { onRemoveTrackFromQueue(index) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Удалить из очереди",
                                                tint = colors.textTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))

            // Large Glowing Vinyl Art Disc with tactile scratching & scrubbing
            val artModel = track.coverArtPath?.let { File(it) } ?: track.thumbnailUrl
            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // 1. The rotating vinyl disc
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = if (colors.isDark) 24.dp else 12.dp,
                            shape = CircleShape,
                            ambientColor = if (colors.isDark) Color.Black else colors.accentPurple.copy(alpha = 0.2f),
                            spotColor = if (colors.isDark) Color.Black else colors.accentPurple.copy(alpha = 0.25f)
                        )
                        .clip(CircleShape)
                        .border(
                            width = 4.dp,
                            brush = Brush.sweepGradient(listOf(colors.accentPurple, colors.accentCyan, colors.accentPink, colors.accentPurple)),
                            shape = CircleShape
                        )
                        .background(if (colors.isDark) colors.surfaceVariant else Color(0xFF1B191D))
                        .rotate(discRotationAngle)
                        .pointerInput(track.id, duration) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val dx = offset.x - centerX
                                    val dy = offset.y - centerY
                                    lastTouchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    isDraggingVinyl = true
                                    hasDraggedEnough = false
                                    totalRotatedAngle = 0f
                                    accumulatedDeltaMs = 0L
                                    val currentPos = currentPositionMsState
                                    dragStartPositionMs = currentPos
                                    scratchPositionMs = currentPos
                                    lastSeekTimestamp = 0L
                                    lastSoughtMs = -1L
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val dx = change.position.x - centerX
                                    val dy = change.position.y - centerY
                                    val dist = hypot(dx.toDouble(), dy.toDouble())
                                    if (dist > 15.0) {
                                        val currentAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        var deltaAngle = currentAngle - lastTouchAngle
                                        while (deltaAngle < -180f) deltaAngle += 360f
                                        while (deltaAngle > 180f) deltaAngle -= 360f

                                        discRotationAngle = (discRotationAngle + deltaAngle + 360f) % 360f
                                        lastTouchAngle = currentAngle
                                        totalRotatedAngle += deltaAngle

                                        if (abs(totalRotatedAngle) >= 1.5f) {
                                            hasDraggedEnough = true
                                        }

                                        if (hasDraggedEnough) {
                                            // 360 degrees rotation = 30 seconds of track scrub time
                                            val msPerDegree = 30_000f / 360f
                                            val deltaMs = (totalRotatedAngle * msPerDegree).toLong()
                                            scratchPositionMs = (dragStartPositionMs + deltaMs).coerceIn(0L, duration)
                                            accumulatedDeltaMs = scratchPositionMs - dragStartPositionMs

                                            // Real-time live seek throttled to 120ms
                                            val now = System.currentTimeMillis()
                                            if (now - lastSeekTimestamp >= 120L && abs(scratchPositionMs - lastSoughtMs) >= 250L) {
                                                lastSeekTimestamp = now
                                                lastSoughtMs = scratchPositionMs
                                                onSeekState(scratchPositionMs)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (isDraggingVinyl) {
                                        isDraggingVinyl = false
                                        if (hasDraggedEnough && scratchPositionMs != lastSoughtMs) {
                                            onSeekState(scratchPositionMs)
                                        }
                                    }
                                },
                                onDragCancel = {
                                    isDraggingVinyl = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (artModel != null) {
                        AsyncImage(
                            model = artModel,
                            contentDescription = track.album,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        // Vinyl grooves overlay for realism
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.15f))
                        )
                        // Center spindle hole
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.bg)
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                    } else {
                        // Vinyl grooves rings
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        )
                        // Center label
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(colors.accentPurple, if (colors.isDark) colors.surface else Color(0xFF2E2738)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // 2. Center HUD overlay when rotating (unrotated, upright, tactile scrub feedback)
                if (isDraggingVinyl && hasDraggedEnough) {
                    val deltaSeconds = (accumulatedDeltaMs / 1000)
                    val deltaSign = if (deltaSeconds >= 0) "+$deltaSeconds" else "$deltaSeconds"
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .shadow(16.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.82f))
                            .border(2.dp, colors.accentCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = formatMs(scratchPositionMs),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${deltaSign}s",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = if (deltaSeconds >= 0) colors.accentCyan else colors.accentPink
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Track info
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = colors.accentPurple,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // HTTP Range streaming badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant)
                    .border(1.dp, colors.border.copy(alpha = if (colors.isDark) 0.1f else 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = colors.accentCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HTTP Streaming • Cached LRU",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = colors.accentCyan
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Scrubber Slider
            val currentPos = when {
                isUserDraggingSlider -> (draggingSliderPosition * duration).toLong()
                isDraggingVinyl && hasDraggedEnough -> scratchPositionMs
                else -> playerState.currentPositionMs
            }
            val sliderValue = when {
                isUserDraggingSlider -> draggingSliderPosition
                isDraggingVinyl && hasDraggedEnough -> (scratchPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                else -> (playerState.currentPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }

            Slider(
                value = sliderValue,
                onValueChange = {
                    isUserDraggingSlider = true
                    draggingSliderPosition = it
                },
                onValueChangeFinished = {
                    isUserDraggingSlider = false
                    onSeek((draggingSliderPosition * duration).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = colors.accentPurple,
                    activeTrackColor = colors.accentPurple,
                    inactiveTrackColor = colors.border.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(currentPos),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isDraggingVinyl && hasDraggedEnough) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isDraggingVinyl && hasDraggedEnough) colors.accentCyan else colors.textSecondary
                )
                Text(
                    text = formatMs(duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main playback controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerState.shuffleEnabled) colors.accentPurple else colors.textTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = onSkipPreviousClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Large Play/Pause FAB
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(colors.accentPurple, colors.accentCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                // Next
                IconButton(
                    onClick = onSkipNextClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat
                IconButton(onClick = onCycleRepeat) {
                    val repeatIcon = when (playerState.repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    val repeatTint = when (playerState.repeatMode) {
                        Player.REPEAT_MODE_OFF -> colors.textTertiary
                        else -> colors.accentPurple
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = repeatTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Queue quick access pill
            Surface(
                onClick = { isQueueVisible = true },
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, colors.border.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = colors.accentPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Очередь: ${playerState.playlist.size} ${getTrackCountPlural(playerState.playlist.size)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val minutes = (ms / 1000) / 60
    val seconds = (ms / 1000) % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun getTrackCountPlural(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..19 -> "треков"
        mod10 == 1 -> "трек"
        mod10 in 2..4 -> "трека"
        else -> "треков"
    }
}
