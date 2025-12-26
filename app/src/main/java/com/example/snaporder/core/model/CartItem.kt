package com.example.snaporder.core.model

/**
 * CartItem model representing an item in the shopping cart.
 * 
 * This model is designed to be reusable for Firestore Order creation.
 * When creating an Order, CartItems will be converted to OrderItems.
 * 
 * ARCHITECTURE:
 * - id: Unique identifier for the cart item (can be UUID)
 * - menuItemId: Reference to the MenuItem this cart item represents
 * - name: Display name (copied from MenuItem for performance)
 * - price: Unit price in VND (Int for precision)
 * - quantity: Number of items
 */
data class CartItem(
    val id: String = "",
    val menuItemId: String = "",
    val name: String = "",
    val price: Int = 0, // Price in VND (Int for precision, no decimals)
    val quantity: Int = 1
) {
    /**
     * Calculate subtotal for this cart item.
     */
    val subtotal: Int
        get() = price * quantity
}
