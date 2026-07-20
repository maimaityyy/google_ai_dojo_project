package com.example.emojistamp.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.example.emojistamp.models.DrawPath
import com.example.emojistamp.models.Stamp
import kotlin.math.roundToInt

/**
 * 画像選択・表示、スタンプ配置、手書き描画を行う画面。
 */
@Composable
fun PhotoPickerScreen(
    selectedImageUri: Uri?,
    selectedEmoji: String?,
    stamps: List<Stamp>,
    drawPaths: List<DrawPath>,
    isDrawMode: Boolean,
    isEraserMode: Boolean,
    currentColor: Color,
    currentStrokeWidth: Float,
    onPickImage: () -> Unit,
    onCaptureImage: () -> Unit,
    onEmojiSelected: (String) -> Unit,
    onPhotoTapped: (Offset) -> Unit,
    onPhotoLongTapped: (Offset) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragged: (Offset) -> Unit,
    onDrawStart: (Offset) -> Unit,
    onDrawing: (Offset) -> Unit,
    onToggleMode: () -> Unit,
    onToggleEraser: () -> Unit,
    onColorSelected: (Color) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onImageSizeChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val containerSizePx = with(density) { 300.dp.toPx() }
    
    // 表示されている画像のサイズを計算（初期値はコンテナサイズ）
    var displaySize by remember(selectedImageUri) { 
        mutableStateOf(Size(containerSizePx, containerSizePx)) 
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // モード切替ボタン
        ModeToggle(
            isDrawMode = isDrawMode,
            onToggleMode = onToggleMode,
            modifier = Modifier.padding(top = 16.dp)
        )

        // メインコンテンツ（中央）
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (selectedImageUri != null) {
                // 画像、スタンプ、手書きのレイヤー
                // Boxのサイズを画像の表示サイズに合わせる
                Box(
                    modifier = Modifier
                        .size(
                            width = with(density) { displaySize.width.toDp() },
                            height = with(density) { displaySize.height.toDp() }
                        )
                        .clipToBounds()
                        .pointerInput(isDrawMode, isEraserMode) {
                            if (isDrawMode) {
                                detectDragGestures(
                                    onDragStart = { offset -> onDrawStart(offset) },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        onDrawing(change.position)
                                    }
                                )
                            } else {
                                detectTapGestures(
                                    onTap = { offset -> onPhotoTapped(offset) },
                                    onLongPress = { offset -> onPhotoLongTapped(offset) }
                                )
                            }
                        }
                        .pointerInput(isDrawMode) {
                            if (!isDrawMode) {
                                detectDragGestures(
                                    onDragStart = { offset -> onDragStart(offset) },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDragged(dragAmount)
                                    }
                                )
                            }
                        }
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        onState = { state ->
                            if (state is AsyncImagePainter.State.Success) {
                                val intrinsicSize = state.painter.intrinsicSize
                                if (intrinsicSize != Size.Unspecified && intrinsicSize.width > 0 && intrinsicSize.height > 0) {
                                    val ratio = intrinsicSize.width / intrinsicSize.height
                                    val (w, h) = if (ratio > 1f) {
                                        // 横長
                                        containerSizePx to (containerSizePx / ratio)
                                    } else {
                                        // 縦長
                                        (containerSizePx * ratio) to containerSizePx
                                    }
                                    displaySize = Size(w, h)
                                    onImageSizeChanged(w, h)
                                }
                            }
                        }
                    )

                    // 手書き描画レイヤー
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawPaths.forEach { path ->
                            if (path.points.size > 1) {
                                val drawPath = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(path.points.first().x, path.points.first().y)
                                    path.points.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(
                                    path = drawPath,
                                    color = path.color,
                                    style = Stroke(
                                        width = path.strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }

                    // スタンプの表示
                    stamps.forEach { stamp ->
                        Text(
                            text = stamp.emoji,
                            fontSize = 40.sp,
                            modifier = Modifier
                                .offset {
                                    IntOffset(stamp.offset.x.roundToInt(), stamp.offset.y.roundToInt())
                                }
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    layout(placeable.width, placeable.height) {
                                        placeable.placeRelative(
                                            -placeable.width / 2,
                                            -placeable.height / 2
                                        )
                                    }
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = onPickImage) {
                        Text(text = "画像を変更")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = onCaptureImage) {
                        Text(text = "カメラで撮影")
                    }
                }
            } else {
                Text(
                    text = "画像が選択されていません",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row {
                    Button(onClick = onPickImage) {
                        Text(text = "画像を選択")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = onCaptureImage) {
                        Text(text = "カメラで撮影")
                    }
                }
            }
        }

        // 下部設定パネル
        if (isDrawMode) {
            PenSettingsPanel(
                currentColor = currentColor,
                currentStrokeWidth = currentStrokeWidth,
                isEraserMode = isEraserMode,
                onToggleEraser = onToggleEraser,
                onColorSelected = onColorSelected,
                onStrokeWidthChanged = onStrokeWidthChanged,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            EmojiPalette(
                selectedEmoji = selectedEmoji,
                onEmojiSelected = onEmojiSelected,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ModeToggle(
    isDrawMode: Boolean,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val stampColor = if (!isDrawMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        val penColor = if (isDrawMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

        Surface(
            onClick = { if (isDrawMode) onToggleMode() },
            color = stampColor,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.height(40.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("スタンプ", fontSize = 14.sp)
            }
        }

        Surface(
            onClick = { if (!isDrawMode) onToggleMode() },
            color = penColor,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.height(40.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ペン", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun PenSettingsPanel(
    currentColor: Color,
    currentStrokeWidth: Float,
    isEraserMode: Boolean,
    onToggleEraser: () -> Unit,
    onColorSelected: (Color) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Black, Color.Yellow, Color.Magenta)

    Surface(
        modifier = modifier,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // カラーセレクター
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isEraserMode) color.copy(alpha = 0.3f) else color)
                                .border(
                                    width = if (!isEraserMode && currentColor == color) 2.dp else 0.dp,
                                    color = if (!isEraserMode && currentColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable(enabled = !isEraserMode) { onColorSelected(color) }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 消しゴムボタン
                IconButton(
                    onClick = onToggleEraser,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                ) {
                    Icon(
                        Icons.Default.AutoFixNormal,
                        contentDescription = "Eraser",
                        tint = if (isEraserMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // 太さ調整スライダー
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("太さ", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(16.dp))
                Slider(
                    value = currentStrokeWidth,
                    onValueChange = onStrokeWidthChanged,
                    valueRange = 2f..40f,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(currentStrokeWidth.dp.coerceAtMost(32.dp))
                        .clip(CircleShape)
                        .background(if (isEraserMode) MaterialTheme.colorScheme.outline else currentColor)
                )
            }
        }
    }
}

@Composable
fun EmojiPalette(
    selectedEmoji: String?,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val emojis = listOf("😀", "🎉", "❤️", "⭐", "🐱", "🔥")

    Surface(
        modifier = modifier,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(emojis) { emoji ->
                val isSelected = emoji == selectedEmoji
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .padding(4.dp)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 32.sp)
                }
            }
        }
    }
}
