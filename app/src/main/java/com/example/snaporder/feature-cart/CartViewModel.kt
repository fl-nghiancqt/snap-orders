package com.example.snaporder.feature.cart

import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.data.CartRepository
import com.example.snaporder.core.model.CartItem
import com.example.snaporder.core.model.OrderDraft
import com.example.snaporder.core.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Cart ViewModel for managing cart screen state and order draft.
 * 
 * ARCHITECTURE:
 * - Uses CartRepository interface (currently FakeCartRepository)
 * - Can swap to Firestore implementation without changing this code
 * - Manages UI state via StateFlow
 * - Calculates order totals (subtotal, serviceFee, totalPrice)
 * - Handles quantity changes and table number input
 * 
 * BUSINESS LOGIC:
 * - Service fee is fixed at 10,000 VND (fake for now)
 * - Total = Subtotal + Service Fee
 * - Order can only be placed if cart has items and table number is set
 * 
 * FUTURE INTEGRATION:
 * - onPlaceOrderClick() will call OrderBusinessLogic
 * - Will validate table availability
 * - Will create or append to existing order
 */
@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository
) : BaseViewModel<CartUiState>() {
    
    override fun createInitialState(): CartUiState {
        return CartUiState(
            isLoading = true,
            orderDraft = null,
            errorMessage = null
        )
    }
    
    init {
        loadCartItems()
    }
    
    /**
     * Load cart items from repository.
     * Called automatically on ViewModel creation.
     */
    private fun loadCartItems() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            try {
                val cartItems = cartRepository.getCartItems()
                val orderDraft = createOrderDraft(cartItems)
                
                updateState {
                    copy(
                        isLoading = false,
                        orderDraft = orderDraft,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        orderDraft = null,
                        errorMessage = "Failed to load cart: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Create OrderDraft from cart items.
     * Calculates subtotal, service fee, and total.
     * 
     * @param items List of cart items
     * @return OrderDraft with calculated totals
     */
    private fun createOrderDraft(items: List<CartItem>): OrderDraft {
        val subtotal = items.sumOf { it.subtotal }
        val serviceFee = 10000 // Fixed service fee (fake)
        val totalPrice = subtotal + serviceFee
        
        return OrderDraft(
            tableNumber = null, // User will input this
            items = items,
            subtotal = subtotal,
            serviceFee = serviceFee,
            totalPrice = totalPrice
        )
    }
    
    /**
     * Increase quantity of a cart item.
     * 
     * @param itemId The ID of the cart item
     */
    fun onIncreaseQuantity(itemId: String) {
        val currentDraft = currentState.orderDraft ?: return
        
        val updatedItems = currentDraft.items.map { item ->
            if (item.id == itemId) {
                item.copy(quantity = item.quantity + 1)
            } else {
                item
            }
        }
        
        val updatedDraft = createOrderDraft(updatedItems).copy(
            tableNumber = currentDraft.tableNumber
        )
        
        updateState { copy(orderDraft = updatedDraft) }
    }
    
    /**
     * Decrease quantity of a cart item.
     * If quantity reaches 0, the item is removed.
     * 
     * @param itemId The ID of the cart item
     */
    fun onDecreaseQuantity(itemId: String) {
        val currentDraft = currentState.orderDraft ?: return
        
        val updatedItems = currentDraft.items
            .map { item ->
                if (item.id == itemId) {
                    item.copy(quantity = (item.quantity - 1).coerceAtLeast(0))
                } else {
                    item
                }
            }
            .filter { it.quantity > 0 } // Remove items with quantity 0
        
        // If cart becomes empty, set orderDraft to null
        if (updatedItems.isEmpty()) {
            updateState { copy(orderDraft = null) }
        } else {
            val updatedDraft = createOrderDraft(updatedItems).copy(
                tableNumber = currentDraft.tableNumber
            )
            updateState { copy(orderDraft = updatedDraft) }
        }
    }
    
    /**
     * Update table number input.
     * 
     * @param value The table number string (can be empty or invalid)
     */
    fun onTableNumberChange(value: String) {
        val currentDraft = currentState.orderDraft ?: return
        
        val tableNumber = value.trim().toIntOrNull()
        
        val updatedDraft = currentDraft.copy(tableNumber = tableNumber)
        
        updateState { copy(orderDraft = updatedDraft) }
    }
    
    /**
     * Handle place order button click.
     * 
     * FUTURE IMPLEMENTATION:
     * - Validate table availability using OrderBusinessLogic
     * - Create new order or append to existing order
     * - Navigate to order result screen
     * 
     * Currently: No real logic, just validates UI state
     */
    fun onPlaceOrderClick() {
        val orderDraft = currentState.orderDraft
        
        if (orderDraft == null || !orderDraft.isValid) {
            updateState {
                copy(errorMessage = "Please add items and select a table number")
            }
            return
        }
        
        // TODO: Implement real order creation
        // - Call OrderBusinessLogic.validateTableAvailable()
        // - Call OrderBusinessLogic.createNewOrder() or addMoreToExistingOrder()
        // - Navigate to order result screen
    }
}

/**
 * Cart UI State.
 * 
 * Contains all state needed for the cart screen:
 * - Loading state
 * - OrderDraft (cart items, table number, totals)
 * - Error message (if any)
 */
data class CartUiState(
    val isLoading: Boolean = false,
    val orderDraft: OrderDraft? = null,
    val errorMessage: String? = null
)

