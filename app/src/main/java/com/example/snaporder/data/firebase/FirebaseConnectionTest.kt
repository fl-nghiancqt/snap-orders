package com.example.snaporder.data.firebase

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Simple Firestore connection test.
 * 
 * This test verifies that:
 * 1. The app can connect to Firebase Firestore
 * 2. The app can WRITE a document
 * 3. The app can READ the same document back
 * 
 * USAGE:
 * Call FirebaseConnectionTest.test() once from MainActivity.onCreate()
 * This call is temporary and can be removed after testing.
 */
object FirebaseConnectionTest {
    
    private const val TAG = "FirebaseTest"
    private const val COLLECTION_NAME = "test_connection"
    
    /**
     * Run the Firestore connection test.
     * Writes a document, then reads it back, logging all results.
     */
    fun test() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "Starting Firestore Connection Test")
        Log.d(TAG, "========================================")
        
        // Get FirebaseFirestore instance
        val db = Firebase.firestore
        
        Log.d(TAG, "Firestore instance obtained")
        
        // Create test data
        val testData = hashMapOf(
            "message" to "hello firebase",
            "createdAt" to Timestamp.now()
        )
        
        Log.d(TAG, "Test data created: message='hello firebase'")
        
        // Write document to Firestore
        db.collection(COLLECTION_NAME)
            .add(testData)
            .addOnSuccessListener { documentReference ->
                val documentId = documentReference.id
                Log.d(TAG, "✓ Write SUCCESS - Document ID: $documentId")
                
                // Read the same document back
                db.collection(COLLECTION_NAME)
                    .document(documentId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val message = document.getString("message")
                            val createdAt = document.getTimestamp("createdAt")
                            Log.d(TAG, "✓ Read SUCCESS")
                            Log.d(TAG, "  - Message: $message")
                            Log.d(TAG, "  - CreatedAt: $createdAt")
                            Log.d(TAG, "========================================")
                            Log.d(TAG, "Firestore Connection Test: PASSED ✓")
                            Log.d(TAG, "========================================")
                        } else {
                            Log.e(TAG, "✗ Read FAILED - Document does not exist")
                            Log.d(TAG, "========================================")
                            Log.d(TAG, "Firestore Connection Test: FAILED ✗")
                            Log.d(TAG, "========================================")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "✗ Read FAILED", e)
                        Log.d(TAG, "========================================")
                        Log.d(TAG, "Firestore Connection Test: FAILED ✗")
                        Log.d(TAG, "========================================")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "✗ Write FAILED", e)
                Log.d(TAG, "========================================")
                Log.d(TAG, "Firestore Connection Test: FAILED ✗")
                Log.d(TAG, "========================================")
            }
    }
}

