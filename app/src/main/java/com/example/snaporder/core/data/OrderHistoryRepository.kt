package com.example.snaporder.core.data

import com.example.snaporder.core.model.Order

/**
 * Interface for order history data operations.
 * 
 * ARCHITECTURE:
 * This abstraction allows swapping between different implementations:
 * - FakeOrderHistoryRepository: For UI development and testing
 * - FirestoreOrderHistoryRepository: For real Firestore integration (future)
 * 
 * The UI and ViewModel will NOT change when swapping implementations.
 */
interface OrderHistoryRepository {
    /**
     * Get all orders for the current user.
     * In a real implementation, this would fetch from Firestore
     * filtered by the logged-in user's ID.
     * 
     * @return List of orders for the current user
     */
    suspend fun getOrdersByUser(): List<Order>
}

