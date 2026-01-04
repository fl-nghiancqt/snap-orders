package com.example.snaporder.feature.admin

import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.firestore.MenuRepository
import com.example.snaporder.core.model.MenuItem
import com.example.snaporder.core.viewmodel.BaseViewModel
import com.example.snaporder.data.firebase.FirestoreMenuSeeder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

/**
 * Menu Management ViewModel for admin screen.
 * 
 * Features:
 * - Seed menu data to Firestore (with dummy/test data)
 * - Load menus from Firestore (real-time updates via Flow)
 * - Update menu availability
 * 
 * This ViewModel connects directly to Firebase Firestore for all operations.
 */
@HiltViewModel
class MenuManagementViewModel @Inject constructor(
    private val menuRepository: MenuRepository
) : BaseViewModel<MenuManagementUiState>() {
    
    override fun createInitialState(): MenuManagementUiState {
        return MenuManagementUiState(
            isLoading = true, // Start with loading state
            isSeeding = false,
            menus = emptyList(),
            errorMessage = null,
            seedSuccess = false
        )
    }
    
    private val _seedResult = MutableStateFlow<String?>(null)
    val seedResult: StateFlow<String?> = _seedResult.asStateFlow()
    
    init {
        // Start listening to real-time updates from Firestore
        observeMenus()
    }
    
    /**
     * Observe menus from Firestore in real-time.
     * Uses Flow to automatically update UI when data changes.
     */
    private fun observeMenus() {
        menuRepository.getAllMenuItems()
            .onEach { menus ->
                updateState {
                    copy(
                        isLoading = false,
                        menus = menus,
                        errorMessage = null
                    )
                }
            }
            .catch { e ->
                updateState {
                    copy(
                        isLoading = false,
                        menus = emptyList(),
                        errorMessage = "Failed to load menus: ${e.message}"
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Load all menus from Firestore (one-time fetch).
     * This is used as a fallback or for manual refresh.
     */
    fun loadMenus() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            try {
                // Use getAllMenus() suspend function for one-time fetch
                val menus = menuRepository.getAllMenus()
                updateState {
                    copy(
                        isLoading = false,
                        menus = menus,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        menus = emptyList(),
                        errorMessage = "Failed to load menus: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Seed menu data to Firestore.
     * Calls FirestoreMenuSeeder to batch write ~60 menu items.
     * 
     * This adds dummy/test data for development and testing.
     * Can be called multiple times (will overwrite existing items with same ID).
     */
    fun seedMenus() {
        viewModelScope.launch {
            updateState { copy(isSeeding = true, errorMessage = null, seedSuccess = false) }
            _seedResult.value = null
            
            try {
                // Call the seeder (now a suspend function)
                val result = FirestoreMenuSeeder.seedMenusToFirestore()
                
                result.fold(
                    onSuccess = { count ->
                        updateState {
                            copy(
                                isSeeding = false,
                                seedSuccess = true
                            )
                        }
                        _seedResult.value = "✓ Successfully seeded $count menu items to Firestore!"
                        
                        // Menus will automatically update via Flow observer
                        // No need to manually reload
                    },
                    onFailure = { e ->
                        updateState {
                            copy(
                                isSeeding = false,
                                errorMessage = "Seeding failed: ${e.message}",
                                seedSuccess = false
                            )
                        }
                        _seedResult.value = "✗ Seeding failed: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                updateState {
                    copy(
                        isSeeding = false,
                        errorMessage = "Seeding failed: ${e.message}",
                        seedSuccess = false
                    )
                }
                _seedResult.value = "✗ Seeding failed: ${e.message}"
            }
        }
    }
    
    /**
     * Toggle menu item availability.
     * Updates the menu item in Firestore and UI will update automatically via Flow.
     * 
     * @param menuId The ID of the menu item to update
     * @param available New availability status
     */
    fun updateMenuAvailability(menuId: String, available: Boolean) {
        viewModelScope.launch {
            try {
                // Get current menu
                val menu = currentState.menus.find { it.id == menuId }
                if (menu != null) {
                    // Update availability
                    val updatedMenu = menu.copy(available = available)
                    val result = menuRepository.updateMenu(updatedMenu)
                    
                    result.fold(
                        onSuccess = {
                            // Success - UI will update automatically via Flow observer
                        },
                        onFailure = { e ->
                            updateState {
                                copy(errorMessage = "Failed to update menu: ${e.message}")
                            }
                        }
                    )
                } else {
                    updateState {
                        copy(errorMessage = "Menu item not found: $menuId")
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(errorMessage = "Failed to update menu: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Update menu item price.
     * Updates the menu item price in Firestore and UI will update automatically via Flow.
     * 
     * @param menuId The ID of the menu item to update
     * @param newPrice New price value
     */
    fun updateMenuPrice(menuId: String, newPrice: Double) {
        viewModelScope.launch {
            try {
                // Get current menu
                val menu = currentState.menus.find { it.id == menuId }
                if (menu != null) {
                    // Validate price
                    if (newPrice < 0) {
                        updateState {
                            copy(errorMessage = "Price cannot be negative")
                        }
                        return@launch
                    }
                    
                    // Update price
                    val updatedMenu = menu.copy(price = newPrice)
                    val result = menuRepository.updateMenu(updatedMenu)
                    
                    result.fold(
                        onSuccess = {
                            // Success - UI will update automatically via Flow observer
                        },
                        onFailure = { e ->
                            updateState {
                                copy(errorMessage = "Failed to update price: ${e.message}")
                            }
                        }
                    )
                } else {
                    updateState {
                        copy(errorMessage = "Menu item not found: $menuId")
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(errorMessage = "Failed to update price: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Update menu item name.
     * Updates the menu item name in Firestore and UI will update automatically via Flow.
     * 
     * @param menuId The ID of the menu item to update
     * @param newName New name value
     */
    fun updateMenuName(menuId: String, newName: String) {
        viewModelScope.launch {
            try {
                // Get current menu
                val menu = currentState.menus.find { it.id == menuId }
                if (menu != null) {
                    // Validate name
                    val trimmedName = newName.trim()
                    if (trimmedName.isBlank()) {
                        updateState {
                            copy(errorMessage = "Name cannot be empty")
                        }
                        return@launch
                    }
                    
                    // Update name
                    val updatedMenu = menu.copy(name = trimmedName)
                    val result = menuRepository.updateMenu(updatedMenu)
                    
                    result.fold(
                        onSuccess = {
                            // Success - UI will update automatically via Flow observer
                        },
                        onFailure = { e ->
                            updateState {
                                copy(errorMessage = "Failed to update name: ${e.message}")
                            }
                        }
                    )
                } else {
                    updateState {
                        copy(errorMessage = "Menu item not found: $menuId")
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(errorMessage = "Failed to update name: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Refresh menu list manually.
     * Triggers a one-time fetch from Firestore.
     */
    fun refresh() {
        loadMenus()
    }
    
    /**
     * Update filter query for menu name search.
     * 
     * @param query The search query string
     */
    fun updateFilterQuery(query: String) {
        updateState { copy(filterQuery = query) }
    }
    
    /**
     * Create a new menu item.
     * Adds the menu item to Firestore and UI will update automatically via Flow.
     * 
     * @param name Menu item name (required)
     * @param price Menu item price (required, must be >= 0)
     * @param imageUrl Optional image URL
     * @param available Availability status (default: true)
     * @return Result containing success/failure
     */
    fun createMenuItem(
        name: String,
        price: Double,
        imageUrl: String = "",
        available: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                // Validate name
                val trimmedName = name.trim()
                if (trimmedName.isBlank()) {
                    updateState {
                        copy(errorMessage = "Name cannot be empty")
                    }
                    return@launch
                }
                
                // Validate price
                if (price < 0) {
                    updateState {
                        copy(errorMessage = "Price cannot be negative")
                    }
                    return@launch
                }
                
                // Create new menu item (ID will be auto-generated by Firestore)
                val newMenuItem = MenuItem(
                    id = "", // Empty ID will trigger auto-generation
                    name = trimmedName,
                    price = price,
                    available = available,
                    imageUrl = imageUrl.trim()
                )
                
                val result = menuRepository.createMenu(newMenuItem)
                
                result.fold(
                    onSuccess = { menuId ->
                        // Success - UI will update automatically via Flow observer
                        updateState { copy(errorMessage = null) }
                    },
                    onFailure = { e ->
                        updateState {
                            copy(errorMessage = "Failed to create menu item: ${e.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                updateState {
                    copy(errorMessage = "Failed to create menu item: ${e.message}")
                }
            }
        }
    }
}

/**
 * Menu Management UI State.
 */
data class MenuManagementUiState(
    val isLoading: Boolean = false,
    val isSeeding: Boolean = false,
    val menus: List<MenuItem> = emptyList(),
    val errorMessage: String? = null,
    val seedSuccess: Boolean = false,
    val filterQuery: String = ""
) {
    /**
     * Filtered menus based on filterQuery.
     * Filters by name (case-insensitive, accent-insensitive).
     */
    val filteredMenus: List<MenuItem>
        get() = if (filterQuery.isBlank()) {
            menus
        } else {
            val query = removeAccents(filterQuery.trim().lowercase())
            menus.filter { 
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
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        // Remove combining diacritical marks (Unicode category Mn)
        normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    } catch (e: Exception) {
        // Fallback to original text if normalization fails
        text
    }
}

