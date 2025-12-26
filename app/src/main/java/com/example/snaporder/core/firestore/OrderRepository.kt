package com.example.snaporder.core.firestore

import com.example.snaporder.core.constants.FirestoreCollections
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.model.OrderStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Order operations in Firestore.
 * Contains core business logic for order management.
 */
@Singleton
class OrderRepository @Inject constructor(
    private val firestoreProvider: FirestoreProvider
) {
    private val firestore: FirebaseFirestore = firestoreProvider.firestore
    private val ordersCollection = firestore.collection(FirestoreCollections.ORDERS)
    
    /**
     * Get order by ID.
     */
    suspend fun getOrder(orderId: String): Order? {
        return try {
            val document = ordersCollection.document(orderId).get().await()
            if (document.exists()) {
                document.toOrder()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get all orders for a specific user.
     */
    fun getUserOrders(userId: String): Flow<List<Order>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toOrder()
                } ?: emptyList()
                
                trySend(orders)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get all orders (for admin).
     */
    fun getAllOrders(): Flow<List<Order>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = ordersCollection
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toOrder()
                } ?: emptyList()
                
                trySend(orders)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get open orders for a specific table.
     * BUSINESS RULE: Used to check if table already has an open order.
     */
    suspend fun getOpenOrderByTable(tableNumber: Int): Order? {
        return try {
            val snapshot = ordersCollection
                .whereEqualTo("tableNumber", tableNumber)
                .get()
                .await()
            
            snapshot.documents
                .mapNotNull { it.toOrder() }
                .firstOrNull { it.isOpen() }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create a new order.
     */
    suspend fun createOrder(order: Order): Result<String> {
        return try {
            val docRef = ordersCollection.add(order.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing order.
     */
    suspend fun updateOrder(order: Order): Result<Unit> {
        return try {
            if (order.id.isEmpty()) {
                return Result.failure(IllegalArgumentException("Order ID cannot be empty"))
            }
            ordersCollection.document(order.id).set(order.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update order status.
     */
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> {
        return try {
            ordersCollection.document(orderId)
                .update("status", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Extension functions for Firestore conversion
private fun Order.toMap(): Map<String, Any> {
    return mapOf(
        "tableNumber" to tableNumber,
        "items" to items.map { it.toMap() },
        "totalPrice" to totalPrice,
        "status" to status.name,
        "createdAt" to (createdAt ?: Timestamp.now()),
        "userId" to userId
    )
}

private fun com.example.snaporder.core.model.OrderItem.toMap(): Map<String, Any> {
    return mapOf(
        "menuItemId" to menuItemId,
        "menuItemName" to menuItemName,
        "price" to price,
        "quantity" to quantity
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toOrder(): Order? {
    return try {
        val itemsList = get("items") as? List<Map<String, Any>> ?: emptyList()
        val items = itemsList.mapNotNull { map ->
            try {
                com.example.snaporder.core.model.OrderItem(
                    menuItemId = map["menuItemId"] as? String ?: "",
                    menuItemName = map["menuItemName"] as? String ?: "",
                    price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                    quantity = (map["quantity"] as? Number)?.toInt() ?: 1
                )
            } catch (e: Exception) {
                null
            }
        }
        
        Order(
            id = id,
            tableNumber = (get("tableNumber") as? Number)?.toInt() ?: 0,
            items = items,
            totalPrice = (get("totalPrice") as? Number)?.toDouble() ?: 0.0,
            status = OrderStatus.valueOf(getString("status") ?: "CREATED"),
            createdAt = getTimestamp("createdAt"),
            userId = getString("userId") ?: ""
        )
    } catch (e: Exception) {
        null
    }
}

