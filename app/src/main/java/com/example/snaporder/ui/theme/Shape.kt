package com.example.snaporder.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * SnapOrders Shape System
 * 
 * Design Philosophy: Rounded, soft corners for modern feel
 * - Buttons: 14.dp corners (prominent, friendly)
 * - Cards: 18.dp corners (elegant, premium)
 * - Soft elevation for depth without harsh shadows
 */
val SnapOrdersShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp), // Buttons
    large = RoundedCornerShape(18.dp),  // Cards
    extraLarge = RoundedCornerShape(28.dp)
)

