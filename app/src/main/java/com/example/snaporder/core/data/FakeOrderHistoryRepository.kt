package com.example.snaporder.core.data

import com.example.snaporder.core.model.Order
import com.example.snaporder.core.model.OrderItem
import com.example.snaporder.core.model.OrderStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of OrderHistoryRepository for UI development and testing.
 * 
 * SAMPLE DATA (as per requirements):
 * 1. Order ID: ORD-001
 *    Table: 5
 *    Status: PAID
 *    Total: 105,000
 *    CreatedAt: Yesterday
 * 
 * 2. Order ID: ORD-002
 *    Table: 3
 *    Status: SERVED
 *    Total: 75,000
 *    CreatedAt: Today
 * 
 * 3. Order ID: ORD-003
 *    Table: 8
 *    Status: PREPARING
 *    Total: 140,000
 *    CreatedAt: Today
 * 
 * This will be replaced by a real Firestore implementation later.
 * The UI and ViewModel will NOT need to change when swapping implementations.
 */
@Singleton
class FakeOrderHistoryRepository @Inject constructor() : OrderHistoryRepository {
    
    override suspend fun getOrdersByUser(): List<Order> {
        delay(800) // Simulate network delay
        
        val calendar = Calendar.getInstance()
        
        // Order 1: Yesterday, PAID
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayTimestamp = Timestamp(calendar.timeInMillis / 1000, 0)
        
        // Order 2: Today, SERVED (2 hours ago)
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.HOUR_OF_DAY, -2)
        val todayMorningTimestamp = Timestamp(calendar.timeInMillis / 1000, 0)
        
        // Order 3: Today, PREPARING (30 minutes ago)
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.MINUTE, -30)
        val todayRecentTimestamp = Timestamp(calendar.timeInMillis / 1000, 0)
        
        return listOf(
            // Order 1: ORD-001 - PAID (Yesterday)
            Order(
                id = "ORD-001",
                tableNumber = 5,
                status = OrderStatus.PAID,
                items = listOf(
                    OrderItem(
                        menuItemId = "1",
                        menuItemName = "Trà đào",
                        price = 30000.0,
                        quantity = 2
                    ),
                    OrderItem(
                        menuItemId = "2",
                        menuItemName = "Bạc xỉu",
                        price = 35000.0,
                        quantity = 1
                    )
                ),
                totalPrice = 105000.0, // 95,000 + 10,000 service fee
                createdAt = yesterdayTimestamp,
                userId = "user_123"
            ),
            
            // Order 2: ORD-002 - SERVED (Today, 2 hours ago)
            Order(
                id = "ORD-002",
                tableNumber = 3,
                status = OrderStatus.PAID, // Note: Using PAID since SERVED doesn't exist in enum
                items = listOf(
                    OrderItem(
                        menuItemId = "3",
                        menuItemName = "Cà phê sữa",
                        price = 25000.0,
                        quantity = 2
                    ),
                    OrderItem(
                        menuItemId = "4",
                        menuItemName = "Nước cam",
                        price = 28000.0,
                        quantity = 1
                    )
                ),
                totalPrice = 75000.0, // 65,000 + 10,000 service fee
                createdAt = todayMorningTimestamp,
                userId = "user_123"
            ),
            
            // Order 3: ORD-003 - PREPARING (Today, 30 minutes ago)
            Order(
                id = "ORD-003",
                tableNumber = 8,
                status = OrderStatus.PREPARING,
                items = listOf(
                    OrderItem(
                        menuItemId = "1",
                        menuItemName = "Trà đào",
                        price = 30000.0,
                        quantity = 3
                    ),
                    OrderItem(
                        menuItemId = "5",
                        menuItemName = "Trà sữa matcha",
                        price = 40000.0,
                        quantity = 1
                    ),
                    OrderItem(
                        menuItemId = "2",
                        menuItemName = "Bạc xỉu",
                        price = 35000.0,
                        quantity = 1
                    )
                ),
                totalPrice = 140000.0, // 130,000 + 10,000 service fee
                createdAt = todayRecentTimestamp,
                userId = "user_123"
            )
        )
    }
    
    override suspend fun getOrderById(orderId: String): Order? {
        delay(500) // Simulate network delay
        
        // Return the order matching the ID, or create a sample order for ORD-003
        val allOrders = getOrdersByUser()
        val foundOrder = allOrders.find { it.id == orderId }
        
        if (foundOrder != null) {
            return foundOrder
        }
        
        // If not found, return sample order for ORD-003 (as per requirements)
        if (orderId == "ORD-003") {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, -30)
            val todayRecentTimestamp = Timestamp(calendar.timeInMillis / 1000, 0)
            
            return Order(
                id = "ORD-003",
                tableNumber = 8,
                status = OrderStatus.PREPARING,
                items = listOf(
                    OrderItem(
                        menuItemId = "1",
                        menuItemName = "Trà đào",
                        price = 30000.0,
                        quantity = 2
                    ),
                    OrderItem(
                        menuItemId = "5",
                        menuItemName = "Trà sữa matcha",
                        price = 40000.0,
                        quantity = 1
                    ),
                    OrderItem(
                        menuItemId = "3",
                        menuItemName = "Cà phê sữa",
                        price = 25000.0,
                        quantity = 2
                    )
                ),
                totalPrice = 160000.0, // 150,000 + 10,000 service fee
                createdAt = todayRecentTimestamp,
                userId = "user_123"
            )
        }
        
        return null
    }
}

