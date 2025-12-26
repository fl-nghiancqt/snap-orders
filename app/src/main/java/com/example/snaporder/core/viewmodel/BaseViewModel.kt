package com.example.snaporder.core.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Base ViewModel with StateFlow support.
 * All feature ViewModels should extend this class.
 */
abstract class BaseViewModel<UiState> : ViewModel() {
    
    /**
     * Private mutable state flow for internal updates.
     */
    private val _uiState = MutableStateFlow(createInitialState())
    
    /**
     * Public immutable state flow for UI observation.
     */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    /**
     * Get current UI state.
     */
    protected val currentState: UiState
        get() = _uiState.value
    
    /**
     * Update UI state.
     */
    protected fun updateState(update: UiState.() -> UiState) {
        _uiState.value = _uiState.value.update()
    }
    
    /**
     * Set UI state directly.
     */
    protected fun setState(newState: UiState) {
        _uiState.value = newState
    }
    
    /**
     * Create initial state. Must be implemented by subclasses.
     */
    protected abstract fun createInitialState(): UiState
}

