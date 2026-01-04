package com.example.snaporder.feature.admin

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snaporder.core.model.OrderStatus
import com.example.snaporder.ui.theme.SnapOrdersColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Admin Report Screen - Detailed report view with custom date range.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportScreen(
    onBackClick: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Date picker state
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val calendar = Calendar.getInstance()
    var startDate by remember { mutableStateOf(calendar.clone() as Calendar) }
    var endDate by remember { mutableStateOf(calendar.clone() as Calendar) }
    
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Report",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = SnapOrdersColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SnapOrdersColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SnapOrdersColors.Background
                )
            )
        },
        containerColor = SnapOrdersColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today's Report
            item {
                Text(
                    text = "Today's Report",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.TextPrimary
                )
            }
            
            item {
                ReportCard(
                    ordersCount = uiState.todayOrdersCount,
                    revenue = uiState.todayRevenue
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Current Month Report
            item {
                Text(
                    text = "Current Month Report",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.TextPrimary
                )
            }
            
            item {
                ReportCard(
                    ordersCount = uiState.monthOrdersCount,
                    revenue = uiState.monthRevenue
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Custom Date Range Report
            item {
                Text(
                    text = "Custom Date Range Report",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.TextPrimary
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SnapOrdersColors.Surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date Range Picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start Date
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showStartDatePicker = true }
                            ) {
                                OutlinedTextField(
                                    value = dateFormat.format(startDate.time),
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    label = { Text("Start Date") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.DateRange,
                                            contentDescription = "Calendar"
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SnapOrdersColors.Primary,
                                        unfocusedBorderColor = SnapOrdersColors.Outline
                                    )
                                )
                            }
                            
                            // End Date
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showEndDatePicker = true }
                            ) {
                                OutlinedTextField(
                                    value = dateFormat.format(endDate.time),
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    label = { Text("End Date") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.DateRange,
                                            contentDescription = "Calendar"
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SnapOrdersColors.Primary,
                                        unfocusedBorderColor = SnapOrdersColors.Outline
                                    )
                                )
                            }
                        }
                        
                        // Generate Report Button
                        Button(
                            onClick = {
                                viewModel.loadCustomDateRangeReport(startDate, endDate)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SnapOrdersColors.Primary
                            ),
                            enabled = !uiState.isCustomReportLoading
                        ) {
                            if (uiState.isCustomReportLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "Generate Report",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
            
            // Custom Report Results
            if (!uiState.isCustomReportLoading && uiState.customDateRangeOrders > 0) {
                item {
                    ReportCard(
                        ordersCount = uiState.customDateRangeOrders,
                        revenue = uiState.customDateRangeRevenue
                    )
                }
            }
            
            // Order Statistics by Status
            if (uiState.orderStatisticsByStatus.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                item {
                    Text(
                        text = "Order Statistics by Status",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = SnapOrdersColors.TextPrimary
                    )
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SnapOrdersColors.Surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OrderStatus.values().forEach { status ->
                                val count = uiState.orderStatisticsByStatus[status] ?: 0
                                if (count > 0 || uiState.orderStatisticsByStatus.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = getStatusLabel(status),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = SnapOrdersColors.TextPrimary
                                        )
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = getStatusColor(status)
                                        )
                                    }
                                    if (status != OrderStatus.values().last()) {
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Revenue Charts and Trends
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                Text(
                    text = "Revenue Trends",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.TextPrimary
                )
            }
            
            item {
                RevenueChartSection(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
            
            // Popular Menu Items
            if (uiState.popularMenuItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                item {
                    Text(
                        text = "Popular Menu Items",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = SnapOrdersColors.TextPrimary
                    )
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SnapOrdersColors.Surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            uiState.popularMenuItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}.",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = SnapOrdersColors.TextSecondary
                                        )
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = SnapOrdersColors.TextPrimary
                                        )
                                    }
                                    Text(
                                        text = "${item.orderCount} orders",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = SnapOrdersColors.Primary
                                    )
                                }
                                if (index < uiState.popularMenuItems.size - 1) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
            
            // Error message
            if (uiState.customReportError != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = uiState.customReportError ?: "Error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Date Pickers
    if (showStartDatePicker) {
        val threeMonthsAgo = Calendar.getInstance().apply {
            add(Calendar.MONTH, -3)
        }
        val maxStartDate = Calendar.getInstance() // Today
        
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                startDate.set(year, month, dayOfMonth)
                showStartDatePicker = false
            },
            startDate.get(Calendar.YEAR),
            startDate.get(Calendar.MONTH),
            startDate.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = threeMonthsAgo.timeInMillis
            datePicker.maxDate = maxStartDate.timeInMillis
        }.show()
    }
    
    if (showEndDatePicker) {
        val minEndDate = startDate.clone() as Calendar // Cannot be before start date
        val maxEndDate = Calendar.getInstance() // Today
        
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                endDate.set(year, month, dayOfMonth)
                showEndDatePicker = false
            },
            endDate.get(Calendar.YEAR),
            endDate.get(Calendar.MONTH),
            endDate.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = minEndDate.timeInMillis
            datePicker.maxDate = maxEndDate.timeInMillis
        }.show()
    }
}

