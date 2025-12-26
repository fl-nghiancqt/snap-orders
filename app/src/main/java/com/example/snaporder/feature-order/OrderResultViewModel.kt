package com.example.snaporder.feature.order

import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.model.OrderItem
import com.example.snaporder.core.model.OrderStatus
import com.example.snaporder.core.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Order Result ViewModel for managing order success screen state.
 * 
 * ARCHITECTURE:
 * - Loads a fake Order object for UI development
 * - Can swap to real Firestore implementation without changing UI code
 * - Manages UI state via StateFlow
 * - Calculates subtotal and serviceFee from order items
 * 
 * FUTURE INTEGRATION:
 * - Will receive Order ID from navigation arguments
 * - Will fetch real Order from Firestore using OrderRepository
 * - Will handle order status updates
 */
@HiltViewModel
class OrderResultViewModel @Inject constructor() : BaseViewModel<OrderResultUiState>() {
    
    override fun createInitialState(): OrderResultUiState {
        return OrderResultUiState(
            isLoading = true,
            order = null,
            errorMessage = null
        )
    }
    
    init {
        loadOrder()
    }
    
    /**
     * Load order data.
     * In a real implementation, this would fetch from Firestore using OrderRepository.
     * Currently loads fake data for UI development.
     */
    private fun loadOrder() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            try {
                delay(500) // Simulate network delay
                
                // Create fake order matching sample data requirements
                val fakeOrder = createFakeOrder()
                
                updateState {
                    copy(
                        isLoading = false,
                        order = fakeOrder,
                        errorMessage = null
                    )
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
     * Create fake order for UI development.
     * 
     * SAMPLE DATA (as per requirements):
     * - Order ID: ORD-001
     * - Table: 5
     * - Status: CREATED
     * - Items:
     *   - Trà đào – 30,000 x 2
     *   - Bạc xỉu – 35,000 x 1
     * - Subtotal: 95,000
     * - Service fee: 10,000
     * - Total: 105,000
     */
    private fun createFakeOrder(): Order {
        val items = listOf(
            OrderItem(
                menuItemId = "1",
                menuItemName = "Trà đào",
                price = 30000.0,
                quantity = 2
            ),
            OrderItem(
                menuItemId = "2",
                menuItemName = "Bạc xỉu",
                price = 35000.0,
                quantity = 1
            )
        )
        
        val subtotal = items.sumOf { it.totalPrice }.toInt()
        val serviceFee = 10000 // Fixed service fee
        val totalPrice = subtotal + serviceFee
        
        return Order(
            id = "ORD-001",
            tableNumber = 5,
            status = OrderStatus.CREATED,
            items = items,
            totalPrice = totalPrice.toDouble(),
            createdAt = com.google.firebase.Timestamp.now(),
            userId = "user_123"
        )
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
     * Currently fixed at 10,000 VND.
     * In a real implementation, this could be calculated or fetched from settings.
     */
    fun getServiceFee(): Int {
        return 10000
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

