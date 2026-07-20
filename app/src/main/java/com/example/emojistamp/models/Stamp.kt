package com.example.emojistamp.models

import androidx.compose.ui.geometry.Offset

/**
 * 画像上に配置されるスタンプのデータクラス。
 *
 * @param emoji 絵文字
 * @param offset 画像内での座標
 */
data class Stamp(
    val emoji: String,
    val offset: Offset
)
