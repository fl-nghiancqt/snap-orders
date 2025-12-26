package com.example.snaporder.core.model

import com.google.firebase.Timestamp

/**
 * Order model representing a customer order.
 * 
 * BUSINESS RULES:
 * - One table can have ONLY ONE OPEN order at a time
 * - OPEN statuses: CREATED, PREPARING
 * - CLOSED statuses: PAID, CANCELLED
 */
data class Order(
    val id: String = "",
    val tableNumber: Int = 0,
    val items: List<OrderItem> = emptyList(),
    val totalPrice: Double = 0.0,
    val status: OrderStatus = OrderStatus.CREATED,
    val createdAt: Timestamp? = null,
    val userId: String = "" // User who created the order
) {
    /**
     * Check if this order is currently OPEN.
     */
    fun isOpen(): Boolean = status.isOpen()
    
    /**
     * Check if this order is CLOSED.
     */
    fun isClosed(): Boolean = status.isClosed()
}

