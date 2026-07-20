package com.example.emojistamp.ui

import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.emojistamp.models.Stamp
import kotlin.math.roundToInt

/**
 * 状態を持たない画像選択・表示画面の Composable。
 */
@Composable
fun PhotoPickerScreen(
    selectedImageUri: Uri?,
    selectedEmoji: String?,
    stamps: List<Stamp>,
    onPickImage: () -> Unit,
    onCaptureImage: () -> Unit,
    onEmojiSelected: (String) -> Unit,
    onPhotoTapped: (Offset) -> Unit,
    onPhotoLongTapped: (Offset) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragged: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                // 画像とスタンプのレイヤー
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset -> onPhotoTapped(offset) },
                                onLongPress = { offset -> onPhotoLongTapped(offset) }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> onDragStart(offset) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDragged(dragAmount)
                                }
                            )
                        }
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
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
                
                Spacer(modifier = Modifier.height(24.dp))
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

        // 絵文字パレット（下部）
        EmojiPalette(
            selectedEmoji = selectedEmoji,
            onEmojiSelected = onEmojiSelected,
            modifier = Modifier.fillMaxWidth()
        )
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
