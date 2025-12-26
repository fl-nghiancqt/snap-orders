package com.example.snaporder.core.model

/**
 * CartItem model representing an item in the shopping cart.
 * Contains reference to MenuItem and quantity.
 */
data class CartItem(
    val menuItemId: String,
    val menuItemName: String,
    val price: Double,
    val quantity: Int = 1
) {
    val totalPrice: Double
        get() = price * quantity
}