@Composable
private fun ReportCard(ordersCount: Int, revenue: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SnapOrdersColors.Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Orders",
                    style = MaterialTheme.typography.titleMedium,
                    color = SnapOrdersColors.TextPrimary
                )
                Text(
                    text = ordersCount.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.Primary
                )
            }
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Revenue",
                    style = MaterialTheme.typography.titleMedium,
                    color = SnapOrdersColors.TextPrimary
                )
                Text(
                    text = formatPrice(revenue.toInt()),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.Primary
                )
            }
        }
    }
}

@Composable
private fun RevenueChartSection(
    uiState: AdminUiState,
    viewModel: AdminViewModel
) {
    var selectedPeriod by remember { mutableStateOf(RevenuePeriod.WEEK) }
    val scrollState = rememberLazyListState()
    
    // Load data when period changes
    LaunchedEffect(selectedPeriod) {
        viewModel.loadRevenueChartData(selectedPeriod)
    }
    
    // Scroll to end when revenue data changes
    val revenueData = when (selectedPeriod) {
        RevenuePeriod.WEEK -> uiState.weeklyRevenue
        RevenuePeriod.MONTH -> uiState.monthlyRevenue
        RevenuePeriod.YEAR -> uiState.yearlyRevenue
    }
    
    LaunchedEffect(revenueData.size) {
        if (revenueData.isNotEmpty()) {
            scrollState.animateScrollToItem(revenueData.size - 1)
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SnapOrdersColors.Surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tabs
            var selectedTabIndex by remember { mutableStateOf(0) }
            TabRow(
                selectedTabIndex = selectedTabIndex
            ) {
                RevenuePeriod.values().forEachIndexed { index, period ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            selectedPeriod = period
                        },
                        text = {
                            Text(
                                text = when (period) {
                                    RevenuePeriod.WEEK -> "Week"
                                    RevenuePeriod.MONTH -> "Month"
                                    RevenuePeriod.YEAR -> "Year"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    )
                }
            }
            
            // Chart Content
            if (uiState.isRevenueChartLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SnapOrdersColors.Primary)
                }
            } else if (uiState.revenueChartError != null) {
                Text(
                    text = uiState.revenueChartError ?: "Error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                val revenueData = when (selectedPeriod) {
                    RevenuePeriod.WEEK -> uiState.weeklyRevenue
                    RevenuePeriod.MONTH -> uiState.monthlyRevenue
                    RevenuePeriod.YEAR -> uiState.yearlyRevenue
                }
                
                if (revenueData.isEmpty()) {
                    Text(
                        text = "No revenue data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnapOrdersColors.TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    val maxRevenue = revenueData.maxOfOrNull { it.revenue } ?: 1.0
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Chart area with vertical bars (scrollable)
                        LazyRow(
                            state = scrollState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(revenueData.size) { index ->
                                val revenueItem = revenueData[index]
                                VerticalBarChartItem(
                                    value = revenueItem.revenue,
                                    maxValue = maxRevenue,
                                    label = formatRevenueDate(revenueItem.date, selectedPeriod),
                                    revenue = formatRevenueShort(revenueItem.revenue),
                                    modifier = Modifier
                                        .width(60.dp)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalBarChartItem(
    value: Double,
    maxValue: Double,
    label: String,
    revenue: String,
    modifier: Modifier = Modifier
) {
    // Animate bar height from 0 to target value
    val animatedHeight = remember { Animatable(0f) }
    
    LaunchedEffect(value) {
        val targetHeight = (value / maxValue).toFloat().coerceIn(0f, 1f)
        animatedHeight.animateTo(
            targetValue = targetHeight,
            animationSpec = tween(durationMillis = 800, delayMillis = 0)
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Bar chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            VerticalBarChart(
                value = value,
                maxValue = maxValue,
                animatedProgress = animatedHeight.value,
                modifier = Modifier.fillMaxSize()
            )
            // Revenue value on top of bar
            if (value > 0) {
                Text(
                    text = revenue,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.TextPrimary,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(bottom = 4.dp)
                )
            }
        }
        // Date label
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SnapOrdersColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VerticalBarChart(
    value: Double,
    maxValue: Double,
    animatedProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val maxBarHeight = ((value / maxValue) * height.toDouble()).toFloat().coerceAtLeast(0f).coerceAtMost(height)
        val barHeight = maxBarHeight * animatedProgress.coerceIn(0f, 1f)
        val barWidth = width * 0.7f // Use 70% of available width for the bar
        
        // Draw bar from bottom
        val barLeft = (width - barWidth) / 2f
        val barTop = height - barHeight
        
        drawRect(
            color = SnapOrdersColors.Primary,
            topLeft = Offset(barLeft, barTop),
            size = Size(barWidth, barHeight)
        )
    }
}

private fun formatRevenueDate(dateKey: String, period: RevenuePeriod): String {
    return try {
        when (period) {
            RevenuePeriod.WEEK, RevenuePeriod.MONTH -> {
                // Format: YYYY-MM-DD
                val parts = dateKey.split("-")
                if (parts.size == 3) {
                    val day = parts[2].toInt()
                    val month = parts[1].toInt()
                    "$day/${month}"
                } else {
                    dateKey
                }
            }
            RevenuePeriod.YEAR -> {
                // Format: YYYY-MM
                val parts = dateKey.split("-")
                if (parts.size == 2) {
                    val month = parts[1].toInt()
                    val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    if (month in 1..12) {
                        monthNames[month - 1]
                    } else {
                        dateKey
                    }
                } else {
                    dateKey
                }
            }
        }
    } catch (e: Exception) {
        dateKey
    }
}

private fun getStatusLabel(status: OrderStatus): String {
    return when (status) {
        OrderStatus.CREATED -> "Created"
        OrderStatus.PREPARING -> "Preparing"
        OrderStatus.PAID -> "Paid"
        OrderStatus.CANCELLED -> "Cancelled"
    }
}

private fun getStatusColor(status: OrderStatus): Color {
    return when (status) {
        OrderStatus.CREATED -> Color(0xFF2196F3) // Blue
        OrderStatus.PREPARING -> Color(0xFFFF9800) // Orange
        OrderStatus.PAID -> Color(0xFF4CAF50) // Green
        OrderStatus.CANCELLED -> Color(0xFFF44336) // Red
    }
}

private fun formatDate(dateString: String): String {
    return try {
        // Format: YYYY-MM-DD
        val parts = dateString.split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

/**
 * Format price in VND format.
 * Example: 30000 -> "30,000 đ"
 */
private fun formatPrice(price: Int): String {
    return "${price.toString().reversed().chunked(3).joinToString(",").reversed()} đ"
}

/**
 * Format revenue with abbreviated notation for chart display.
 * Example: 1000 -> "1K", 1500000 -> "1.5M", 500 -> "500"
 */
private fun formatRevenueShort(revenue: Double): String {
    val value = revenue.toLong()
    return when {
        value >= 1_000_000_000 -> {
            val billions = value / 1_000_000_000.0
            if (billions % 1 == 0.0) {
                "${billions.toInt()}B"
            } else {
                String.format("%.1fB", billions)
            }
        }
        value >= 1_000_000 -> {
            val millions = value / 1_000_000.0
            if (millions % 1 == 0.0) {
                "${millions.toInt()}M"
            } else {
                String.format("%.1fM", millions)
            }
        }
        value >= 1_000 -> {
            val thousands = value / 1_000.0
            if (thousands % 1 == 0.0) {
                "${thousands.toInt()}K"
            } else {
                String.format("%.1fK", thousands)
            }
        }
        else -> value.toString()
    }
}
