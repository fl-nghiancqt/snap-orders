package com.example.snaporder.feature.cart

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.data.CartRepository
import com.example.snaporder.core.firestore.OrderRepository
import com.example.snaporder.core.model.CartItem
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.model.OrderDraft
import com.example.snaporder.core.model.OrderItem
import com.example.snaporder.core.model.OrderStatus
import com.example.snaporder.core.utils.OrderBusinessLogic
import com.example.snaporder.core.viewmodel.BaseViewModel
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
 * - Validates table availability before creating order
 * - Creates new order or adds to existing open order
 */
@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val orderBusinessLogic: OrderBusinessLogic,
    private val orderRepository: OrderRepository
) : BaseViewModel<CartUiState>() {
    
    private val _orderPlacementResult = MutableStateFlow<OrderPlacementResult?>(null)
    val orderPlacementResult: StateFlow<OrderPlacementResult?> = _orderPlacementResult.asStateFlow()
    
    /**
     * Order placement result.
     */
    sealed class OrderPlacementResult {
        data class Success(val orderId: String, val isNewOrder: Boolean) : OrderPlacementResult()
        data class Error(val message: String) : OrderPlacementResult()
    }
    
    override fun createInitialState(): CartUiState {
        return CartUiState(
            isLoading = true,
            orderDraft = null,
            errorMessage = null
        )
    }
    
    init {
        observeCartItems()
    }
    
    /**
     * Observe cart items for real-time updates.
     * This ensures the cart screen updates when items are added from menu screen.
     */
    private fun observeCartItems() {
        cartRepository.getCartItemsFlow()
            .onEach { cartItems ->
                val orderDraft = if (cartItems.isNotEmpty()) {
                    createOrderDraft(cartItems)
                } else {
                    null
                }
                
                updateState {
                    copy(
                        isLoading = false,
                        orderDraft = orderDraft,
                        errorMessage = null
                    )
                }
            }
            .catch { e ->
                updateState {
                    copy(
                        isLoading = false,
                        orderDraft = null,
                        errorMessage = "Failed to load cart: ${e.message}"
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Load cart items from repository (one-time fetch).
     * Used for initial load or manual refresh.
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
     * Updates the cart repository, which will trigger Flow update.
     * 
     * @param itemId The ID of the cart item
     */
    fun onIncreaseQuantity(itemId: String) {
        viewModelScope.launch {
            val currentDraft = currentState.orderDraft ?: return@launch
            val item = currentDraft.items.find { it.id == itemId } ?: return@launch
            
            cartRepository.updateQuantity(itemId, item.quantity + 1)
            // Cart will update automatically via observeCartItems()
        }
    }
    
    /**
     * Decrease quantity of a cart item.
     * If quantity reaches 0, the item is removed.
     * Updates the cart repository, which will trigger Flow update.
     * 
     * @param itemId The ID of the cart item
     */
    fun onDecreaseQuantity(itemId: String) {
        viewModelScope.launch {
            val currentDraft = currentState.orderDraft ?: return@launch
            val item = currentDraft.items.find { it.id == itemId } ?: return@launch
            
            val newQuantity = item.quantity - 1
            if (newQuantity <= 0) {
                cartRepository.removeItem(itemId)
            } else {
                cartRepository.updateQuantity(itemId, newQuantity)
            }
            // Cart will update automatically via observeCartItems()
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
     * BUSINESS LOGIC:
     * - Validates cart has items and table number is set
     * - Checks if table has existing open order
     * - If table has open order (CREATED or PREPARING): adds items to existing order
     * - If table has PAID order or no order: creates new order
     * - If table has open order that cannot accept items: shows error
     * 
     * @param userId The ID of the user placing the order
     */
    fun onPlaceOrderClick(userId: String) {
        Log.d("CartViewModel", "onPlaceOrderClick: Called with userId='$userId'")
        
        val orderDraft = currentState.orderDraft
        
        if (orderDraft == null || !orderDraft.isValid) {
            Log.w("CartViewModel", "onPlaceOrderClick: Order draft is invalid or null")
            updateState {
                copy(errorMessage = "Please add items and select a table number")
            }
            _orderPlacementResult.value = OrderPlacementResult.Error("Please add items and select a table number")
            return
        }
        
        val tableNumber = orderDraft.tableNumber
        if (tableNumber == null) {
            Log.w("CartViewModel", "onPlaceOrderClick: Table number is null")
            updateState {
                copy(errorMessage = "Please enter a table number")
            }
            _orderPlacementResult.value = OrderPlacementResult.Error("Please enter a table number")
            return
        }
        
        Log.d("CartViewModel", "onPlaceOrderClick: ========================================")
        Log.d("CartViewModel", "onPlaceOrderClick: REAL ORDER DATA STRUCTURE:")
        Log.d("CartViewModel", "onPlaceOrderClick: ========================================")
        Log.d("CartViewModel", "onPlaceOrderClick: Order draft valid - table=$tableNumber, items=${orderDraft.items.size}, userId='$userId'")
        orderDraft.items.forEachIndexed { index, item ->
            Log.d("CartViewModel", "onPlaceOrderClick: CartItem $index:")
            Log.d("CartViewModel", "onPlaceOrderClick:   id: '${item.id}'")
            Log.d("CartViewModel", "onPlaceOrderClick:   menuItemId: '${item.menuItemId}'")
            Log.d("CartViewModel", "onPlaceOrderClick:   name: '${item.name}'")
            Log.d("CartViewModel", "onPlaceOrderClick:   price: ${item.price} (type: ${item.price.javaClass.simpleName})")
            Log.d("CartViewModel", "onPlaceOrderClick:   quantity: ${item.quantity} (type: ${item.quantity.javaClass.simpleName})")
            Log.d("CartViewModel", "onPlaceOrderClick:   subtotal: ${item.subtotal}")
        }
        Log.d("CartViewModel", "onPlaceOrderClick: ========================================")
        
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            _orderPlacementResult.value = null
            
            try {
                Log.d("CartViewModel", "onPlaceOrderClick: Calling processOrderPlacement")
                val result = orderBusinessLogic.processOrderPlacement(
                    tableNumber = tableNumber,
                    cartItems = orderDraft.items,
                    userId = userId
                )
                
                Log.d("CartViewModel", "onPlaceOrderClick: processOrderPlacement returned result")
                
                result.fold(
                    onSuccess = { placementResult ->
                        Log.i("CartViewModel", "onPlaceOrderClick: Order placement successful - $placementResult")
                        
                        
                        when (placementResult) {
                            is OrderBusinessLogic.OrderPlacementResult.Created -> {
                                Log.i("CartViewModel", "onPlaceOrderClick: New order created - id='${placementResult.orderId}'")
                                _orderPlacementResult.value = OrderPlacementResult.Success(
                                    orderId = placementResult.orderId,
                                    isNewOrder = true
                                )
                                updateState { copy(isLoading = false, errorMessage = null) }
                            }
                            is OrderBusinessLogic.OrderPlacementResult.Updated -> {
                                Log.i("CartViewModel", "onPlaceOrderClick: Existing order updated - id='${placementResult.orderId}'")
                                _orderPlacementResult.value = OrderPlacementResult.Success(
                                    orderId = placementResult.orderId,
                                    isNewOrder = false
                                )
                                updateState { copy(isLoading = false, errorMessage = null) }
                            }
                        }
                        // Clear cart after successful order placement
                        cartRepository.clearCart()
                        Log.d("CartViewModel", "onPlaceOrderClick: Cart cleared")
                    },
                    onFailure = { error ->
                        Log.e("CartViewModel", "onPlaceOrderClick: Order placement failed", error)
                        updateState {
                            copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to place order"
                            )
                        }
                        _orderPlacementResult.value = OrderPlacementResult.Error(
                            error.message ?: "Failed to place order"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("CartViewModel", "onPlaceOrderClick: Exception during order placement", e)
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = "Failed to place order: ${e.message}"
                    )
                }
                _orderPlacementResult.value = OrderPlacementResult.Error(
                    "Failed to place order: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear order placement result.
     * Call this after navigating away from the cart screen.
     */
    fun clearOrderPlacementResult() {
        _orderPlacementResult.value = null
    }
    
    /**
     * Set error message in UI state.
     * Used for displaying errors from UI layer (e.g., missing user).
     */
    fun setErrorMessage(message: String) {
        updateState {
            copy(errorMessage = message)
        }
    }
    
    /**
     * DEBUG FUNCTION: Create a test order directly in Firestore.
     * This bypasses all business logic and creates an order with hardcoded test data.
     * Use this to verify Firestore connection and order creation works.
     */
    fun createTestOrderDirectly(userId: String) {
        Log.d("CartViewModel", "========================================")
        Log.d("CartViewModel", "DEBUG: createTestOrderDirectly called")
        Log.d("CartViewModel", "========================================")
        
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            _orderPlacementResult.value = null
            
            try {
                // Create test order with hardcoded data
                val testItems = listOf(
                    OrderItem(
                        menuItemId = "test_item_1",
                        menuItemName = "Test Item 1",
                        price = 30000.0,
                        quantity = 2
                    ),
                    OrderItem(
                        menuItemId = "test_item_2",
                        menuItemName = "Test Item 2",
                        price = 25000.0,
                        quantity = 1
                    )
                )
                val subtotal = testItems.sumOf { it.price * it.quantity }
                val serviceFee = 10000.0
                val totalPrice = subtotal + serviceFee
                
                val testOrder = Order(
                    id = "", // Will be set by Firestore
                    tableNumber = 99, // Test table number
                    status = OrderStatus.CREATED,
                    items = testItems,
                    totalPrice = totalPrice,
                    createdAt = Timestamp.now(),
                    userId = userId.ifBlank { "test_user" }
                )
                
                Log.d("CartViewModel", "DEBUG: ========================================")
                Log.d("CartViewModel", "DEBUG: TEST ORDER DATA STRUCTURE:")
                Log.d("CartViewModel", "DEBUG: ========================================")
                Log.d("CartViewModel", "DEBUG: Test order created:")
                Log.d("CartViewModel", "DEBUG:   - Table: ${testOrder.tableNumber} (type: ${testOrder.tableNumber.javaClass.simpleName})")
                Log.d("CartViewModel", "DEBUG:   - Items: ${testOrder.items.size}")
                Log.d("CartViewModel", "DEBUG:   - Subtotal: $subtotal (type: ${subtotal.javaClass.simpleName})")
                Log.d("CartViewModel", "DEBUG:   - Service Fee: $serviceFee (type: ${serviceFee.javaClass.simpleName})")
                Log.d("CartViewModel", "DEBUG:   - Total: ${testOrder.totalPrice} (type: ${testOrder.totalPrice.javaClass.simpleName})")
                Log.d("CartViewModel", "DEBUG:   - Status: ${testOrder.status}")
                Log.d("CartViewModel", "DEBUG:   - UserId: '${testOrder.userId}'")
                testOrder.items.forEachIndexed { index, item ->
                    Log.d("CartViewModel", "DEBUG:   - Item $index:")
                    Log.d("CartViewModel", "DEBUG:     menuItemId: '${item.menuItemId}'")
                    Log.d("CartViewModel", "DEBUG:     menuItemName: '${item.menuItemName}'")
                    Log.d("CartViewModel", "DEBUG:     price: ${item.price} (type: ${item.price.javaClass.simpleName})")
                    Log.d("CartViewModel", "DEBUG:     quantity: ${item.quantity} (type: ${item.quantity.javaClass.simpleName})")
                    Log.d("CartViewModel", "DEBUG:     totalPrice: ${item.totalPrice}")
                }
                Log.d("CartViewModel", "DEBUG: ========================================")
                
                Log.d("CartViewModel", "DEBUG: Calling orderRepository.createOrder()...")
                val result = orderRepository.createOrder(testOrder)
                
                result.fold(
                    onSuccess = { orderId ->
                        Log.i("CartViewModel", "========================================")
                        Log.i("CartViewModel", "DEBUG: ✓ Test order created successfully!")
                        Log.i("CartViewModel", "DEBUG: Order ID: $orderId")
                        Log.i("CartViewModel", "========================================")
                        
                        updateState { copy(isLoading = false, errorMessage = null) }
                        _orderPlacementResult.value = OrderPlacementResult.Success(
                            orderId = orderId,
                            isNewOrder = true
                        )
                    },
                    onFailure = { error ->
                        Log.e("CartViewModel", "========================================")
                        Log.e("CartViewModel", "DEBUG: ✗ Test order creation FAILED")
                        Log.e("CartViewModel", "DEBUG: Error: ${error.message}")
                        Log.e("CartViewModel", "DEBUG: Error type: ${error.javaClass.simpleName}")
                        error.printStackTrace()
                        Log.e("CartViewModel", "========================================")
                        
                        updateState {
                            copy(
                                isLoading = false,
                                errorMessage = "Debug test failed: ${error.message}"
                            )
                        }
                        _orderPlacementResult.value = OrderPlacementResult.Error(
                            "Debug test failed: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("CartViewModel", "DEBUG: Exception during test order creation", e)
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = "Debug test exception: ${e.message}"
                    )
                }
                _orderPlacementResult.value = OrderPlacementResult.Error(
                    "Debug test exception: ${e.message}"
                )
            }
        }
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

