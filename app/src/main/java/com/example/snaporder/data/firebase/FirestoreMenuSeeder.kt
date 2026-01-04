package com.example.snaporder.data.firebase

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * One-time seed script to batch write menu items to Firestore.
 * 
 * This script writes ~60 menu items to the "menus" collection in Firestore.
 * Can be called multiple times (will overwrite existing items with same ID).
 */
object FirestoreMenuSeeder {
    
    private const val TAG = "MenuSeeder"
    private const val COLLECTION_NAME = "menus"
    
    /**
     * Seed menu items to Firestore.
     * Uses batch write for efficiency.
     * Returns Result<Int> with number of items written, or error.
     * 
     * This is a suspend function that can be called from coroutines.
     */
    suspend fun seedMenusToFirestore(): Result<Int> {
        return try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "Starting Menu Seeding to Firestore")
            Log.d(TAG, "========================================")
            
            val db = Firebase.firestore
            val batch = db.batch()
            
            val menus = getMenuData()
            Log.d(TAG, "Prepared ${menus.size} menu items for batch write")
            
            menus.forEach { menu ->
                val menuId = menu["id"] as String
                val docRef = db.collection(COLLECTION_NAME).document(menuId)
                batch.set(docRef, menu)
            }
            
            // Commit the batch and await result
            batch.commit().await()
            
            Log.d(TAG, "✓ Batch write SUCCESS")
            Log.d(TAG, "✓ ${menus.size} menu items written to Firestore")
            Log.d(TAG, "========================================")
            Log.d(TAG, "Menu Seeding: COMPLETED ✓")
            Log.d(TAG, "========================================")
            
