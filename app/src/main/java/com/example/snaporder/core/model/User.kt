package com.example.snaporder.core.model

/**
 * User model representing a user in the system.
 * 
 * ROLE-BASED ACCESS CONTROL:
 * - USER: Can browse menu, place orders, view order history
 * - ADMIN: Can manage all orders, update order statuses, view all tables
 * 
 * Role determines which navigation flow the user can access after login.
 */
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "", // Username/Email for login
    val password: String = "", // Password (optional for school project)
    val role: UserRole = UserRole.USER,
    val createdAt: Long = 0L
)

/**
 * UserRole enum defining the two access levels in the app.
 * 
 * USER FLOW:
 * - Login → Menu → Cart → Place Order → Order Result → History
 * - Can add more items to existing open orders
 * - Can only view their own order history
 * 
 * ADMIN FLOW:
 * - Login → Admin Dashboard
 * - Can view all orders from all tables
 * - Can update order statuses (CREATED → PREPARING → PAID)
 * - Can manage order lifecycle
 */
enum class UserRole {
    USER,
    ADMIN
}

