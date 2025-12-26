package com.example.snaporder.feature.history

import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.data.OrderHistoryRepository
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Order History ViewModel for managing order history screen state.
 * 
 * ARCHITECTURE:
 * - Uses OrderHistoryRepository interface (currently FakeOrderHistoryRepository)
 * - Can swap to Firestore implementation without changing this code
 * - Manages UI state via StateFlow
 * - Handles order list loading and refresh
 * 
 * FUTURE INTEGRATION:
 * - Will filter orders by current logged-in user ID
 * - Will handle real-time updates from Firestore
 * - Will support pagination for large order lists
 */
@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val orderHistoryRepository: OrderHistoryRepository
) : BaseViewModel<OrderHistoryUiState>() {
    
    override fun createInitialState(): OrderHistoryUiState {
        return OrderHistoryUiState(
            isLoading = true,
            orders = emptyList(),
            errorMessage = null
        )
    }
    
    init {
        loadOrders()
    }
    
    /**
     * Load order history from repository.
     * Called automatically on ViewModel creation.
     */
    fun loadOrders() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            try {
                val orders = orderHistoryRepository.getOrdersByUser()
                // Sort by createdAt descending (newest first)
                val sortedOrders = orders.sortedByDescending { 
                    it.createdAt?.seconds ?: 0L 
                }
                
                updateState {
                    copy(
                        isLoading = false,
                        orders = sortedOrders,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        orders = emptyList(),
                        errorMessage = "Failed to load orders: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Refresh order history.
     * Called when user pulls to refresh.
     */
    fun onRefresh() {
        loadOrders()
    }
    
    /**
     * Handle order item click.
     * Navigation will be handled by the screen composable.
     * 
     * @param orderId The ID of the clicked order
     */
    fun onOrderClick(orderId: String) {
        // Navigation logic will be handled in the screen
        // This function is here for future order detail logic
    }
}

/**
 * Order History UI State.
 * 
 * Contains all state needed for the order history screen:
 * - Loading state
 * - List of orders
 * - Error message (if any)
 */
data class OrderHistoryUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val errorMessage: String? = null
)