            Result.success(menus.size)
        } catch (e: Exception) {
            Log.e(TAG, "✗ Batch write FAILED", e)
            Log.d(TAG, "========================================")
            Log.e(TAG, "Menu Seeding: FAILED ✗")
            Log.d(TAG, "========================================")
            Result.failure(e)
        }
    }
    
    /**
     * Get menu data from the provided list.
     * Returns list of menu items with Firestore-compatible structure.
     */
    private fun getMenuData(): List<Map<String, Any>> {
        val currentTime = System.currentTimeMillis()
        
        return listOf(
            mapOf("id" to "menu_001", "name" to "Cà phê đen", "price" to 25000, "imageUrl" to "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_002", "name" to "Cà phê sữa", "price" to 30000, "imageUrl" to "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_003", "name" to "Bạc xỉu", "price" to 35000, "imageUrl" to "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_004", "name" to "Cà phê muối", "price" to 35000, "imageUrl" to "https://images.unsplash.com/photo-1504754524776-8f4f37790ca0?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_005", "name" to "Espresso", "price" to 35000, "imageUrl" to "https://images.unsplash.com/photo-1510707577719-ae7c14805e3a?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_006", "name" to "Americano", "price" to 40000, "imageUrl" to "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_007", "name" to "Latte", "price" to 45000, "imageUrl" to "https://images.unsplash.com/photo-1523942839745-7848d6cfa6b1?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_008", "name" to "Cappuccino", "price" to 45000, "imageUrl" to "https://images.unsplash.com/photo-1523942839745-7848d6cfa6b1?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_009", "name" to "Mocha", "price" to 48000, "imageUrl" to "https://images.unsplash.com/photo-1541167760496-1628856ab772?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_010", "name" to "Cà phê sữa đá", "price" to 30000, "imageUrl" to "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_011", "name" to "Trà sữa trân châu", "price" to 40000, "imageUrl" to "https://images.unsplash.com/photo-1558857563-b371033873b8?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_012", "name" to "Trà sữa matcha", "price" to 45000, "imageUrl" to "https://images.unsplash.com/photo-1622484212850-eb596b3f8f23?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_013", "name" to "Trà sữa socola", "price" to 45000, "imageUrl" to "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_014", "name" to "Trà sữa khoai môn", "price" to 45000, "imageUrl" to "https://images.unsplash.com/photo-1622484212850-eb596b3f8f23?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_015", "name" to "Trà sữa caramel", "price" to 48000, "imageUrl" to "https://images.unsplash.com/photo-1558857563-b371033873b8?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_016", "name" to "Trà sữa đường đen", "price" to 50000, "imageUrl" to "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_017", "name" to "Trà sữa Oolong", "price" to 45000, "imageUrl" to "https://images.unsplash.com/photo-1558857563-b371033873b8?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_018", "name" to "Trà sữa Thái xanh", "price" to 45000, "imageUrl" to "https://images.unsplash.com/photo-1622484212850-eb596b3f8f23?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_019", "name" to "Trà đào", "price" to 30000, "imageUrl" to "https://images.unsplash.com/photo-1560807707-8cc77767d783?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_020", "name" to "Trà đào cam sả", "price" to 35000, "imageUrl" to "https://images.unsplash.com/photo-1560807707-8cc77767d783?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_021", "name" to "Trà vải", "price" to 30000, "imageUrl" to "https://images.unsplash.com/photo-1558642452-9d2a7deb7f62?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_022", "name" to "Trà chanh", "price" to 25000, "imageUrl" to "https://images.unsplash.com/photo-1497534446932-c925b458314e?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_023", "name" to "Trà tắc", "price" to 25000, "imageUrl" to "https://images.unsplash.com/photo-1558642452-9d2a7deb7f62?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_024", "name" to "Trà hoa cúc", "price" to 30000, "imageUrl" to "https://images.unsplash.com/photo-1497534446932-c925b458314e?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_025", "name" to "Trà gừng mật ong", "price" to 30000, "imageUrl" to "https://images.unsplash.com/photo-1497534446932-c925b458314e?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_026", "name" to "Nước cam ép", "price" to 30000, "imageUrl" to "https://images.unsplash.com/photo-1572441710534-680a1a5b05a0?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_027", "name" to "Nước táo ép", "price" to 30000, "imageUrl" to "https://images.unsplash.com/photo-1560807707-8cc77767d783?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_028", "name" to "Nước dưa hấu", "price" to 28000, "imageUrl" to "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_029", "name" to "Nước ổi", "price" to 30000, "imageUrl" to "https://images.unsplash.com/photo-1572441710534-680a1a5b05a0?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_030", "name" to "Nước dứa", "price" to 28000, "imageUrl" to "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_031", "name" to "Sinh tố xoài", "price" to 40000, "imageUrl" to "https://images.unsplash.com/photo-1623065422902-30a2d299bbe4?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_032", "name" to "Sinh tố bơ", "price" to 45000, "imageUrl" to "https://images.unsplash.com/photo-1623065422902-30a2d299bbe4?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_033", "name" to "Sinh tố dâu", "price" to 45000, "imageUrl" to "https://images.unsplash.com/photo-1623065422902-30a2d299bbe4?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_034", "name" to "Sinh tố chuối", "price" to 40000, "imageUrl" to "https://images.unsplash.com/photo-1623065422902-30a2d299bbe4?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_035", "name" to "Coca Cola", "price" to 20000, "imageUrl" to "https://images.unsplash.com/photo-1629203851122-3726ecdf080e?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_036", "name" to "Pepsi", "price" to 20000, "imageUrl" to "https://images.unsplash.com/photo-1629203851122-3726ecdf080e?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_037", "name" to "7Up", "price" to 20000, "imageUrl" to "https://images.unsplash.com/photo-1629203851122-3726ecdf080e?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_038", "name" to "Sting dâu", "price" to 22000, "imageUrl" to "https://images.unsplash.com/photo-1629203851122-3726ecdf080e?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_039", "name" to "Red Bull", "price" to 25000, "imageUrl" to "https://images.unsplash.com/photo-1629203851122-3726ecdf080e?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_040", "name" to "Khoai tây chiên", "price" to 35000, "imageUrl" to "https://images.unsplash.com/photo-1571091718767-18b5b1457add?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_041", "name" to "Khoai lang chiên", "price" to 35000, "imageUrl" to "https://images.unsplash.com/photo-1571091718767-18b5b1457add?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_042", "name" to "Bánh tráng trộn", "price" to 30000, "imageUrl" to "https://images.unsplash.com/photo-1604908554160-33c61c86f0b6?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_043", "name" to "Bánh tráng nướng", "price" to 35000, "imageUrl" to "https://images.unsplash.com/photo-1604908554160-33c61c86f0b6?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_044", "name" to "Xúc xích chiên", "price" to 30000, "imageUrl" to "https://images.unsplash.com/photo-1571091718767-18b5b1457add?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_045", "name" to "Gà rán", "price" to 50000, "imageUrl" to "https://images.unsplash.com/photo-1606755962773-d324e8f7a8a9?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_046", "name" to "Cánh gà chiên mắm", "price" to 55000, "imageUrl" to "https://images.unsplash.com/photo-1606755962773-d324e8f7a8a9?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_047", "name" to "Burger bò", "price" to 55000, "imageUrl" to "https://images.unsplash.com/photo-1550547660-d9450f859349?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_048", "name" to "Burger gà", "price" to 50000, "imageUrl" to "https://images.unsplash.com/photo-1550547660-d9450f859349?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_049", "name" to "Pizza mini", "price" to 70000, "imageUrl" to "https://images.unsplash.com/photo-1548365328-8b7b6a1b1c56?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_050", "name" to "Mì xào bò", "price" to 55000, "imageUrl" to "https://images.unsplash.com/photo-1604908177522-0401c2b98e85?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_051", "name" to "Mì xào hải sản", "price" to 60000, "imageUrl" to "https://images.unsplash.com/photo-1604908177522-0401c2b98e85?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_052", "name" to "Cơm gà xối mỡ", "price" to 60000, "imageUrl" to "https://images.unsplash.com/photo-1604908554160-33c61c86f0b6?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_053", "name" to "Cơm chiên hải sản", "price" to 55000, "imageUrl" to "https://images.unsplash.com/photo-1604908554160-33c61c86f0b6?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_054", "name" to "Cơm sườn", "price" to 60000, "imageUrl" to "https://images.unsplash.com/photo-1604908554160-33c61c86f0b6?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_055", "name" to "Bánh flan", "price" to 25000, "imageUrl" to "https://images.unsplash.com/photo-1551024601-bec78aea704b?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_056", "name" to "Tiramisu", "price" to 40000, "imageUrl" to "https://images.unsplash.com/photo-1551024601-bec78aea704b?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_057", "name" to "Cheesecake", "price" to 45000, "imageUrl" to "https://images.unsplash.com/photo-1551024601-bec78aea704b?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_058", "name" to "Rau câu dừa", "price" to 25000, "imageUrl" to "https://images.unsplash.com/photo-1551024601-bec78aea704b?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_059", "name" to "Kem vani", "price" to 30000, "imageUrl" to "https://images.unsplash.com/photo-1497034825429-c343d7c6a68f?w=600", "available" to true, "createdAt" to currentTime),
            mapOf("id" to "menu_060", "name" to "Kem socola", "price" to 30000, "imageUrl" to "https://images.unsplash.com/photo-1497034825429-c343d7c6a68f?w=600", "available" to true, "createdAt" to currentTime)
        )
    }
}

