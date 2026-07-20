package com.example.emojistamp

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.emojistamp.models.DrawPath
import com.example.emojistamp.models.Stamp
import com.example.emojistamp.ui.PhotoPickerScreen
import com.example.emojistamp.ui.theme.EmojiStampTheme
import java.io.File
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current

            EmojiStampTheme {
                // 画像Uri
                var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
                // 選択中の絵文字
                var selectedEmoji by rememberSaveable { mutableStateOf<String?>(null) }
                // スタンプリスト
                val stamps = remember { mutableStateListOf<Stamp>() }
                // ドラッグ中のスタンプ
                var draggedStampIndex by remember { mutableIntStateOf(-1) }
                
                // 手書き状態
                val drawPaths = remember { mutableStateListOf<DrawPath>() }
                var isDrawMode by rememberSaveable { mutableStateOf(false) }
                var isEraserMode by rememberSaveable { mutableStateOf(false) }
                var currentColor by remember { mutableStateOf(Color.Red) }
                var currentStrokeWidth by remember { mutableFloatStateOf(10f) }
                
                // 現在の表示上の画像サイズ（ピクセル）
                var imageWidthPx by remember { mutableFloatStateOf(0f) }
                var imageHeightPx by remember { mutableFloatStateOf(0f) }

                // カメラ用一時Uri
                var tempPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

                // 座標を画像領域内に収めるヘルパー
                fun clampOffset(offset: Offset): Offset {
                    if (imageWidthPx == 0f || imageHeightPx == 0f) return offset
                    return Offset(
                        x = offset.x.coerceIn(0f, imageWidthPx),
                        y = offset.y.coerceIn(0f, imageHeightPx)
                    )
                }

                // 最も近いスタンプを探す
                fun findNearestStampIndex(offset: Offset, maxDistance: Float = 100f): Int {
                    var minDistance = Float.MAX_VALUE
                    var nearestIndex = -1
                    stamps.forEachIndexed { index, stamp ->
                        val dx = stamp.offset.x - offset.x
                        val dy = stamp.offset.y - offset.y
                        val distance = sqrt(dx * dx + dy * dy)
                        if (distance < minDistance && distance < maxDistance) {
                            minDistance = distance
                            nearestIndex = index
                        }
                    }
                    return nearestIndex
                }

                // 消しゴム: 座標の近くにあるパスを削除
                fun erasePathsAt(offset: Offset) {
                    val threshold = 30f // 消去判定の距離
                    drawPaths.removeAll { path ->
                        path.points.any { point ->
                            val dx = point.x - offset.x
                            val dy = point.y - offset.y
                            sqrt(dx * dx + dy * dy) < threshold
                        }
                    }
                }

                // ランチャー設定
                val pickMedia = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia(),
                    onResult = { uri ->
                        if (uri != null) {
                            selectedImageUri = uri
                            stamps.clear()
                            drawPaths.clear()
                            imageWidthPx = 0f
                            imageHeightPx = 0f
                        }
                    }
                )

                val takePicture = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture(),
                    onResult = { success ->
                        if (success && tempPhotoUri != null) {
                            selectedImageUri = tempPhotoUri
                            stamps.clear()
                            drawPaths.clear()
                            imageWidthPx = 0f
                            imageHeightPx = 0f
                        }
                    }
                )

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PhotoPickerScreen(
                        selectedImageUri = selectedImageUri,
                        selectedEmoji = selectedEmoji,
                        stamps = stamps,
                        drawPaths = drawPaths,
                        isDrawMode = isDrawMode,
                        isEraserMode = isEraserMode,
                        currentColor = currentColor,
                        currentStrokeWidth = currentStrokeWidth,
                        onPickImage = {
                            pickMedia.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onCaptureImage = {
                            val tempFile = File.createTempFile(
                                "captured_image_",
                                ".jpg",
                                File(context.cacheDir, "images").apply { mkdirs() }
                            )
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                tempFile
                            )
                            tempPhotoUri = uri
                            takePicture.launch(uri)
                        },
                        onEmojiSelected = { emoji -> selectedEmoji = emoji },
                        onPhotoTapped = { offset ->
                            selectedEmoji?.let { emoji ->
                                stamps.add(Stamp(emoji, offset))
                            }
                        },
                        onPhotoLongTapped = { offset ->
                            val index = findNearestStampIndex(offset)
                            if (index != -1) stamps.removeAt(index)
                        },
                        onDragStart = { offset ->
                            if (isDrawMode && isEraserMode) {
                                erasePathsAt(offset)
                            } else if (!isDrawMode) {
                                draggedStampIndex = findNearestStampIndex(offset)
                            }
                        },
                        onDragged = { dragAmount ->
                            if (draggedStampIndex != -1 && draggedStampIndex < stamps.size) {
                                val currentStamp = stamps[draggedStampIndex]
                                stamps[draggedStampIndex] = currentStamp.copy(
                                    offset = clampOffset(currentStamp.offset + dragAmount)
                                )
                            }
                        },
                        onDrawStart = { offset ->
                            if (isEraserMode) {
                                erasePathsAt(offset)
                            } else {
                                drawPaths.add(
                                    DrawPath(
                                        points = listOf(clampOffset(offset)),
                                        color = currentColor,
                                        strokeWidth = currentStrokeWidth
                                    )
                                )
                            }
                        },
                        onDrawing = { offset ->
                            if (isEraserMode) {
                                erasePathsAt(offset)
                            } else if (drawPaths.isNotEmpty()) {
                                val lastPath = drawPaths.last()
                                drawPaths[drawPaths.size - 1] = lastPath.copy(
                                    points = lastPath.points + clampOffset(offset)
                                )
                            }
                        },
                        onToggleMode = { 
                            isDrawMode = !isDrawMode
                            if (!isDrawMode) isEraserMode = false
                        },
                        onToggleEraser = { isEraserMode = !isEraserMode },
                        onColorSelected = { 
                            currentColor = it
                            isEraserMode = false
                        },
                        onStrokeWidthChanged = { currentStrokeWidth = it },
                        onImageSizeChanged = { w, h ->
                            imageWidthPx = w
                            imageHeightPx = h
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
