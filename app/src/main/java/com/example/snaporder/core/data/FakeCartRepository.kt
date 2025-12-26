package com.example.snaporder.core.data

import com.example.snaporder.core.model.CartItem
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of CartRepository for UI development and testing.
 * 
 * SAMPLE DATA (as per requirements):
 * - Trà đào – 30,000 x 2
 * - Bạc xỉu – 35,000 x 1
 * - Cà phê sữa – 25,000 x 3
 * 
 * This will be replaced by a real Firestore implementation later.
 * The UI and ViewModel will NOT need to change when swapping implementations.
 */
@Singleton
class FakeCartRepository @Inject constructor() : CartRepository {
    
    override suspend fun getCartItems(): List<CartItem> {
        delay(500) // Simulate network delay
        
        return listOf(
            CartItem(
                id = "cart_item_1",
                menuItemId = "1",
                name = "Trà đào",
                price = 30000,
                quantity = 2
            ),
            CartItem(
                id = "cart_item_2",
                menuItemId = "2",
                name = "Bạc xỉu",
                price = 35000,
                quantity = 1
            ),
            CartItem(
                id = "cart_item_3",
                menuItemId = "3",
                name = "Cà phê sữa",
                price = 25000,
                quantity = 3
            )
        )
    }
}

