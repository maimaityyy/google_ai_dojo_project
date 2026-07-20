package com.example.emojistamp.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * 描画された1本の線を表現するデータクラス。
 *
 * @param points 線を構成する点のリスト
 * @param color 線の色
 * @param strokeWidth 線の太さ
 */
data class DrawPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)
