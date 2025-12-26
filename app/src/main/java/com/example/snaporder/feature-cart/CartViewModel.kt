package com.example.snaporder.feature.cart

import com.example.snaporder.core.viewmodel.BaseViewModel

/**
 * Cart ViewModel placeholder.
 * TODO: Implement cart management and order placement logic.
 * Must use OrderBusinessLogic for table validation and order creation.
 */
class CartViewModel : BaseViewModel<CartUiState>() {
    override fun createInitialState(): CartUiState {
        return CartUiState()
    }
}

/**
 * Cart UI State.
 */
data class CartUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTableNumber: Int? = null
)

