package com.example.snaporder.feature.history

import com.example.snaporder.core.viewmodel.BaseViewModel

/**
 * History ViewModel placeholder.
 * TODO: Implement order history loading using OrderRepository.getUserOrders().
 */
class HistoryViewModel : BaseViewModel<HistoryUiState>() {
    override fun createInitialState(): HistoryUiState {
        return HistoryUiState()
    }
}

/**
 * History UI State.
 */
data class HistoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

