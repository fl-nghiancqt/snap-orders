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
    
    /**
     * Get all menus (suspend function for one-time fetch).
     * Used in admin screen to load all menus.
     */
    suspend fun getAllMenus(): List<MenuItem> {
        return try {
            val snapshot = menusCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toMenuItem()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Create a new menu item in Firestore.
     * Generates a new document ID if menu.id is empty.
     * 
     * @param menu The menu item to create
     * @return Result containing the document ID on success
     */
    suspend fun createMenu(menu: MenuItem): Result<String> {
        return try {
            val documentRef = if (menu.id.isNotEmpty()) {
                menusCollection.document(menu.id)
            } else {
                menusCollection.document() // Auto-generate ID
            }
            
            val menuToCreate = if (menu.id.isEmpty()) {
                menu.copy(id = documentRef.id)
            } else {
                menu
            }
            
            documentRef.set(menuToCreate.toMap()).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update menu item.
     * Used to update availability or other menu properties.
     */
    suspend fun updateMenu(menu: MenuItem): Result<Unit> {
        return try {
            if (menu.id.isEmpty()) {
                return Result.failure(IllegalArgumentException("Menu ID cannot be empty"))
            }
            menusCollection.document(menu.id).set(menu.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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

// Extension function to convert MenuItem to Firestore map
private fun MenuItem.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "name" to name,
        "price" to price,
        "available" to available,
        "imageUrl" to imageUrl
    )
}

