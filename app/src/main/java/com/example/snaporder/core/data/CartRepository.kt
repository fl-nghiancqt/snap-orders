package com.example.snaporder.core.data

import com.example.snaporder.core.model.CartItem

/**
 * Interface for cart data operations.
 * 
 * ARCHITECTURE:
 * This abstraction allows swapping between different implementations:
 * - FakeCartRepository: For UI development and testing
 * - FirestoreCartRepository: For real Firestore integration (future)
 * 
 * The UI and ViewModel will NOT change when swapping implementations.
 */
interface CartRepository {
    /**
     * Get all cart items.
     * In a real implementation, this would fetch from Firestore or local storage.
     * 
     * @return List of cart items
     */
    suspend fun getCartItems(): List<CartItem>
}

