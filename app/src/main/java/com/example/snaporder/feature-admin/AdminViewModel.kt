package com.example.snaporder.feature.admin

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.firestore.OrderRepository
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.viewmodel.BaseViewModel
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Admin ViewModel for managing admin dashboard state.
 * 
 * ARCHITECTURE:
 * - Uses OrderRepository to fetch orders
 * - Calculates today's statistics (orders count, revenue)
 * - Manages UI state via StateFlow
 */
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : BaseViewModel<AdminUiState>() {
    
    override fun createInitialState(): AdminUiState {
        return AdminUiState(
            isLoading = false,
            todayOrdersCount = 0,
            todayRevenue = 0.0,
            monthOrdersCount = 0,
            monthRevenue = 0.0,
            errorMessage = null
        )
    }
    
    init {
        loadTodayStats()
    }
    
    /**
     * Load today's statistics (orders count and revenue).
     */
    fun loadTodayStats() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            
            try {
                // Get start and end of today
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = Timestamp(calendar.timeInMillis / 1000, 0)
                
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfDay = Timestamp(calendar.timeInMillis / 1000, 0)
                
                // Get start and end of current month
                val monthCalendar = Calendar.getInstance()
                monthCalendar.set(Calendar.DAY_OF_MONTH, 1)
                monthCalendar.set(Calendar.HOUR_OF_DAY, 0)
                monthCalendar.set(Calendar.MINUTE, 0)
                monthCalendar.set(Calendar.SECOND, 0)
                monthCalendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = Timestamp(monthCalendar.timeInMillis / 1000, 0)
                
                monthCalendar.add(Calendar.MONTH, 1)
                monthCalendar.add(Calendar.MILLISECOND, -1)
                val endOfMonth = Timestamp(monthCalendar.timeInMillis / 1000, 0)
                
                Log.d("AdminViewModel", "loadTodayStats: Loading orders from ${startOfDay.seconds} to ${endOfDay.seconds} (today)")
                Log.d("AdminViewModel", "loadTodayStats: Loading orders from ${startOfMonth.seconds} to ${endOfMonth.seconds} (month)")
                
                // Collect all orders and filter for today and month
                orderRepository.getAllOrders()
                    .onEach { allOrders ->
                        // Filter today's orders
                        val todayOrders = allOrders.filter { order ->
                            val orderTime = order.createdAt
                            if (orderTime != null) {
                                val orderSeconds = orderTime.seconds
                                orderSeconds >= startOfDay.seconds && orderSeconds <= endOfDay.seconds
                            } else {
                                false
                            }
                        }
                        
                        // Filter current month's orders
                        val monthOrders = allOrders.filter { order ->
                            val orderTime = order.createdAt
                            if (orderTime != null) {
                                val orderSeconds = orderTime.seconds
                                orderSeconds >= startOfMonth.seconds && orderSeconds <= endOfMonth.seconds
                            } else {
                                false
                            }
                        }
                        
                        // Calculate today's stats - only count PAID orders for revenue
                        val paidTodayOrders = todayOrders.filter { it.status == com.example.snaporder.core.model.OrderStatus.PAID }
                        val todayOrdersCount = paidTodayOrders.size
                        val todayRevenue = paidTodayOrders.sumOf { it.totalPrice }
                        
                        // Calculate month's stats - only count PAID orders for revenue
                        val paidMonthOrders = monthOrders.filter { it.status == com.example.snaporder.core.model.OrderStatus.PAID }
                        val monthOrdersCount = paidMonthOrders.size
                        val monthRevenue = paidMonthOrders.sumOf { it.totalPrice }
                        
                        Log.d("AdminViewModel", "loadTodayStats: Today orders=${todayOrders.size}, Paid orders=${todayOrdersCount}, Revenue=$todayRevenue")
                        Log.d("AdminViewModel", "loadTodayStats: Month orders=${monthOrders.size}, Paid orders=${monthOrdersCount}, Revenue=$monthRevenue")
                        
                        updateState {
                            copy(
                                isLoading = false,
                                todayOrdersCount = todayOrdersCount,
                                todayRevenue = todayRevenue,
                                monthOrdersCount = monthOrdersCount,
                                monthRevenue = monthRevenue,
                                errorMessage = null
                            )
                        }
                    }
                    .catch { error ->
                        Log.e("AdminViewModel", "loadTodayStats: Error loading orders", error)
                        updateState {
                            copy(
                                isLoading = false,
                                errorMessage = "Failed to load statistics: ${error.message}"
                            )
                        }
                    }
                    .launchIn(viewModelScope)
            } catch (e: Exception) {
                Log.e("AdminViewModel", "loadTodayStats: Exception", e)
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = "Failed to load statistics: ${e.message}"
                    )
                }
            }
        }
    }
}

/**
 * Admin UI State.
 */
data class AdminUiState(
    val isLoading: Boolean = false,
    val todayOrdersCount: Int = 0,
    val todayRevenue: Double = 0.0,
    val monthOrdersCount: Int = 0,
    val monthRevenue: Double = 0.0,
    val errorMessage: String? = null
)

