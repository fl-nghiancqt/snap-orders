package com.example.snaporder.feature.admin

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.snaporder.core.firestore.MenuRepository
import com.example.snaporder.core.firestore.OrderRepository
import com.example.snaporder.core.model.MenuItem
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.model.OrderStatus
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
 * - Uses MenuRepository to get menu item names
 * - Calculates today's statistics (orders count, revenue)
 * - Supports custom date range reports
 * - Manages UI state via StateFlow
 */
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val menuRepository: MenuRepository
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
    
    /**
     * Load custom date range report.
     */
    fun loadCustomDateRangeReport(startDate: Calendar, endDate: Calendar) {
        viewModelScope.launch {
            updateState { copy(isCustomReportLoading = true, customReportError = null) }
            
            try {
                // Set start date to beginning of day
                val startCal = startDate.clone() as Calendar
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)
                val startTimestamp = Timestamp(startCal.timeInMillis / 1000, 0)
                
                // Set end date to end of day
                val endCal = endDate.clone() as Calendar
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                val endTimestamp = Timestamp(endCal.timeInMillis / 1000, 0)
                
                Log.d("AdminViewModel", "loadCustomDateRangeReport: Loading orders from ${startTimestamp.seconds} to ${endTimestamp.seconds}")
                
                // Get all orders and filter by date range
                orderRepository.getAllOrders()
                    .onEach { allOrders ->
                        // Filter orders by date range
                        val dateRangeOrders = allOrders.filter { order ->
                            val orderTime = order.createdAt
                            if (orderTime != null) {
                                val orderSeconds = orderTime.seconds
                                orderSeconds >= startTimestamp.seconds && orderSeconds <= endTimestamp.seconds
                            } else {
                                false
                            }
                        }
                        
                        // Calculate order statistics by status
                        val statusStats = OrderStatus.values().associateWith { status ->
                            dateRangeOrders.count { it.status == status }
                        }
                        
                        // Calculate total revenue (only PAID orders)
                        val paidOrders = dateRangeOrders.filter { it.status == OrderStatus.PAID }
                        val totalRevenue = paidOrders.sumOf { it.totalPrice }
                        val totalOrders = dateRangeOrders.size
                        
                        // Calculate popular menu items
                        val menuItemCounts = mutableMapOf<String, Int>()
                        dateRangeOrders.forEach { order ->
                            order.items.forEach { item ->
                                menuItemCounts[item.menuItemId] = 
                                    menuItemCounts.getOrDefault(item.menuItemId, 0) + item.quantity
                            }
                        }
                        
                        // Get menu item names
                        val menuItems = menuRepository.getAllMenus()
                        val popularItems = menuItemCounts.entries
                            .sortedByDescending { it.value }
                            .take(10)
                            .mapNotNull { (menuItemId, count) ->
                                val menuItem = menuItems.find { it.id == menuItemId }
                                if (menuItem != null) {
                                    PopularMenuItem(menuItem.name, count)
                                } else {
                                    null
                                }
                            }
                        
                        // Calculate daily revenue for trends
                        val dailyRevenueMap = mutableMapOf<String, Double>()
                        paidOrders.forEach { order ->
                            val orderTime = order.createdAt
                            if (orderTime != null) {
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = orderTime.seconds * 1000L
                                val dateKey = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
                                dailyRevenueMap[dateKey] = dailyRevenueMap.getOrDefault(dateKey, 0.0) + order.totalPrice
                            }
                        }
                        val dailyRevenue = dailyRevenueMap.entries
                            .sortedBy { it.key }
                            .map { DailyRevenue(it.key, it.value) }
                        
                        Log.d("AdminViewModel", "loadCustomDateRangeReport: Found ${dateRangeOrders.size} orders, Revenue=$totalRevenue")
                        
                        updateState {
                            copy(
                                isCustomReportLoading = false,
                                customDateRangeOrders = totalOrders,
                                customDateRangeRevenue = totalRevenue,
                                orderStatisticsByStatus = statusStats,
                                popularMenuItems = popularItems,
                                dailyRevenueTrends = dailyRevenue,
                                customReportError = null
                            )
                        }
                    }
                    .catch { error ->
                        Log.e("AdminViewModel", "loadCustomDateRangeReport: Error loading orders", error)
                        updateState {
                            copy(
                                isCustomReportLoading = false,
                                customReportError = "Failed to load report: ${error.message}"
                            )
                        }
                    }
                    .launchIn(viewModelScope)
            } catch (e: Exception) {
                Log.e("AdminViewModel", "loadCustomDateRangeReport: Exception", e)
                updateState {
                    copy(
                        isCustomReportLoading = false,
                        customReportError = "Failed to load report: ${e.message}"
                    )
                }
            }
        }
    }
}

/**
 * Data class for popular menu items.
 */
data class PopularMenuItem(
    val name: String,
    val orderCount: Int
)

/**
 * Data class for daily revenue.
 */
data class DailyRevenue(
    val date: String,
    val revenue: Double
)

/**
 * Admin UI State.
 */
data class AdminUiState(
    val isLoading: Boolean = false,
    val todayOrdersCount: Int = 0,
    val todayRevenue: Double = 0.0,
    val monthOrdersCount: Int = 0,
    val monthRevenue: Double = 0.0,
    val errorMessage: String? = null,
    // Custom date range report
    val isCustomReportLoading: Boolean = false,
    val customDateRangeOrders: Int = 0,
    val customDateRangeRevenue: Double = 0.0,
    val orderStatisticsByStatus: Map<OrderStatus, Int> = emptyMap(),
    val popularMenuItems: List<PopularMenuItem> = emptyList(),
    val dailyRevenueTrends: List<DailyRevenue> = emptyList(),
    val customReportError: String? = null
)

