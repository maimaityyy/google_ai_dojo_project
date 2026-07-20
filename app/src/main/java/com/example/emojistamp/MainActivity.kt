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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.emojistamp.ui.PhotoPickerScreen
import com.example.emojistamp.ui.theme.EmojiStampTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            EmojiStampTheme {
                // 画像のUriをホイスティングして保持する
                var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
                
                // カメラで撮影した画像を一時的に保存するUriを保持（画面回転等に対応）
                var tempPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

                // Photo Pickerのランチャー
                val pickMedia = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia(),
                    onResult = { uri ->
                        if (uri != null) {
                            selectedImageUri = uri
                        }
                    }
                )

                // カメラ撮影のランチャー
                val takePicture = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture(),
                    onResult = { success ->
                        if (success && tempPhotoUri != null) {
                            selectedImageUri = tempPhotoUri
                        }
                    }
                )

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PhotoPickerScreen(
                        selectedImageUri = selectedImageUri,
                        onPickImage = {
                            pickMedia.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onCaptureImage = {
                            // 撮影用の一時ファイルを作成
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
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
