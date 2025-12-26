package com.example.snaporder.feature.menu

import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.data.MenuDataSource
import com.example.snaporder.core.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Menu ViewModel for managing menu screen state.
 * 
 * ARCHITECTURE:
 * - Uses MenuDataSource interface (currently FakeMenuRepository)
 * - Can swap to Firestore implementation without changing this code
 * - Manages UI state via StateFlow
 * - Handles cart item count for UI display (not real cart logic yet)
 */
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuDataSource: MenuDataSource
) : BaseViewModel<MenuUiState>() {
    
    override fun createInitialState(): MenuUiState {
        return MenuUiState(
            isLoading = true,
            items = emptyList(),
            cartItemCount = 0,
            errorMessage = null
        )
    }
    
    init {
        loadMenus()
    }
    
    /**
     * Load menu items from data source.
     * Called automatically on ViewModel creation.
     * Can be called manually to retry after error.
     */
    fun loadMenus() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            try {
                val menus = menuDataSource.getMenus()
                updateState {
                    copy(
                        isLoading = false,
                        items = menus,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = "Failed to load menu: ${e.message}",
                        items = emptyList()
                    )
                }
            }
        }
    }
    
    /**
     * Handle add to cart action.
     * Currently just increments cartItemCount for UI display.
     * Real cart logic will be implemented later.
     * 
     * @param itemId The ID of the menu item to add
     */
    fun onAddToCart(itemId: String) {
        val item = currentState.items.find { it.id == itemId }
        if (item != null && item.available) {
            updateState {
                copy(cartItemCount = cartItemCount + 1)
            }
        }
    }
    
    /**
     * Handle cart icon click.
     * Navigation will be handled by the screen composable.
     */
    fun onCartClick() {
        // Navigation logic will be handled in the screen
        // This function is here for future cart logic
    }
}

/**
 * Menu UI State.
 * 
 * Contains all state needed for the menu screen:
 * - Loading state
 * - Menu items list
 * - Cart item count (for badge display)
 * - Error message (if any)
 */
data class MenuUiState(
    val isLoading: Boolean = false,
    val items: List<com.example.snaporder.core.model.MenuItem> = emptyList(),
    val cartItemCount: Int = 0,
    val errorMessage: String? = null
)
