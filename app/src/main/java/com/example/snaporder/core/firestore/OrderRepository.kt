package com.example.snaporder.core.firestore

import android.util.Log
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
     * Note: Uses whereEqualTo only (no orderBy) to avoid requiring a composite index.
     * Orders will be sorted in the ViewModel if needed.
     */
    fun getUserOrders(userId: String): Flow<List<Order>> = kotlinx.coroutines.flow.callbackFlow {
        Log.d("OrderRepository", "getUserOrders: Starting listener for userId='$userId'")
        
        val listener = ordersCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OrderRepository", "getUserOrders: Firestore error for userId='$userId'", error)
                    // Don't close the flow on error, just emit empty list
                    // The error will be caught by the catch block in ViewModel
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toOrder()
                    } catch (e: Exception) {
                        Log.e("OrderRepository", "getUserOrders: Error converting document ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                // Sort by createdAt descending in-memory (since we can't use orderBy without index)
                val sortedOrders = orders.sortedByDescending { it.createdAt?.seconds ?: 0L }
                
                Log.d("OrderRepository", "getUserOrders: Emitting ${sortedOrders.size} orders for userId='$userId'")
                trySend(sortedOrders)
            }
        
        awaitClose {
            Log.d("OrderRepository", "getUserOrders: Closing listener for userId='$userId'")
            listener.remove()
        }
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
            Log.d("OrderRepository", "getOpenOrderByTable: Checking for open order on table $tableNumber")
            // Convert Int to Long for Firestore query (Firestore stores integers as Long)
            val snapshot = ordersCollection
                .whereEqualTo("tableNumber", tableNumber.toLong())
                .get()
                .await()
            
            Log.d("OrderRepository", "getOpenOrderByTable: Found ${snapshot.documents.size} orders for table $tableNumber")
            
            val orders = snapshot.documents.mapNotNull { doc ->
                try {
                    val order = doc.toOrder()
                    Log.d("OrderRepository", "getOpenOrderByTable: Order ${doc.id} - status=${order?.status}, isOpen=${order?.isOpen()}")
                    order
                } catch (e: Exception) {
                    Log.e("OrderRepository", "getOpenOrderByTable: Failed to convert document ${doc.id}", e)
                    null
                }
            }
            
            val openOrder = orders.firstOrNull { it.isOpen() }
            if (openOrder != null) {
                Log.d("OrderRepository", "getOpenOrderByTable: Found open order - id='${openOrder.id}', status=${openOrder.status}")
            } else {
                Log.d("OrderRepository", "getOpenOrderByTable: No open order found for table $tableNumber")
            }
            
            openOrder
        } catch (e: Exception) {
            Log.e("OrderRepository", "getOpenOrderByTable: Exception while checking for open order", e)
            null
        }
    }
    
    /**
     * Create a new order in Firestore.
     * 
     * VALIDATION:
     * - Order must have items (validated before calling this method)
     * - Order must have valid table number
     * 
     * @param order The order to create
     * @return Result containing the Firestore document ID on success
     */
    suspend fun createOrder(order: Order): Result<String> {
        return try {
            // Validate order has items
            if (order.items.isEmpty()) {
                Log.e("OrderRepository", "createOrder: Order has no items")
                return Result.failure(
                    IllegalArgumentException("Cannot create order with no items")
                )
            }
            
            // Validate table number
            if (order.tableNumber <= 0) {
                Log.e("OrderRepository", "createOrder: Invalid table number: ${order.tableNumber}")
                return Result.failure(
                    IllegalArgumentException("Table number must be greater than 0")
                )
            }
            
            Log.d("OrderRepository", "createOrder: Creating order - table=${order.tableNumber}, items=${order.items.size}, total=${order.totalPrice}, status=${order.status}, userId='${order.userId}'")
            
            // Log order items details
            order.items.forEachIndexed { index, item ->
                Log.d("OrderRepository", "createOrder: Item $index - menuItemId='${item.menuItemId}', name='${item.menuItemName}', price=${item.price}, quantity=${item.quantity}")
            }
            
            // Convert order to Firestore map
            val orderMap = order.toMap()
            
            Log.d("OrderRepository", "createOrder: ========================================")
            Log.d("OrderRepository", "createOrder: ORDER DATA STRUCTURE:")
            Log.d("OrderRepository", "createOrder: ========================================")
            Log.d("OrderRepository", "createOrder: Order map keys: ${orderMap.keys}")
            Log.d("OrderRepository", "createOrder: tableNumber: ${orderMap["tableNumber"]} (type: ${orderMap["tableNumber"]?.javaClass?.simpleName})")
            Log.d("OrderRepository", "createOrder: totalPrice: ${orderMap["totalPrice"]} (type: ${orderMap["totalPrice"]?.javaClass?.simpleName})")
            Log.d("OrderRepository", "createOrder: status: ${orderMap["status"]} (type: ${orderMap["status"]?.javaClass?.simpleName})")
            Log.d("OrderRepository", "createOrder: userId: ${orderMap["userId"]} (type: ${orderMap["userId"]?.javaClass?.simpleName})")
            Log.d("OrderRepository", "createOrder: createdAt: ${orderMap["createdAt"]} (type: ${orderMap["createdAt"]?.javaClass?.simpleName})")
            
            val itemsList = orderMap["items"] as? List<*>
            Log.d("OrderRepository", "createOrder: items count: ${itemsList?.size}")
            
            itemsList?.forEachIndexed { index, item ->
                val itemMap = item as? Map<*, *>
                Log.d("OrderRepository", "createOrder:   Item $index:")
                Log.d("OrderRepository", "createOrder:     menuItemId: ${itemMap?.get("menuItemId")} (type: ${itemMap?.get("menuItemId")?.javaClass?.simpleName})")
                Log.d("OrderRepository", "createOrder:     menuItemName: ${itemMap?.get("menuItemName")} (type: ${itemMap?.get("menuItemName")?.javaClass?.simpleName})")
                Log.d("OrderRepository", "createOrder:     price: ${itemMap?.get("price")} (type: ${itemMap?.get("price")?.javaClass?.simpleName})")
                Log.d("OrderRepository", "createOrder:     quantity: ${itemMap?.get("quantity")} (type: ${itemMap?.get("quantity")?.javaClass?.simpleName})")
            }
            
            Log.d("OrderRepository", "createOrder: Full order map: $orderMap")
            Log.d("OrderRepository", "createOrder: ========================================")
            
            // Verify collection name
            Log.d("OrderRepository", "createOrder: Using collection: ${FirestoreCollections.ORDERS}")
            
            // Add order to Firestore orders collection
            Log.d("OrderRepository", "createOrder: Calling ordersCollection.add()...")
            Log.d("OrderRepository", "createOrder: Collection path: ${ordersCollection.path}")
            
            try {
                val docRef = ordersCollection.add(orderMap).await()
                
                Log.i("OrderRepository", "createOrder: ✓ Order created successfully in Firestore!")
                Log.i("OrderRepository", "createOrder: Document ID: ${docRef.id}")
                Log.i("OrderRepository", "createOrder: Collection: ${FirestoreCollections.ORDERS}")
                Log.i("OrderRepository", "createOrder: Full path: ${docRef.path}")
                Log.i("OrderRepository", "createOrder: Table: ${order.tableNumber}, Total: ${order.totalPrice}")
                
                // Verify the document was created by reading it back
                val verifyDoc = docRef.get().await()
                if (verifyDoc.exists()) {
                    Log.i("OrderRepository", "createOrder: ✓ Verified: Document exists in Firestore")
                } else {
                    Log.e("OrderRepository", "createOrder: ✗ WARNING: Document does not exist after creation!")
                }
                
                // Return the document ID
                Result.success(docRef.id)
            } catch (firestoreException: com.google.firebase.firestore.FirebaseFirestoreException) {
                Log.e("OrderRepository", "createOrder: ✗ FirestoreException", firestoreException)
                Log.e("OrderRepository", "createOrder: Error code: ${firestoreException.code}")
                Log.e("OrderRepository", "createOrder: Error message: ${firestoreException.message}")
                if (firestoreException.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e("OrderRepository", "createOrder: PERMISSION_DENIED - Check Firestore security rules for 'orders' collection")
                }
                Result.failure(firestoreException)
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "createOrder: ✗ ERROR creating order in Firestore", e)
            Log.e("OrderRepository", "createOrder: Exception type: ${e.javaClass.simpleName}")
            Log.e("OrderRepository", "createOrder: Exception message: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing order.
     */
    suspend fun updateOrder(order: Order): Result<Unit> {
        return try {
            Log.d("OrderRepository", "updateOrder: Called - orderId='${order.id}', table=${order.tableNumber}, items=${order.items.size}, total=${order.totalPrice}")
            
            if (order.id.isEmpty()) {
                Log.e("OrderRepository", "updateOrder: Order ID is empty")
                return Result.failure(IllegalArgumentException("Order ID cannot be empty"))
            }
            
            val orderMap = order.toMap()
            Log.d("OrderRepository", "updateOrder: Order map keys: ${orderMap.keys}")
            Log.d("OrderRepository", "updateOrder: Calling ordersCollection.document('${order.id}').set()...")
            
            ordersCollection.document(order.id).set(orderMap).await()
            
            Log.i("OrderRepository", "updateOrder: ✓ Order updated successfully in Firestore - id='${order.id}'")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepository", "updateOrder: ✗ ERROR updating order", e)
            Log.e("OrderRepository", "updateOrder: Exception type: ${e.javaClass.simpleName}")
            Log.e("OrderRepository", "updateOrder: Exception message: ${e.message}")
            e.printStackTrace()
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
        // Convert Int to Long for Firestore (Firestore stores integers as Long/Number)
        "tableNumber" to tableNumber.toLong(),
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
        "price" to price, // Double is fine for Firestore
        // Convert Int to Long for Firestore (Firestore stores integers as Long/Number)
        "quantity" to quantity.toLong()
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

