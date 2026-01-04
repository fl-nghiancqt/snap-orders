package com.example.snaporder.feature.menu

import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.data.CartRepository
import com.example.snaporder.core.firestore.MenuRepository
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
 * - Uses MenuRepository to observe menu items in real-time (Flow)
 * - Uses CartRepository for cart operations
 * - Observes cart changes to update cart item count
 * - Manages UI state via StateFlow
 */
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
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
        observeMenus()
        observeCartItems()
    }
    
    /**
     * Observe menu items from Firestore in real-time.
     * Uses Flow to automatically update UI when data changes.
     */
    private fun observeMenus() {
        menuRepository.getAvailableMenuItems()
            .onEach { menus ->
                updateState {
                    copy(
                        isLoading = false,
                        items = menus,
                        errorMessage = null
                    )
                }
            }
            .catch { e ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = "Failed to load menu: ${e.message}",
                        items = emptyList()
                    )
                }
            }
            .launchIn(viewModelScope)
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
     * Refresh menu items manually.
     * Since we're observing a Flow, items update automatically.
     * This function is kept for API compatibility but doesn't need to do anything.
     */
    fun refreshMenus() {
        // Menu items update automatically via Flow observer
        // No action needed - Flow will emit new data when Firestore changes
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
 * - Refreshing state (for pull-to-refresh)
 */
data class MenuUiState(
    val isLoading: Boolean = false,
    val items: List<com.example.snaporder.core.model.MenuItem> = emptyList(),
    val cartItemCount: Int = 0,
    val errorMessage: String? = null
)
