package com.example.snaporder.feature.history

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.firestore.OrderRepository
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.session.UserSessionManager
import com.example.snaporder.core.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Order History ViewModel for managing order history screen state.
 * 
 * ARCHITECTURE:
 * - Uses OrderRepository to fetch orders from Firestore
 * - Filters orders by current logged-in user ID
 * - Manages UI state via StateFlow
 * - Handles real-time updates from Firestore
 */
@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val userSessionManager: UserSessionManager
) : BaseViewModel<OrderHistoryUiState>() {
    
    private var ordersCollectionJob: Job? = null
    
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
     * Load order history from Firestore for the current user.
     * Called automatically on ViewModel creation.
     */
    fun loadOrders() {
        val currentUser = userSessionManager.getUser()
        val userId = currentUser?.id
        
        if (userId.isNullOrBlank()) {
            Log.w("OrderHistoryViewModel", "loadOrders: No user logged in")
            updateState {
                copy(
                    isLoading = false,
                    orders = emptyList(),
                    errorMessage = "Please log in to view order history"
                )
            }
            return
        }
        
        Log.d("OrderHistoryViewModel", "loadOrders: Loading orders for user ID='$userId'")
        
        // Cancel previous collection if any
        ordersCollectionJob?.cancel()
        
        updateState { copy(isLoading = true, errorMessage = null) }
        
        // Observe orders from Firestore (real-time updates)
        ordersCollectionJob = viewModelScope.launch {
            orderRepository.getUserOrders(userId)
                .catch { error ->
                    Log.e("OrderHistoryViewModel", "loadOrders: Error loading orders", error)
                    updateState {
                        copy(
                            isLoading = false,
                            orders = emptyList(),
                            errorMessage = "Failed to load orders: ${error.message}"
                        )
                    }
                }
                .collect { orders ->
                    Log.d("OrderHistoryViewModel", "loadOrders: Received ${orders.size} orders from Firestore")
                    
                    // Orders are already sorted by createdAt DESC from repository
                    updateState {
                        copy(
                            isLoading = false,
                            orders = orders,
                            errorMessage = null
                        )
                    }
                }
        }
    }
    
    /**
     * Refresh order history.
     * Called when user pulls to refresh.
     * Reloads orders from Firestore.
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

