package com.example.snaporder.core.firestore

import android.util.Log
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
     * Email is normalized to lowercase for case-insensitive matching.
     */
    suspend fun getUserByEmail(email: String): User? {
        return try {
            // Normalize email to lowercase for case-insensitive matching
            val normalizedEmail = email.trim().lowercase()
            
            Log.d("UserRepository", "getUserByEmail: Searching for email='$email' (normalized='$normalizedEmail')")
            
            // First, let's try the exact match
            var snapshot = usersCollection
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .get()
                .await()
            
            Log.d("UserRepository", "getUserByEmail: Query with normalized email returned ${snapshot.documents.size} documents")
            
            // If no results, try without normalization (in case email wasn't stored lowercase)
            if (snapshot.documents.isEmpty()) {
                Log.d("UserRepository", "getUserByEmail: Trying case-sensitive search for email='$email'")
                snapshot = usersCollection
                    .whereEqualTo("email", email.trim())
                    .limit(1)
                    .get()
                    .await()
                Log.d("UserRepository", "getUserByEmail: Case-sensitive query returned ${snapshot.documents.size} documents")
            }
            
            // Log all documents found for debugging
            snapshot.documents.forEachIndexed { index, doc ->
                Log.d("UserRepository", "getUserByEmail: Document $index - id='${doc.id}', data=${doc.data}")
            }
            
            val user = snapshot.documents.firstOrNull()?.toUser()
            
            if (user != null) {
                Log.d("UserRepository", "getUserByEmail: User found - id='${user.id}', name='${user.name}', email='${user.email}', role='${user.role}'")
                Log.d("UserRepository", "getUserByEmail: User password length=${user.password.length}, password='${user.password.replace(Regex("."), "*")}' (masked)")
            } else {
                Log.w("UserRepository", "getUserByEmail: No user found with email='$normalizedEmail' or '$email'")
                Log.d("UserRepository", "getUserByEmail: Query returned ${snapshot.documents.size} documents")
                
                // List all users for debugging
                listAllUsersForDebugging()
            }
            
            user
        } catch (e: Exception) {
            Log.e("UserRepository", "getUserByEmail: Error searching for email='$email'", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Debug helper: List all users in Firestore.
     * This helps identify if the user exists and how the email is stored.
     */
    private suspend fun listAllUsersForDebugging() {
        try {
            val allUsers = usersCollection.get().await()
            Log.d("UserRepository", "DEBUG: Total users in Firestore: ${allUsers.documents.size}")
            allUsers.documents.forEachIndexed { index, doc ->
                val data = doc.data
                Log.d("UserRepository", "DEBUG: User $index - id='${doc.id}', email='${data?.get("email")}', name='${data?.get("name")}', role='${data?.get("role")}'")
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "DEBUG: Error listing all users", e)
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
        // Normalize email to lowercase for consistent storage and querying
        if (email.isNotEmpty()) put("email", email.trim().lowercase())
        if (password.isNotEmpty()) put("password", password)
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toUser(): User? {
    return try {
        val emailValue = getString("email") ?: ""
        val passwordValue = getString("password") ?: ""
        val roleValue = getString("role") ?: "USER"
        
        Log.d("UserRepository", "toUser: Converting document id='$id'")
        Log.d("UserRepository", "toUser: email='$emailValue', password length=${passwordValue.length}, role='$roleValue'")
        
        User(
            id = id,
            name = getString("name") ?: "",
            email = emailValue,
            password = passwordValue,
            role = UserRole.valueOf(roleValue),
            createdAt = getLong("createdAt") ?: 0L
        )
    } catch (e: Exception) {
        Log.e("UserRepository", "toUser: Error converting document id='$id'", e)
        null
    }
}

