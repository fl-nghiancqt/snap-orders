package com.example.snaporder.core.model

/**
 * MenuItem model representing a food/drink item in the menu.
 */
data class MenuItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val available: Boolean = true,
    val imageUrl: String = "" // Optional image URL for Coil
)

