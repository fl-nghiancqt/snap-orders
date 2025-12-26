package com.example.snaporder.core.model

/**
 * OrderDraft model representing a draft order before placement.
 * 
 * This model is designed to be reusable for Firestore Order creation.
 * When placing an order, OrderDraft will be converted to Order.
 * 
 * ARCHITECTURE:
 * - tableNumber: Table number (null if not selected yet)
 * - items: List of cart items
 * - subtotal: Sum of all item subtotals
 * - serviceFee: Service fee (fixed for now, can be calculated later)
 * - totalPrice: Final total (subtotal + serviceFee)
 * 
 * BUSINESS LOGIC:
 * - All calculations are done in the ViewModel
 * - This is a pure data model
 * - Can be easily converted to Firestore Order document
 */
data class OrderDraft(
    val tableNumber: Int? = null,
    val items: List<CartItem> = emptyList(),
    val subtotal: Int = 0,
    val serviceFee: Int = 0,
    val totalPrice: Int = 0
) {
    /**
     * Check if the order draft is valid for placement.
     * - Must have at least one item
     * - Must have a table number
     */
    val isValid: Boolean
        get() = items.isNotEmpty() && tableNumber != null
}

