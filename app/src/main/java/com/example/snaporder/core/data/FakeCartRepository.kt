package com.example.snaporder.core.data

import com.example.snaporder.core.model.CartItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory implementation of CartRepository.
 * 
 * This maintains cart state in memory and provides real-time updates via Flow.
 * Items added from MenuScreen will be visible in CartScreen immediately.
 * 
 * ARCHITECTURE:
 * - Uses MutableStateFlow for reactive cart updates
 * - All ViewModels observing the cart will get updates automatically
 * - Thread-safe operations
 * 
 * FUTURE: Can be replaced with FirestoreCartRepository without changing ViewModels.
 */
@Singleton
class FakeCartRepository @Inject constructor() : CartRepository {
    
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    
    override suspend fun getCartItems(): List<CartItem> {
        delay(100) // Simulate small delay
        return _cartItems.value
    }
    
    override fun getCartItemsFlow(): kotlinx.coroutines.flow.Flow<List<CartItem>> {
        return _cartItems.asStateFlow()
    }
    
    override suspend fun addItem(menuItemId: String, name: String, price: Double): CartItem {
        delay(50) // Simulate small delay
        
        val currentItems = _cartItems.value.toMutableList()
        
        // Check if item already exists in cart
        val existingItemIndex = currentItems.indexOfFirst { it.menuItemId == menuItemId }
        
        val updatedItem = if (existingItemIndex >= 0) {
            // Item exists - increment quantity
            val existingItem = currentItems[existingItemIndex]
            existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            // New item - add to cart
            CartItem(
                id = UUID.randomUUID().toString(),
                menuItemId = menuItemId,
                name = name,
                price = price.toInt(),
                quantity = 1
            )
        }
        
        if (existingItemIndex >= 0) {
            currentItems[existingItemIndex] = updatedItem
        } else {
            currentItems.add(updatedItem)
        }
        
        _cartItems.value = currentItems
        return updatedItem
    }
    
    override suspend fun updateQuantity(cartItemId: String, quantity: Int) {
        delay(50)
        
        if (quantity <= 0) {
            removeItem(cartItemId)
            return
        }
        
        val currentItems = _cartItems.value.toMutableList()
        val itemIndex = currentItems.indexOfFirst { it.id == cartItemId }
        
        if (itemIndex >= 0) {
            currentItems[itemIndex] = currentItems[itemIndex].copy(quantity = quantity)
            _cartItems.value = currentItems
        }
    }
    
    override suspend fun removeItem(cartItemId: String) {
        delay(50)
        
        val currentItems = _cartItems.value.toMutableList()
        currentItems.removeAll { it.id == cartItemId }
        _cartItems.value = currentItems
    }
    
    override suspend fun clearCart() {
        delay(50)
        _cartItems.value = emptyList()
    }
}

