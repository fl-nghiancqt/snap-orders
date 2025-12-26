package com.example.snaporder.core.data

import com.example.snaporder.core.model.MenuItem
import kotlinx.coroutines.delay

/**
 * Fake MenuRepository implementation for UI development.
 * 
 * REPLACEMENT STRATEGY:
 * When ready to connect Firestore:
 * 1. Create FirestoreMenuDataSource implementing MenuDataSource
 * 2. Update Hilt module to provide FirestoreMenuDataSource instead
 * 3. No changes needed in ViewModel or UI code
 * 
 * This repository simulates network delay and returns sample Vietnamese drinks.
 */
class FakeMenuRepository : MenuDataSource {
    
    override suspend fun getMenus(): List<MenuItem> {
        // Simulate network delay
        delay(500)
        
        return listOf(
            MenuItem(
                id = "menu_001",
                name = "Trà đào",
                price = 30000.0,
                available = true,
                imageUrl = "https://example.com/images/tra-dao.jpg"
            ),
            MenuItem(
                id = "menu_002",
                name = "Bạc xỉu",
                price = 35000.0,
                available = true,
                imageUrl = "https://example.com/images/bac-xiu.jpg"
            ),
            MenuItem(
                id = "menu_003",
                name = "Cà phê sữa",
                price = 25000.0,
                available = true,
                imageUrl = "https://example.com/images/ca-phe-sua.jpg"
            ),
            MenuItem(
                id = "menu_004",
                name = "Trà sữa matcha",
                price = 40000.0,
                available = true,
                imageUrl = "https://example.com/images/tra-sua-matcha.jpg"
            ),
            MenuItem(
                id = "menu_005",
                name = "Nước cam",
                price = 28000.0,
                available = true,
                imageUrl = "https://example.com/images/nuoc-cam.jpg"
            ),
            MenuItem(
                id = "menu_006",
                name = "Cà phê đen",
                price = 20000.0,
                available = false, // Unavailable item for testing disabled state
                imageUrl = "https://example.com/images/ca-phe-den.jpg"
            )
        )
    }
}

