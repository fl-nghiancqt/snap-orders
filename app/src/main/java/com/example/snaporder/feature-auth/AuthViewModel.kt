package com.example.snaporder.feature.auth

import android.util.Log
import com.example.snaporder.core.firestore.UserRepository
import com.example.snaporder.core.model.User
import com.example.snaporder.core.model.UserRole
import com.example.snaporder.core.session.UserSessionManager
import com.example.snaporder.core.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Auth ViewModel for user login and selection.
 * 
 * LOGIN FLOW:
 * 1. Fetch all users from Firestore
 * 2. Display users for selection
 * 3. On user selection, validate user exists and role is valid
 * 4. Navigate based on role:
 *    - USER → UserNavGraph (Menu → Cart → Order → History)
 *    - ADMIN → AdminNavGraph (Admin Dashboard)
 * 
 * VALIDATION:
 * - User must exist in Firestore (checked via getUserById)
 * - Role must be valid (USER or ADMIN enum value)
 * 
 * NOTE: This is a school project with NO Firebase Auth.
 * Users are selected from Firestore user collection.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userSessionManager: UserSessionManager
) : BaseViewModel<AuthUiState>() {
    
    override fun createInitialState(): AuthUiState {
        return AuthUiState()
    }
    
    /**
     * Current logged-in user.
     * Delegates to UserSessionManager for centralized session management.
     * This ensures all screens can access the same user state.
     */
    val currentUser: StateFlow<User?> = userSessionManager.currentUser
    
    /**
     * Login with email and password.
     * Validates credentials against Firestore users collection and role, then sets current user.
     * 
     * AUTHENTICATION FLOW:
     * 1. Validate input (email and password are required)
     * 2. Query Firestore users collection by email
     * 3. Verify password matches
     * 4. Validate user role
     * 5. Set current user and return success
     * 
     * @param username Email address
     * @param password User password (required)
     * @return LoginResult indicating success or failure with reason
     */
    suspend fun login(username: String, password: String): LoginResult {
        Log.d("AuthViewModel", "login: Attempting login with email='$username'")
        updateState { copy(isLoading = true, error = null) }
        
        return try {
            // Validate input
            if (username.isBlank()) {
                Log.w("AuthViewModel", "login: Email is blank")
                updateState { copy(isLoading = false, error = "Email is required") }
                return LoginResult.Failure("Email is required")
            }
            
            if (password.isBlank()) {
                Log.w("AuthViewModel", "login: Password is blank")
                updateState { copy(isLoading = false, error = "Password is required") }
                return LoginResult.Failure("Password is required")
            }
            
            val trimmedEmail = username.trim().lowercase()
            Log.d("AuthViewModel", "login: Normalized email='$trimmedEmail'")
            
            // Find user by email in Firestore users collection
            val user = userRepository.getUserByEmail(trimmedEmail)
            if (user == null) {
                Log.w("AuthViewModel", "login: User not found for email='$trimmedEmail'")
                updateState { 
                    copy(
                        isLoading = false, 
                        error = "Invalid email or password"
                    ) 
                }
                return LoginResult.Failure("Invalid email or password")
            }
            
            Log.d("AuthViewModel", "login: User found - id='${user.id}', name='${user.name}', role='${user.role}'")
            Log.d("AuthViewModel", "login: Stored password length=${user.password.length}, entered password length=${password.length}")
            Log.d("AuthViewModel", "login: Stored password='${user.password.replace(Regex("."), "*")}' (masked), entered password='${password.replace(Regex("."), "*")}' (masked)")
            
            // Validate password - must match exactly
            if (user.password.isBlank()) {
                Log.w("AuthViewModel", "login: User has no password set - id='${user.id}'")
                updateState { 
                    copy(
                        isLoading = false, 
                        error = "User account has no password set"
                    ) 
                }
                return LoginResult.Failure("User account has no password set")
            }
            
            // Trim both passwords for comparison (in case of whitespace issues)
            val storedPassword = user.password.trim()
            val enteredPassword = password.trim()
            
            Log.d("AuthViewModel", "login: Comparing passwords - stored='${storedPassword.replace(Regex("."), "*")}' (masked), entered='${enteredPassword.replace(Regex("."), "*")}' (masked)")
            Log.d("AuthViewModel", "login: Passwords match: ${storedPassword == enteredPassword}")
            
            if (storedPassword != enteredPassword) {
                Log.w("AuthViewModel", "login: Password mismatch for user id='${user.id}'")
                Log.w("AuthViewModel", "login: Stored password chars: ${storedPassword.map { it.code }}")
                Log.w("AuthViewModel", "login: Entered password chars: ${enteredPassword.map { it.code }}")
                updateState { 
                    copy(
                        isLoading = false, 
                        error = "Invalid email or password"
                    ) 
                }
                return LoginResult.Failure("Invalid email or password")
            }
            
            Log.d("AuthViewModel", "login: Password validated successfully")
            
            // Validate role is valid
            if (user.role !in UserRole.values()) {
                Log.e("AuthViewModel", "login: Invalid role='${user.role}' for user id='${user.id}'")
                updateState { 
                    copy(
                        isLoading = false, 
                        error = "Invalid user role"
                    ) 
                }
                return LoginResult.Failure("Invalid user role: ${user.role}")
            }
            
            // Validation passed - set current user in session manager
            userSessionManager.setUser(user)
            Log.i("AuthViewModel", "login: Current user set in session manager - id='${user.id}', name='${user.name}', role='${user.role}'")
            Log.i("AuthViewModel", "login: UserSessionManager.currentUser is now: ${userSessionManager.getUser()?.id}")
            
            updateState { 
                copy(
                    isLoading = false, 
                    error = null
                ) 
            }
            
            Log.i("AuthViewModel", "login: Login successful - user id='${user.id}', name='${user.name}', role='${user.role}'")
            
            // Return success with role for navigation
            LoginResult.Success(user.role)
            
        } catch (e: Exception) {
            Log.e("AuthViewModel", "login: Exception during login", e)
            updateState { 
                copy(
                    isLoading = false, 
                    error = "Login failed: ${e.message}"
                ) 
            }
            LoginResult.Failure("Login failed: ${e.message}")
        }
    }
    
    /**
     * Logout current user.
     */
    fun logout() {
        Log.d("AuthViewModel", "logout: Logging out user - current user id='${userSessionManager.getUser()?.id}'")
        userSessionManager.clearUser()
        setState(createInitialState())
        Log.d("AuthViewModel", "logout: User logged out - UserSessionManager.currentUser is now: ${userSessionManager.getUser()?.id}")
    }
}

/**
 * Auth UI State.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val username: String = "",
    val password: String = ""
)

/**
 * Login result sealed class.
 * Used to determine navigation destination after login.
 */
sealed class LoginResult {
    /**
     * Login successful. Contains the user's role for navigation routing.
     */
    data class Success(val role: UserRole) : LoginResult()
    
    /**
     * Login failed. Contains error message.
     */
    data class Failure(val message: String) : LoginResult()
}


