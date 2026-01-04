package com.example.snaporder.core.session

import com.example.snaporder.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserSessionManager - Centralized user session management.
 * 
 * This singleton provides a single source of truth for the current logged-in user.
 * Any screen or component can inject this to check if user is logged in and access user info.
 * 
 * USAGE:
 * ```kotlin
 * @Composable
 * fun MyScreen(
 *     userSessionManager: UserSessionManager = hiltViewModel() // or inject via Hilt
 * ) {
 *     val currentUser by userSessionManager.currentUser.collectAsStateWithLifecycle()
 *     val isLoggedIn = currentUser != null
 * }
 * ```
 * 
 * ARCHITECTURE:
 * - Singleton (shared across entire app)
 * - StateFlow for reactive updates
 * - Can be injected anywhere with Hilt
 */
@Singleton
class UserSessionManager @Inject constructor() {
    
    /**
     * Current logged-in user.
     * null = not logged in
     * User = logged in
     */
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    /**
     * Flow that emits true when user is logged in, false otherwise.
     * Convenient for checking login status.
     */
    val isLoggedIn: Flow<Boolean> = _currentUser.asStateFlow().map { it != null }
    
    /**
     * Set the current logged-in user.
     * Called after successful login.
     */
    fun setUser(user: User) {
        _currentUser.value = user
    }
    
    /**
     * Clear the current user (logout).
     */
    fun clearUser() {
        _currentUser.value = null
    }
    
    /**
     * Get current user synchronously (nullable).
     * Use this when you need immediate value without Flow.
     */
    fun getUser(): User? = _currentUser.value
    
    /**
     * Check if user is logged in synchronously.
     */
    fun isUserLoggedIn(): Boolean = _currentUser.value != null
    
    /**
     * Get user ID if logged in, null otherwise.
     */
    fun getUserId(): String? = _currentUser.value?.id
    
    /**
     * Get user role if logged in, null otherwise.
     */
    fun getUserRole(): com.example.snaporder.core.model.UserRole? = _currentUser.value?.role
}

