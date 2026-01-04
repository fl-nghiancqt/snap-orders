package com.example.snaporder.feature.menu

import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.data.CartRepository
import com.example.snaporder.core.data.MenuDataSource
import com.example.snaporder.core.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Menu ViewModel for managing menu screen state.
 * 
 * ARCHITECTURE:
 * - Uses MenuDataSource interface for menu items
 * - Uses CartRepository for cart operations
 * - Observes cart changes to update cart item count
 * - Manages UI state via StateFlow
 */
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuDataSource: MenuDataSource,
    private val cartRepository: CartRepository
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
        observeCartItems()
    }
    
    /**
     * Observe cart items to update cart item count in real-time.
     */
    private fun observeCartItems() {
        cartRepository.getCartItemsFlow()
            .onEach { cartItems ->
                val totalCount = cartItems.sumOf { it.quantity }
                updateState { copy(cartItemCount = totalCount) }
            }
            .catch { e ->
                // Handle error silently or log it
            }
            .launchIn(viewModelScope)
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
     * Adds the menu item to the cart repository.
     * 
     * @param itemId The ID of the menu item to add
     */
    fun onAddToCart(itemId: String) {
        viewModelScope.launch {
            val item = currentState.items.find { it.id == itemId }
            if (item != null && item.available) {
                try {
                    cartRepository.addItem(
                        menuItemId = item.id,
                        name = item.name,
                        price = item.price
                    )
                    // Cart count will update automatically via observeCartItems()
                } catch (e: Exception) {
                    updateState {
                        copy(errorMessage = "Failed to add item to cart: ${e.message}")
                    }
                }
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
