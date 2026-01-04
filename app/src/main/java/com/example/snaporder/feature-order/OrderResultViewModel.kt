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
 * Order Result ViewModel for managing order success screen state.
 * 
 * ARCHITECTURE:
 * - Fetches real Order from Firestore using OrderRepository
 * - Manages UI state via StateFlow
 * - Loads order by ID from navigation arguments
 */
@HiltViewModel
class OrderResultViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : BaseViewModel<OrderResultUiState>() {
    
    override fun createInitialState(): OrderResultUiState {
        return OrderResultUiState(
            isLoading = false,
            order = null,
            errorMessage = null
        )
    }
    
    /**
     * Load order data from Firestore by order ID.
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
        
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            try {
                Log.d("OrderResultViewModel", "loadOrder: Loading order with ID='$orderId'")
                
                val order = orderRepository.getOrder(orderId)
                order?.let {
                    Log.d("OrderResultViewModel", "loadOrder: Order loaded successfully - table=${it.tableNumber}, items=${it.items.size}, total=${it.totalPrice}")
                    
                    updateState {
                        copy(
                            isLoading = false,
                            order = it,
                            errorMessage = null
                        )
                    }
                } ?: run {
                    Log.w("OrderResultViewModel", "loadOrder: Order not found with ID='$orderId'")
                    updateState {
                        copy(
                            isLoading = false,
                            order = null,
                            errorMessage = "Order not found"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("OrderResultViewModel", "loadOrder: Error loading order", e)
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
     * Handle back to menu button click.
     * Navigation will be handled by the screen composable.
     */
    fun onBackToMenuClick() {
        // Navigation logic will be handled in the screen
    }
    
    /**
     * Handle view order detail button click.
     * Navigation will be handled by the screen composable.
     */
    fun onViewOrderDetailClick() {
        // Navigation logic will be handled in the screen
    }
}

/**
 * Order Result UI State.
 * 
 * Contains all state needed for the order result screen:
 * - Loading state
 * - Order data (with calculated subtotal/serviceFee for display)
 * - Error message (if any)
 */
data class OrderResultUiState(
    val isLoading: Boolean = false,
    val order: Order? = null,
    val errorMessage: String? = null
)

