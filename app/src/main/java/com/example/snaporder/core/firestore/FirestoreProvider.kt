package com.example.snaporder.core.firestore

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton provider for FirebaseFirestore instance.
 * Centralized access point for Firestore operations.
 */
@Singleton
class FirestoreProvider @Inject constructor() {
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
}

