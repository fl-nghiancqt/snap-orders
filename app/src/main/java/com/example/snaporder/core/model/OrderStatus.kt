package com.example.snaporder.core.model

/**
 * OrderStatus enum representing the state of an order.
 * 
 * BUSINESS RULES:
 * - OPEN statuses: CREATED, PREPARING (table cannot have new order)
 * - CLOSED statuses: PAID, CANCELLED (table can have new order)
 */
enum class OrderStatus {
    CREATED,      // Order created, waiting to be prepared
    PREPARING,    // Order is being prepared
    PAID,         // Order completed and paid
    CANCELLED;    // Order cancelled
    
    /**
     * Check if this status represents an OPEN order.
     * OPEN orders prevent new orders for the same table.
     */
    fun isOpen(): Boolean {
        return this == CREATED || this == PREPARING
    }
    
    /**
     * Check if this status represents a CLOSED order.
     * CLOSED orders allow new orders for the same table.
     */
    fun isClosed(): Boolean {
        return this == PAID || this == CANCELLED
    }
}

