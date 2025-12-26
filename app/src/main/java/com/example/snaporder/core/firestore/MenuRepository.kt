package com.example.snaporder.core.firestore

import com.example.snaporder.core.constants.FirestoreCollections
import com.example.snaporder.core.model.MenuItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Menu operations in Firestore.
 */
@Singleton
class MenuRepository @Inject constructor(
    private val firestoreProvider: FirestoreProvider
) {
    private val firestore: FirebaseFirestore = firestoreProvider.firestore
    private val menusCollection = firestore.collection(FirestoreCollections.MENUS)
    
    /**
     * Get all available menu items.
     * Returns a Flow that emits updates in real-time.
     * Note: This uses callbackFlow for proper snapshot listener handling.
     */
    fun getAvailableMenuItems(): Flow<List<MenuItem>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = menusCollection
            .whereEqualTo("available", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toMenuItem()
                } ?: emptyList()
                
                trySend(items)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get all menu items (including unavailable).
     */
    fun getAllMenuItems(): Flow<List<MenuItem>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = menusCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toMenuItem()
                } ?: emptyList()
                
                trySend(items)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get menu item by ID.
     */
    suspend fun getMenuItem(menuItemId: String): MenuItem? {
        return try {
            val document = menusCollection.document(menuItemId).get().await()
            if (document.exists()) {
                document.toMenuItem()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

// Extension function for Firestore conversion
private fun com.google.firebase.firestore.DocumentSnapshot.toMenuItem(): MenuItem? {
    return try {
        MenuItem(
            id = id,
            name = getString("name") ?: "",
            price = getDouble("price") ?: 0.0,
            available = getBoolean("available") ?: true,
            imageUrl = getString("imageUrl") ?: ""
        )
    } catch (e: Exception) {
        null
    }
}

