package com.example.snaporder.core.utils

import android.util.Log
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
     * 
     * VALIDATION:
     * - Cart items must not be empty
     * - Table must be available (no open orders)
     */
    suspend fun createNewOrder(
        tableNumber: Int,
        cartItems: List<CartItem>,
        userId: String
    ): Result<String> {
        Log.d("OrderBusinessLogic", "createNewOrder: Called - table=$tableNumber, items=${cartItems.size}, userId='$userId'")
        
        // Validate cart items are not empty
        if (cartItems.isEmpty()) {
            Log.e("OrderBusinessLogic", "createNewOrder: Cart items are empty")
            return Result.failure(
                IllegalArgumentException("Cannot create order with empty cart. Please add items to cart first.")
            )
        }
        
        // Validate table is available
        if (!validateTableAvailable(tableNumber)) {
            Log.w("OrderBusinessLogic", "createNewOrder: Table $tableNumber already has an open order")
            return Result.failure(
                IllegalStateException("Table $tableNumber already has an open order")
            )
        }
        
        Log.d("OrderBusinessLogic", "createNewOrder: Converting cart items to order items")
        // Convert cart items to order items
        val orderItems = cartItems.mapIndexed { index, cartItem ->
            Log.d("OrderBusinessLogic", "createNewOrder: Converting cart item $index:")
            Log.d("OrderBusinessLogic", "createNewOrder:   cartItem.id: '${cartItem.id}'")
            Log.d("OrderBusinessLogic", "createNewOrder:   cartItem.menuItemId: '${cartItem.menuItemId}'")
            Log.d("OrderBusinessLogic", "createNewOrder:   cartItem.name: '${cartItem.name}'")
            Log.d("OrderBusinessLogic", "createNewOrder:   cartItem.price: ${cartItem.price} (type: ${cartItem.price.javaClass.simpleName})")
            Log.d("OrderBusinessLogic", "createNewOrder:   cartItem.quantity: ${cartItem.quantity}")
            
            val orderItem = OrderItem(
                menuItemId = cartItem.menuItemId,
                menuItemName = cartItem.name,
                price = cartItem.price.toDouble(),
                quantity = cartItem.quantity
            )
            
            Log.d("OrderBusinessLogic", "createNewOrder:   → orderItem.price: ${orderItem.price} (type: ${orderItem.price.javaClass.simpleName})")
            Log.d("OrderBusinessLogic", "createNewOrder:   → orderItem.totalPrice: ${orderItem.totalPrice}")
            
            orderItem
        }
        
        Log.d("OrderBusinessLogic", "createNewOrder: Converted to ${orderItems.size} order items")
        
        // Validate order items are not empty (double check)
        if (orderItems.isEmpty()) {
            Log.e("OrderBusinessLogic", "createNewOrder: Order items are empty after conversion")
            return Result.failure(
                IllegalArgumentException("Cannot create order with no items")
            )
        }
        
        // Calculate subtotal and service fee
        val subtotal = orderItems.sumOf { it.totalPrice }
        val serviceFee = 10000.0 // Fixed service fee
        val totalPrice = subtotal + serviceFee
        
        Log.d("OrderBusinessLogic", "createNewOrder: Calculated totals - subtotal=$subtotal, serviceFee=$serviceFee, total=$totalPrice")
        
        // Create order with status CREATED (initial processing status)
        val order = Order(
            tableNumber = tableNumber,
            items = orderItems,
            totalPrice = totalPrice,
            status = OrderStatus.CREATED,
            createdAt = Timestamp.now(),
            userId = userId
        )
        
        Log.d("OrderBusinessLogic", "createNewOrder: Order object created - table=${order.tableNumber}, items=${order.items.size}, total=${order.totalPrice}, status=${order.status}")
        
        // Save order to Firestore
        Log.d("OrderBusinessLogic", "createNewOrder: Calling orderRepository.createOrder()")
        val result = orderRepository.createOrder(order)
        
        result.fold(
            onSuccess = { orderId ->
                Log.i("OrderBusinessLogic", "createNewOrder: Order saved to Firestore successfully - id='$orderId'")
            },
            onFailure = { error ->
                Log.e("OrderBusinessLogic", "createNewOrder: Failed to save order to Firestore", error)
            }
        )
        
        return result
    }
    
    /**
     * Check if an order can accept new items.
     * Orders can accept items when status is CREATED or PREPARING.
     * Orders with status PAID or CANCELLED cannot accept new items.
     * 
     * @param order The order to check
     * @return true if order can accept new items, false otherwise
     */
    fun canAddItemsToOrder(order: Order): Boolean {
        return order.status == OrderStatus.CREATED || order.status == OrderStatus.PREPARING
    }
    
    /**
     * Add more items to an existing open order.
     * 
     * BUSINESS RULE: 
     * - Can add items when status is CREATED or PREPARING
     * - Cannot add items when status is PAID or CANCELLED
     * - Merges new items with existing items
     * - If the same menu item exists, quantities are combined
     * 
     * VALIDATION:
     * - New cart items must not be empty
     */
    suspend fun addMoreToExistingOrder(
        existingOrder: Order,
        newCartItems: List<CartItem>
    ): Result<Unit> {
        Log.d("OrderBusinessLogic", "addMoreToExistingOrder: Called - orderId='${existingOrder.id}', newItems=${newCartItems.size}")
        
        // Validate new cart items are not empty
        if (newCartItems.isEmpty()) {
            Log.e("OrderBusinessLogic", "addMoreToExistingOrder: New cart items are empty")
            return Result.failure(
                IllegalArgumentException("Cannot add empty items to order. Please add items to cart first.")
            )
        }
        
        // Validate order can accept new items
        if (!canAddItemsToOrder(existingOrder)) {
            Log.e("OrderBusinessLogic", "addMoreToExistingOrder: Cannot add items to order with status ${existingOrder.status}")
            return Result.failure(
                IllegalStateException("Cannot add items to order with status ${existingOrder.status}. Order must be CREATED or PREPARING.")
            )
        }
        
        Log.d("OrderBusinessLogic", "addMoreToExistingOrder: Merging items...")
        // Merge items
        val mergedItems = mergeOrderItems(existingOrder.items, newCartItems)
        
        Log.d("OrderBusinessLogic", "addMoreToExistingOrder: Merged to ${mergedItems.size} items")
        
        // Validate merged items are not empty
        if (mergedItems.isEmpty()) {
            Log.e("OrderBusinessLogic", "addMoreToExistingOrder: Merged items are empty")
            return Result.failure(
                IllegalArgumentException("Cannot update order with no items")
            )
        }
        
        // Calculate new subtotal and service fee
        val subtotal = mergedItems.sumOf { it.totalPrice }
        val serviceFee = 10000.0 // Fixed service fee
        val newTotalPrice = subtotal + serviceFee
        
        Log.d("OrderBusinessLogic", "addMoreToExistingOrder: Calculated totals - subtotal=$subtotal, serviceFee=$serviceFee, total=$newTotalPrice")
        
        // Update order in Firestore
        val updatedOrder = existingOrder.copy(
            items = mergedItems,
            totalPrice = newTotalPrice
        )
        
        Log.d("OrderBusinessLogic", "addMoreToExistingOrder: Calling orderRepository.updateOrder()")
        val result = orderRepository.updateOrder(updatedOrder)
        
        result.fold(
            onSuccess = {
                Log.i("OrderBusinessLogic", "addMoreToExistingOrder: Order updated successfully in Firestore")
            },
            onFailure = { error ->
                Log.e("OrderBusinessLogic", "addMoreToExistingOrder: Failed to update order in Firestore", error)
            }
        )
        
        return result
    }
    
    /**
     * Mark an order as paid (completed).
     * This changes the order status to PAID, allowing the table to have a new order.
     * 
     * @param orderId The ID of the order to mark as paid
     * @return Result indicating success or failure
     */
    suspend fun markOrderAsPaid(orderId: String): Result<Unit> {
        return orderRepository.updateOrderStatus(orderId, OrderStatus.PAID)
    }
    
    /**
     * Process order placement: create new order or add to existing order.
     * 
     * BUSINESS RULES:
     * - If table has no open order: create new order with status CREATED
     * - If table has open order (CREATED or PREPARING): add items to existing order
     * - If table has PAID order: create new order (table is available)
     * 
     * VALIDATION:
     * - Cart items must not be empty
     * 
     * @param tableNumber The table number
     * @param cartItems The cart items to add
     * @param userId The user ID creating the order
     * @return Result containing order ID (for new orders) or Unit (for updated orders)
     */
    suspend fun processOrderPlacement(
        tableNumber: Int,
        cartItems: List<CartItem>,
        userId: String
    ): Result<OrderPlacementResult> {
        Log.d("OrderBusinessLogic", "processOrderPlacement: Called - table=$tableNumber, items=${cartItems.size}, userId='$userId'")
        
        // Validate cart items are not empty
        if (cartItems.isEmpty()) {
            Log.e("OrderBusinessLogic", "processOrderPlacement: Cart items are empty")
            return Result.failure(
                IllegalArgumentException("Cannot place order with empty cart. Please add items to cart first.")
            )
        }
        
        // Check for existing open order
        Log.d("OrderBusinessLogic", "processOrderPlacement: Checking for existing open order on table $tableNumber")
        val existingOrder = findOpenOrderByTable(tableNumber)
        
        return if (existingOrder != null) {
            Log.d("OrderBusinessLogic", "processOrderPlacement: Found existing order - id='${existingOrder.id}', status=${existingOrder.status}")
            // Table has open order - add items to existing order
            if (!canAddItemsToOrder(existingOrder)) {
                Log.w("OrderBusinessLogic", "processOrderPlacement: Cannot add items to order with status ${existingOrder.status}")
                Result.failure(
                    IllegalStateException("Table $tableNumber has an order with status ${existingOrder.status}. Please mark the order as paid before creating a new order.")
                )
            } else {
                Log.d("OrderBusinessLogic", "processOrderPlacement: Adding items to existing order")
                addMoreToExistingOrder(existingOrder, cartItems).map {
                    Log.i("OrderBusinessLogic", "processOrderPlacement: Items added to existing order successfully")
                    OrderPlacementResult.Updated(existingOrder.id)
                }
            }
        } else {
            Log.d("OrderBusinessLogic", "processOrderPlacement: No existing order found, creating new order")
            // No open order - create new order and save to Firestore
            createNewOrder(tableNumber, cartItems, userId).map { orderId ->
                Log.i("OrderBusinessLogic", "processOrderPlacement: New order created successfully - id='$orderId'")
                OrderPlacementResult.Created(orderId)
            }
        }
    }
    
    /**
     * Result of order placement operation.
     */
    sealed class OrderPlacementResult {
        data class Created(val orderId: String) : OrderPlacementResult()
        data class Updated(val orderId: String) : OrderPlacementResult()
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
                    menuItemName = cartItem.name,
                    price = cartItem.price.toDouble(),
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

