package com.example.snaporder.core.model

/**
 * OrderItem model representing an item in an order.
 * Similar to CartItem but persisted in Firestore.
 */
data class OrderItem(
    val menuItemId: String,
    val menuItemName: String,
    val price: Double,
    val quantity: Int = 1
) {
    val totalPrice: Double
        get() = price * quantity
}

