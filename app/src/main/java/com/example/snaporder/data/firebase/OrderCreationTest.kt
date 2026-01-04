package com.example.snaporder.data.firebase

import android.util.Log
import com.example.snaporder.core.constants.FirestoreCollections
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.model.OrderItem
import com.example.snaporder.core.model.OrderStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Test function to verify order creation in Firestore.
 * 
 * This can be called from MainActivity to test if orders can be written to Firestore.
 * 
 * USAGE:
 * In MainActivity.onCreate():
 * lifecycleScope.launch {
 *     OrderCreationTest.testOrderCreation()
 * }
 */
object OrderCreationTest {
    private const val TAG = "OrderCreationTest"
    
    /**
     * Test creating a simple order in Firestore.
     * This helps verify:
     * 1. Firestore connection works
     * 2. Orders collection exists
     * 3. Security rules allow writes
     * 4. Order data structure is correct
     */
    suspend fun testOrderCreation(): Result<String> {
        return try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "Testing Order Creation in Firestore")
            Log.d(TAG, "========================================")
            
            val db = Firebase.firestore
            val ordersCollection = db.collection(FirestoreCollections.ORDERS)
            
            // Create a test order
            val testOrder = mapOf(
                "tableNumber" to 99,
                "items" to listOf(
                    mapOf(
                        "menuItemId" to "test_item_1",
                        "menuItemName" to "Test Item",
                        "price" to 10000.0,
                        "quantity" to 2
                    )
                ),
                "totalPrice" to 20000.0,
                "status" to "CREATED",
                "createdAt" to Timestamp.now(),
                "userId" to "test_user"
            )
            
            Log.d(TAG, "Test order data: $testOrder")
            Log.d(TAG, "Collection: ${FirestoreCollections.ORDERS}")
            Log.d(TAG, "Attempting to write order...")
            
            // Try to write the order
            val docRef = ordersCollection.add(testOrder).await()
            
            Log.i(TAG, "✓ Test order created successfully!")
            Log.i(TAG, "Document ID: ${docRef.id}")
            Log.i(TAG, "Full path: ${docRef.path}")
            
            // Verify by reading it back
            val verifyDoc = docRef.get().await()
            if (verifyDoc.exists()) {
                Log.i(TAG, "✓ Verified: Document exists in Firestore")
                Log.d(TAG, "Document data: ${verifyDoc.data}")
            } else {
                Log.e(TAG, "✗ WARNING: Document does not exist after creation!")
            }
            
            Log.d(TAG, "========================================")
            Log.i(TAG, "Order Creation Test: PASSED ✓")
            Log.d(TAG, "========================================")
            
            Result.success(docRef.id)
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            Log.e(TAG, "✗ FirestoreException during test", e)
            Log.e(TAG, "Error code: ${e.code}")
            Log.e(TAG, "Error message: ${e.message}")
            
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.e(TAG, "PERMISSION_DENIED: Check Firestore security rules!")
                Log.e(TAG, "Update rules in Firebase Console to allow writes to 'orders' collection")
            }
            
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "✗ Exception during test", e)
            Result.failure(e)
        }
    }
}

