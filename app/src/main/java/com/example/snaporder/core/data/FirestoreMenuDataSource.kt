package com.example.snaporder.core.data

import com.example.snaporder.core.firestore.MenuRepository
import com.example.snaporder.core.model.MenuItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of MenuDataSource.
 * 
 * This adapter connects the UI layer (MenuDataSource interface) with
 * the Firestore repository layer (MenuRepository).
 * 
 * ARCHITECTURE:
 * - Implements MenuDataSource interface for UI compatibility
 * - Uses MenuRepository for Firestore operations
 * - Filters to only available menu items for user display
 */
@Singleton
class FirestoreMenuDataSource @Inject constructor(
    private val menuRepository: MenuRepository
) : MenuDataSource {
    
    /**
     * Get all available menu items from Firestore.
     * Returns only items where available = true.
     * 
     * @return List of available menu items
     */
    override suspend fun getMenus(): List<MenuItem> {
        return menuRepository.getAllMenus()
            .filter { it.available }
    }
}

