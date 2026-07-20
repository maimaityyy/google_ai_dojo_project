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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
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
                // 状態のホイスティング
                var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
                var selectedEmoji by rememberSaveable { mutableStateOf<String?>(null) }
                
                // スタンプのリスト
                val stamps = remember { mutableStateListOf<Stamp>() }
                
                // ドラッグ中のスタンプのインデックス
                var draggedStampIndex by remember { mutableIntStateOf(-1) }
                
                // カメラで撮影した画像を一時的に保存するUriを保持
                var tempPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

                // 最も近いスタンプを探すヘルパー関数
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

                // Photo Pickerのランチャー
                val pickMedia = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia(),
                    onResult = { uri ->
                        if (uri != null) {
                            selectedImageUri = uri
                            stamps.clear()
                        }
                    }
                )

                // カメラ撮影のランチャー
                val takePicture = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture(),
                    onResult = { success ->
                        if (success && tempPhotoUri != null) {
                            selectedImageUri = tempPhotoUri
                            stamps.clear()
                        }
                    }
                )

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PhotoPickerScreen(
                        selectedImageUri = selectedImageUri,
                        selectedEmoji = selectedEmoji,
                        stamps = stamps,
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
                        onEmojiSelected = { emoji ->
                            selectedEmoji = emoji
                        },
                        onPhotoTapped = { offset ->
                            selectedEmoji?.let { emoji ->
                                stamps.add(Stamp(emoji, offset))
                            }
                        },
                        onPhotoLongTapped = { offset ->
                            val index = findNearestStampIndex(offset)
                            if (index != -1) {
                                stamps.removeAt(index)
                            }
                        },
                        onDragStart = { offset ->
                            draggedStampIndex = findNearestStampIndex(offset)
                        },
                        onDragged = { dragAmount ->
                            if (draggedStampIndex != -1 && draggedStampIndex < stamps.size) {
                                val currentStamp = stamps[draggedStampIndex]
                                stamps[draggedStampIndex] = currentStamp.copy(
                                    offset = currentStamp.offset + dragAmount
                                )
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
