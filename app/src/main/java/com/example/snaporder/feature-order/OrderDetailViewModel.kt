package com.example.snaporder.feature.order

import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.data.OrderHistoryRepository
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Order Detail ViewModel for managing order detail screen state.
 * 
 * ARCHITECTURE:
 * - Uses OrderHistoryRepository interface (currently FakeOrderHistoryRepository)
 * - Can swap to Firestore implementation without changing this code
 * - Manages UI state via StateFlow
 * - Loads a single order by ID
 * 
 * FUTURE INTEGRATION:
 * - Will receive orderId from navigation arguments
 * - Will fetch real Order from Firestore using OrderRepository.getOrderById()
 * - Will handle real-time order status updates
 */
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderHistoryRepository: OrderHistoryRepository
) : BaseViewModel<OrderDetailUiState>() {
    
    override fun createInitialState(): OrderDetailUiState {
        return OrderDetailUiState(
            isLoading = true,
            order = null,
            errorMessage = null
        )
    }
    
    private var currentOrderId: String? = null
    
    /**
     * Load order by ID.
     * Called when screen is opened with an orderId.
     * 
     * @param orderId The ID of the order to load
     */
    fun loadOrder(orderId: String) {
        if (currentOrderId == orderId && currentState.order != null) {
            // Already loaded, no need to reload
            return
        }
        
        currentOrderId = orderId
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            try {
                val order = orderHistoryRepository.getOrderById(orderId)
                
                if (order == null) {
                    updateState {
                        copy(
                            isLoading = false,
                            order = null,
                            errorMessage = "Order not found"
                        )
                    }
                } else {
                    updateState {
                        copy(
                            isLoading = false,
                            order = order,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        order = null,
                        errorMessage = "Failed to load order: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Refresh order data.
     * Called when user pulls to refresh.
     */
    fun onRefresh() {
        currentOrderId?.let { orderId ->
            loadOrder(orderId)
        }
    }
    
    /**
     * Handle back button click.
     * Navigation will be handled by the screen composable.
     */
    fun onBackClick() {
        // Navigation logic will be handled in the screen
    }
    
    /**
     * Calculate subtotal from order items.
     * In the UI, this will be displayed separately from totalPrice.
     */
    fun calculateSubtotal(order: Order): Int {
        return order.items.sumOf { it.totalPrice }.toInt()
    }
    
    /**
     * Get service fee.
     * Currently calculated as: totalPrice - subtotal
     * In a real implementation, this could be stored in the order or fetched from settings.
     */
    fun getServiceFee(order: Order): Int {
        val subtotal = calculateSubtotal(order)
        val total = order.totalPrice.toInt()
        return total - subtotal
    }
}

/**
 * Order Detail UI State.
 * 
 * Contains all state needed for the order detail screen:
 * - Loading state
 * - Order data (with calculated subtotal/serviceFee for display)
 * - Error message (if any)
 */
data class OrderDetailUiState(
    val isLoading: Boolean = false,
    val order: Order? = null,
    val errorMessage: String? = null
)

