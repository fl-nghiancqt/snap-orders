package com.example.snaporder.feature.order

import com.example.snaporder.core.viewmodel.BaseViewModel

/**
 * Order ViewModel placeholder.
 * TODO: Implement order tracking and status updates.
 */
class OrderViewModel : BaseViewModel<OrderUiState>() {
    override fun createInitialState(): OrderUiState {
        return OrderUiState()
    }
}

/**
 * Order UI State.
 */
data class OrderUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

