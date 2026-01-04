package com.example.snaporder.feature.order

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.data.CartRepository
import com.example.snaporder.core.data.MenuDataSource
import com.example.snaporder.core.firestore.OrderRepository
import com.example.snaporder.core.model.CartItem
import com.example.snaporder.core.model.MenuItem
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.model.OrderStatus
import com.example.snaporder.core.utils.OrderBusinessLogic
import com.example.snaporder.core.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val orderRepository: OrderRepository,
    private val orderBusinessLogic: OrderBusinessLogic,
    private val cartRepository: CartRepository,
    private val menuDataSource: MenuDataSource
) : BaseViewModel<OrderDetailUiState>() {
    
    // State for add items operation
    private val _addItemsResult = MutableStateFlow<AddItemsResult?>(null)
    val addItemsResult: StateFlow<AddItemsResult?> = _addItemsResult.asStateFlow()
    
    // State for status change operation
    private val _statusChangeResult = MutableStateFlow<StatusChangeResult?>(null)
    val statusChangeResult: StateFlow<StatusChangeResult?> = _statusChangeResult.asStateFlow()
    
    // State for status change confirmation dialog
    private val _statusChangeConfirmationState = MutableStateFlow<StatusChangeConfirmationState?>(null)
    val statusChangeConfirmationState: StateFlow<StatusChangeConfirmationState?> = _statusChangeConfirmationState.asStateFlow()
    
    // State for add more items dialog
    private val _addMoreItemsDialogState = MutableStateFlow<AddMoreItemsDialogState?>(null)
    val addMoreItemsDialogState: StateFlow<AddMoreItemsDialogState?> = _addMoreItemsDialogState.asStateFlow()
    
    // Temporary cart for dialog (items to be added)
    private val _dialogCartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val dialogCartItems: StateFlow<List<CartItem>> = _dialogCartItems.asStateFlow()
    
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
     * @param forceReload If true, reloads even if order is already loaded
     */
    fun loadOrder(orderId: String, forceReload: Boolean = false) {
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
        
        if (!forceReload && currentOrderId == orderId && currentState.order != null) {
            // Already loaded, no need to reload
            return
        }
        
        currentOrderId = orderId
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            try {
                Log.d("OrderDetailViewModel", "loadOrder: Loading order with ID='$orderId' (forceReload=$forceReload)")
                
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
    
    /**
     * Add more items to the existing order.
     * This will use the dialog cart items (items selected in the dialog).
     * 
     * @param order The existing order to add items to
     */
    fun addMoreItemsToOrder(order: Order) {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            try {
                // Get dialog cart items (items selected in the add more items dialog)
                val cartItems = _dialogCartItems.value
                
                if (cartItems.isEmpty()) {
                    _addItemsResult.value = AddItemsResult.Error("Please add at least one item")
                    updateState { copy(isLoading = false) }
                    return@launch
                }
                
                // Check if order can accept new items
                if (!orderBusinessLogic.canAddItemsToOrder(order)) {
                    _addItemsResult.value = AddItemsResult.Error("Cannot add items to order with status ${order.status}. Order must be CREATED or PREPARING.")
                    updateState { copy(isLoading = false) }
                    return@launch
                }
                
                Log.d("OrderDetailViewModel", "addMoreItemsToOrder: Adding ${cartItems.size} items to order ${order.id}")
                
                // Add items to existing order
                val result = orderBusinessLogic.addMoreToExistingOrder(order, cartItems)
                
                result.fold(
                    onSuccess = {
                        Log.i("OrderDetailViewModel", "addMoreItemsToOrder: Successfully added items to order")
                        _addItemsResult.value = AddItemsResult.Success
                        // Clear dialog cart after successful addition
                        _dialogCartItems.value = emptyList()
                        // Close dialog
                        closeAddMoreItemsDialog()
                        // Force reload order to show updated items
                        loadOrder(order.id, forceReload = true)
                    },
                    onFailure = { error ->
                        Log.e("OrderDetailViewModel", "addMoreItemsToOrder: Failed to add items", error)
                        _addItemsResult.value = AddItemsResult.Error(error.message ?: "Failed to add items to order")
                        updateState { copy(isLoading = false, errorMessage = error.message) }
                    }
                )
            } catch (e: Exception) {
                Log.e("OrderDetailViewModel", "addMoreItemsToOrder: Exception", e)
                _addItemsResult.value = AddItemsResult.Error(e.message ?: "Failed to add items to order")
                updateState { copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
    
    /**
     * Clear the add items result.
     */
    fun clearAddItemsResult() {
        _addItemsResult.value = null
    }
    
    /**
     * Change the order status.
     * 
     * @param orderId The ID of the order
     * @param newStatus The new status to set
     */
    fun changeOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            try {
                Log.d("OrderDetailViewModel", "changeOrderStatus: Changing order $orderId to status $newStatus")
                
                val result = orderRepository.updateOrderStatus(orderId, newStatus)
                
                result.fold(
                    onSuccess = {
                        Log.i("OrderDetailViewModel", "changeOrderStatus: Successfully changed order status")
                        _statusChangeResult.value = StatusChangeResult.Success
                        // Force reload order to show updated status
                        loadOrder(orderId, forceReload = true)
                    },
                    onFailure = { error ->
                        Log.e("OrderDetailViewModel", "changeOrderStatus: Failed to change status", error)
                        _statusChangeResult.value = StatusChangeResult.Error(error.message ?: "Failed to change order status")
                        updateState { copy(isLoading = false, errorMessage = error.message) }
                    }
                )
            } catch (e: Exception) {
                Log.e("OrderDetailViewModel", "changeOrderStatus: Exception", e)
                _statusChangeResult.value = StatusChangeResult.Error(e.message ?: "Failed to change order status")
                updateState { copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
    
    /**
     * Clear the status change result.
     */
    fun clearStatusChangeResult() {
        _statusChangeResult.value = null
    }
    
    /**
     * Show confirmation dialog for status change to PAID or CANCELLED.
     */
    fun showStatusChangeConfirmation(newStatus: OrderStatus) {
        val currentOrder = currentState.order
        if (currentOrder != null) {
            _statusChangeConfirmationState.value = StatusChangeConfirmationState(
                currentStatus = currentOrder.status,
                newStatus = newStatus
            )
        }
    }
    
    /**
     * Clear the status change confirmation state.
     */
    fun clearStatusChangeConfirmation() {
        _statusChangeConfirmationState.value = null
    }
    
    /**
     * Open the add more items dialog.
     * Loads menu items and prepares the dialog state.
     */
    fun openAddMoreItemsDialog(order: Order) {
        viewModelScope.launch {
            try {
                // Load menu items
                val menuItems = menuDataSource.getMenus()
                
                // Reset dialog cart
                _dialogCartItems.value = emptyList()
                
                // Create dialog state
                _addMoreItemsDialogState.value = AddMoreItemsDialogState(
                    order = order,
                    menuItems = menuItems,
                    filterQuery = "",
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("OrderDetailViewModel", "openAddMoreItemsDialog: Error loading menu items", e)
                _addMoreItemsDialogState.value = AddMoreItemsDialogState(
                    order = order,
                    menuItems = emptyList(),
                    filterQuery = "",
                    isLoading = false,
                    errorMessage = "Failed to load menu items: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Close the add more items dialog.
     */
    fun closeAddMoreItemsDialog() {
        _addMoreItemsDialogState.value = null
        _dialogCartItems.value = emptyList()
    }
    
    /**
     * Update the filter query for the add more items dialog.
     */
    fun updateDialogFilterQuery(query: String) {
        val currentState = _addMoreItemsDialogState.value
        if (currentState != null) {
            _addMoreItemsDialogState.value = currentState.copy(filterQuery = query)
        }
    }
    
    /**
     * Add an item to the dialog cart (temporary cart for adding to order).
     */
    fun addItemToDialogCart(menuItem: MenuItem) {
        val currentCart = _dialogCartItems.value.toMutableList()
        val existingItem = currentCart.find { it.menuItemId == menuItem.id }
        
        if (existingItem != null) {
            // Increment quantity
            val index = currentCart.indexOf(existingItem)
            currentCart[index] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            // Add new item
            currentCart.add(
                CartItem(
                    id = "dialog_${menuItem.id}_${System.currentTimeMillis()}",
                    menuItemId = menuItem.id,
                    name = menuItem.name,
                    price = menuItem.price.toInt(),
                    quantity = 1
                )
            )
        }
        
        _dialogCartItems.value = currentCart
    }
    
    /**
     * Update quantity of an item in the dialog cart.
     */
    fun updateDialogCartItemQuantity(cartItemId: String, quantity: Int) {
        if (quantity <= 0) {
            removeItemFromDialogCart(cartItemId)
            return
        }
        
        val currentCart = _dialogCartItems.value.toMutableList()
        val index = currentCart.indexOfFirst { it.id == cartItemId }
        if (index >= 0) {
            currentCart[index] = currentCart[index].copy(quantity = quantity)
            _dialogCartItems.value = currentCart
        }
    }
    
    /**
     * Remove an item from the dialog cart.
     */
    fun removeItemFromDialogCart(cartItemId: String) {
        val currentCart = _dialogCartItems.value.toMutableList()
        currentCart.removeAll { it.id == cartItemId }
        _dialogCartItems.value = currentCart
    }
    
    /**
     * Calculate updated billing for the order with new items.
     */
    fun calculateUpdatedBilling(order: Order, newItems: List<CartItem>): UpdatedBilling {
        // Merge existing order items with new items
        val existingItems = order.items.mapIndexed { index, orderItem ->
            CartItem(
                id = "existing_${orderItem.menuItemId}_$index",
                menuItemId = orderItem.menuItemId,
                name = orderItem.menuItemName,
                price = orderItem.price.toInt(),
                quantity = orderItem.quantity
            )
        }
        
        // Merge items (combine quantities for same menu item)
        val mergedItems = mutableListOf<CartItem>()
        val itemMap = existingItems.associateBy { it.menuItemId }.toMutableMap()
        
        // Add new items
        newItems.forEach { newItem ->
            val existing = itemMap[newItem.menuItemId]
            if (existing != null) {
                itemMap[newItem.menuItemId] = existing.copy(quantity = existing.quantity + newItem.quantity)
            } else {
                itemMap[newItem.menuItemId] = newItem
            }
        }
        
        mergedItems.addAll(itemMap.values)
        
        // Calculate totals
        val subtotal = mergedItems.sumOf { it.price.toDouble() * it.quantity }
        val serviceFee = 10000.0 // Fixed service fee
        val total = subtotal + serviceFee
        
        return UpdatedBilling(
            subtotal = subtotal,
            serviceFee = serviceFee,
            total = total
        )
    }
    
    /**
     * Confirm adding items to the order.
     */
    fun confirmAddMoreItems(order: Order) {
        val itemsToAdd = _dialogCartItems.value
        if (itemsToAdd.isEmpty()) {
            _addItemsResult.value = AddItemsResult.Error("Please add at least one item")
            return
        }
        
        // Use the existing addMoreItemsToOrder function
        addMoreItemsToOrder(order)
    }
}

/**
 * State for the add more items dialog.
 */
data class AddMoreItemsDialogState(
    val order: Order,
    val menuItems: List<MenuItem>,
    val filterQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Filtered menu items based on filterQuery.
     * Filters by name (case-insensitive, accent-insensitive).
     */
    val filteredMenuItems: List<MenuItem>
        get() = if (filterQuery.isBlank()) {
            menuItems
        } else {
            val query = removeAccents(filterQuery.trim().lowercase())
            menuItems.filter { 
                val menuName = removeAccents(it.name.lowercase())
                menuName.contains(query)
            }
        }
}

/**
 * Remove Vietnamese accents/diacritics from text.
 * Converts "Trà đào" to "tra dao" for easier searching.
 * 
 * @param text The text to remove accents from
 * @return Text without accents
 */
private fun removeAccents(text: String): String {
    return try {
        // Normalize to NFD (decomposed form) to separate base characters from diacritics
        val normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        // Remove combining diacritical marks (Unicode category Mn)
        normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    } catch (e: Exception) {
        // Fallback to original text if normalization fails
        text
    }
}

/**
 * Updated billing calculation.
 */
data class UpdatedBilling(
    val subtotal: Double,
    val serviceFee: Double,
    val total: Double
)

/**
 * State for status change confirmation dialog.
 */
data class StatusChangeConfirmationState(
    val currentStatus: OrderStatus,
    val newStatus: OrderStatus
)

/**
 * Result of adding items to order operation.
 */
sealed class AddItemsResult {
    object Success : AddItemsResult()
    data class Error(val message: String) : AddItemsResult()
}

/**
 * Result of changing order status operation.
 */
sealed class StatusChangeResult {
    object Success : StatusChangeResult()
    data class Error(val message: String) : StatusChangeResult()
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

