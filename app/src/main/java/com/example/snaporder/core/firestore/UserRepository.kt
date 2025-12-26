package com.example.snaporder.core.firestore

import com.example.snaporder.core.constants.FirestoreCollections
import com.example.snaporder.core.model.User
import com.example.snaporder.core.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for User operations in Firestore.
 * Handles all user-related database operations.
 */
@Singleton
class UserRepository @Inject constructor(
    private val firestoreProvider: FirestoreProvider
) {
    private val firestore: FirebaseFirestore = firestoreProvider.firestore
    private val usersCollection = firestore.collection(FirestoreCollections.USERS)
    
    /**
     * Get all users from Firestore.
     * Returns a Flow that emits updates in real-time.
     * Used in login screen to display available users for selection.
     */
    fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toUser()
                } ?: emptyList()
                
                trySend(users)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get user by ID.
     * Used to fetch a specific user's details.
     */
    suspend fun getUserById(userId: String): User? {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                document.toUser()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get user by ID (legacy method name, kept for backward compatibility).
     */
    suspend fun getUser(userId: String): User? {
        return getUserById(userId)
    }
    
    /**
     * Create or update user.
     */
    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user by email/username.
     * Used for login with username and password.
     */
    suspend fun getUserByEmail(email: String): User? {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.toUser()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get user role by userId.
     * Used for quick role validation.
     */
    suspend fun getUserRole(userId: String): UserRole? {
        return getUserById(userId)?.role
    }
}

// Extension functions for Firestore conversion
private fun User.toMap(): Map<String, Any> {
    return buildMap {
        put("name", name)
        put("role", role.name)
        put("createdAt", createdAt)
        if (email.isNotEmpty()) put("email", email)
        if (password.isNotEmpty()) put("password", password)
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toUser(): User? {
    return try {
        User(
            id = id,
            name = getString("name") ?: "",
            email = getString("email") ?: "",
            password = getString("password") ?: "",
            role = UserRole.valueOf(getString("role") ?: "USER"),
            createdAt = getLong("createdAt") ?: 0L
        )
    } catch (e: Exception) {
        null
    }
}

