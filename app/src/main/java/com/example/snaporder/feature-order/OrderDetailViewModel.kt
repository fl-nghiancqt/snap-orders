package com.example.snaporder.feature.order

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.firestore.OrderRepository
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Order Detail ViewModel for managing order detail screen state.
 * 
 * ARCHITECTURE:
 * - Uses OrderRepository to fetch order from Firestore
 * - Manages UI state via StateFlow
 * - Loads a single order by ID from Firestore
 */
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : BaseViewModel<OrderDetailUiState>() {
    
    override fun createInitialState(): OrderDetailUiState {
        return OrderDetailUiState(
            isLoading = false,
            order = null,
            errorMessage = null
        )
    }
    
    private var currentOrderId: String? = null
    
    /**
     * Load order by ID from Firestore.
     * Called when screen is opened with an orderId.
     * 
     * @param orderId The ID of the order to load
     */
    fun loadOrder(orderId: String) {
        if (orderId.isBlank()) {
            updateState {
                copy(
                    isLoading = false,
                    order = null,
                    errorMessage = "Order ID is required"
                )
            }
            return
        }
        
        if (currentOrderId == orderId && currentState.order != null) {
            // Already loaded, no need to reload
            return
        }
        
        currentOrderId = orderId
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            try {
                Log.d("OrderDetailViewModel", "loadOrder: Loading order with ID='$orderId'")
                
                val order = orderRepository.getOrder(orderId)
                
                if (order == null) {
                    Log.w("OrderDetailViewModel", "loadOrder: Order not found with ID='$orderId'")
                    updateState {
                        copy(
                            isLoading = false,
                            order = null,
                            errorMessage = "Order not found"
                        )
                    }
                } else {
                    Log.d("OrderDetailViewModel", "loadOrder: Order loaded successfully - table=${order.tableNumber}, items=${order.items.size}, total=${order.totalPrice}")
                    updateState {
                        copy(
                            isLoading = false,
                            order = order,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("OrderDetailViewModel", "loadOrder: Error loading order", e)
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

