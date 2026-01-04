package com.example.snaporder.core.data

import com.example.snaporder.core.model.CartItem
import kotlinx.coroutines.flow.Flow

/**
 * Interface for cart data operations.
 * 
 * ARCHITECTURE:
 * This abstraction allows swapping between different implementations:
 * - InMemoryCartRepository: For in-memory cart management (current)
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
    
    /**
     * Get cart items as a Flow for real-time updates.
     * 
     * @return Flow that emits cart items whenever they change
     */
    fun getCartItemsFlow(): Flow<List<CartItem>>
    
    /**
     * Add an item to the cart.
     * If the item already exists, increment its quantity.
     * 
     * @param menuItemId The ID of the menu item to add
     * @param name The name of the menu item
     * @param price The price of the menu item
     * @return The updated cart item
     */
    suspend fun addItem(menuItemId: String, name: String, price: Double): CartItem
    
    /**
     * Update the quantity of a cart item.
     * 
     * @param cartItemId The ID of the cart item
     * @param quantity The new quantity (must be > 0)
     */
    suspend fun updateQuantity(cartItemId: String, quantity: Int)
    
    /**
     * Remove an item from the cart.
     * 
     * @param cartItemId The ID of the cart item to remove
     */
    suspend fun removeItem(cartItemId: String)
    
    /**
     * Clear all items from the cart.
     */
    suspend fun clearCart()
}

