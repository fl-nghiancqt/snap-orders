package com.example.snaporder.core.data

import com.example.snaporder.core.model.MenuItem

/**
 * Repository interface for menu operations.
 * 
 * ARCHITECTURE NOTE:
 * This interface allows swapping implementations:
 * - FakeMenuRepository: For UI development and testing (current)
 * - FirestoreMenuRepository: For production with Firestore (future)
 * 
 * The ViewModel and UI code remain unchanged when swapping implementations.
 * 
 * NOTE: This is separate from core.firestore.MenuRepository which will be
 * used later. This interface is for UI development phase.
 */
interface MenuDataSource {
    /**
     * Get all menu items.
     * Returns a suspend function that can be called from coroutines.
     * 
     * In production, this will fetch from Firestore.
     * Currently returns fake data for UI development.
     */
    suspend fun getMenus(): List<MenuItem>
}

