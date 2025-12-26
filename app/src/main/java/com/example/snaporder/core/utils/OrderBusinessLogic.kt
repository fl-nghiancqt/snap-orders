package com.example.snaporder.core.utils

import com.example.snaporder.core.firestore.OrderRepository
import com.example.snaporder.core.model.CartItem
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.model.OrderItem
import com.example.snaporder.core.model.OrderStatus
import com.google.firebase.Timestamp
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core business logic helpers for order management.
 * 
 * BUSINESS RULES:
 * - One table can have ONLY ONE OPEN order
 * - OPEN statuses: CREATED, PREPARING
 * - CLOSED statuses: PAID, CANCELLED
 * - If table has OPEN order, new items must be appended
 * - Tables with OPEN orders must NOT be shown when creating new orders
 */
@Singleton
class OrderBusinessLogic @Inject constructor(
    private val orderRepository: OrderRepository
) {
    /**
     * Find an open order for a specific table number.
     * Returns null if no open order exists.
     */
    suspend fun findOpenOrderByTable(tableNumber: Int): Order? {
        return orderRepository.getOpenOrderByTable(tableNumber)
    }
    
    /**
     * Validate if a table is available for creating a new order.
     * A table is available if it has NO open orders.
     * 
     * @return true if table is available, false if it has an open order
     */
    suspend fun validateTableAvailable(tableNumber: Int): Boolean {
        val openOrder = findOpenOrderByTable(tableNumber)
        return openOrder == null
    }
    
    /**
     * Create a new order with cart items.
     * 
     * BUSINESS RULE: This should only be called if validateTableAvailable() returns true.
     */
    suspend fun createNewOrder(
        tableNumber: Int,
        cartItems: List<CartItem>,
        userId: String
    ): Result<String> {
        // Validate table is available
        if (!validateTableAvailable(tableNumber)) {
            return Result.failure(
                IllegalStateException("Table $tableNumber already has an open order")
            )
        }
        
        // Convert cart items to order items
        val orderItems = cartItems.map { cartItem ->
            OrderItem(
                menuItemId = cartItem.menuItemId,
                menuItemName = cartItem.menuItemName,
                price = cartItem.price,
                quantity = cartItem.quantity
            )
        }
        
        // Calculate total price
        val totalPrice = orderItems.sumOf { it.totalPrice }
        
        // Create order
        val order = Order(
            tableNumber = tableNumber,
            items = orderItems,
            totalPrice = totalPrice,
            status = OrderStatus.CREATED,
            createdAt = Timestamp.now(),
            userId = userId
        )
        
        return orderRepository.createOrder(order)
    }
    
    /**
     * Add more items to an existing open order.
     * 
     * BUSINESS RULE: This merges new items with existing items.
     * If the same menu item exists, quantities are combined.
     */
    suspend fun addMoreToExistingOrder(
        existingOrder: Order,
        newCartItems: List<CartItem>
    ): Result<Unit> {
        // Validate order is open
        if (!existingOrder.isOpen()) {
            return Result.failure(
                IllegalStateException("Cannot add items to a closed order")
            )
        }
        
        // Merge items
        val mergedItems = mergeOrderItems(existingOrder.items, newCartItems)
        
        // Calculate new total price
        val newTotalPrice = mergedItems.sumOf { it.totalPrice }
        
        // Update order
        val updatedOrder = existingOrder.copy(
            items = mergedItems,
            totalPrice = newTotalPrice
        )
        
        return orderRepository.updateOrder(updatedOrder)
    }
    
    /**
     * Merge old order items with new cart items.
     * If the same menu item exists in both, quantities are combined.
     * 
     * @param oldItems Existing order items
     * @param newItems New cart items to add
     * @return Merged list of order items
     */
    fun mergeOrderItems(
        oldItems: List<OrderItem>,
        newItems: List<CartItem>
    ): List<OrderItem> {
        // Convert old items to a mutable map for easy lookup
        val itemMap = oldItems.associateBy { it.menuItemId }.toMutableMap()
        
        // Add or merge new items
        newItems.forEach { cartItem ->
            val existingItem = itemMap[cartItem.menuItemId]
            if (existingItem != null) {
                // Merge: add quantities
                itemMap[cartItem.menuItemId] = existingItem.copy(
                    quantity = existingItem.quantity + cartItem.quantity
                )
            } else {
                // New item: add to map
                itemMap[cartItem.menuItemId] = OrderItem(
                    menuItemId = cartItem.menuItemId,
                    menuItemName = cartItem.menuItemName,
                    price = cartItem.price,
                    quantity = cartItem.quantity
                )
            }
        }
        
        return itemMap.values.toList()
    }
    
    /**
     * Get list of table numbers that have open orders.
     * Used to filter out unavailable tables when creating new orders.
     * 
     * NOTE: This is a helper function. In the UI, you should query orders
     * using OrderRepository.getAllOrders() Flow and filter for open orders.
     * This function is provided for convenience but may not be the most efficient
     * for large datasets. Consider maintaining a separate collection or cache
     * for better performance in production.
     */
    suspend fun getTablesWithOpenOrders(): List<Int> {
        // This would require collecting from Flow, which is better done in ViewModel.
        // For now, return empty list as placeholder.
        // In ViewModel, use: orderRepository.getAllOrders().collect { orders ->
        //     val openTables = orders.filter { it.isOpen() }.map { it.tableNumber }
        // }
        return emptyList()
    }
}

