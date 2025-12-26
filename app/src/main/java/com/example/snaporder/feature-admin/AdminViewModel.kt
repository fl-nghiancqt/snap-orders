package com.example.snaporder.feature.admin

import com.example.snaporder.core.viewmodel.BaseViewModel

/**
 * Admin ViewModel placeholder.
 * TODO: Implement admin dashboard logic.
 * Should show all orders and allow status updates.
 */
class AdminViewModel : BaseViewModel<AdminUiState>() {
    override fun createInitialState(): AdminUiState {
        return AdminUiState()
    }
}

/**
 * Admin UI State.
 */
data class AdminUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

