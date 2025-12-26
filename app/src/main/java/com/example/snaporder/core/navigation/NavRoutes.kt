package com.example.snaporder.core.navigation

/**
 * Navigation routes for the app.
 * 
 * AUTH ROUTES:
 * - Login screen where users select their account
 * 
 * USER ROUTES:
 * - Menu: Browse available menu items
 * - Cart: Review cart items and select table number
 * - Order: Place order and view order result
 * - History: View past orders
 * 
 * ADMIN ROUTES:
 * - Dashboard: View all orders, manage order statuses
 */
object NavRoutes {
    // Auth
    const val AUTH = "auth"
    
    // User Flow
    const val USER_MENU = "user_menu"
    const val USER_CART = "user_cart"
    const val USER_ORDER = "user_order"
    const val USER_HISTORY = "user_history"
    const val USER_ORDER_DETAIL = "user_order_detail/{orderId}"
    
    // Helper function to build order detail route
    fun userOrderDetail(orderId: String) = "user_order_detail/$orderId"
    
    // Admin Flow
    const val ADMIN_DASHBOARD = "admin_dashboard"
}

