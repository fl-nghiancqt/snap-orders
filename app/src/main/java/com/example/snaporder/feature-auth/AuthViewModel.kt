package com.example.snaporder.feature.auth

import com.example.snaporder.core.firestore.UserRepository
import com.example.snaporder.core.model.User
import com.example.snaporder.core.model.UserRole
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
    private val userRepository: UserRepository
) : BaseViewModel<AuthUiState>() {
    
    override fun createInitialState(): AuthUiState {
        return AuthUiState()
    }
    
    /**
     * Current logged-in user.
     * Set after successful login validation.
     */
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    /**
     * Login with username/email and password.
     * Validates credentials and role, then sets current user.
     * 
     * @param username Email or username
     * @param password User password (optional - if empty, skip password validation for school project)
     * @return LoginResult indicating success or failure with reason
     */
    suspend fun login(username: String, password: String): LoginResult {
        updateState { copy(isLoading = true, error = null) }
        
        return try {
            // Validate input
            if (username.isBlank()) {
                updateState { copy(isLoading = false, error = "Username is required") }
                return LoginResult.Failure("Username is required")
            }
            
            // Find user by email/username
            val user = userRepository.getUserByEmail(username.trim())
            if (user == null) {
                updateState { 
                    copy(
                        isLoading = false, 
                        error = "Invalid username or password"
                    ) 
                }
                return LoginResult.Failure("Invalid username or password")
            }
            
            // Validate password if provided (optional for school project)
            if (password.isNotBlank() && user.password.isNotBlank()) {
                if (user.password != password) {
                    updateState { 
                        copy(
                            isLoading = false, 
                            error = "Invalid username or password"
                        ) 
                    }
                    return LoginResult.Failure("Invalid username or password")
                }
            }
            
            // Validate role is valid
            if (user.role !in UserRole.values()) {
                updateState { 
                    copy(
                        isLoading = false, 
                        error = "Invalid user role"
                    ) 
                }
                return LoginResult.Failure("Invalid user role: ${user.role}")
            }
            
            // Validation passed - set current user
            _currentUser.value = user
            updateState { 
                copy(
                    isLoading = false, 
                    error = null
                ) 
            }
            
            // Return success with role for navigation
            LoginResult.Success(user.role)
            
        } catch (e: Exception) {
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
        _currentUser.value = null
        setState(createInitialState())
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


