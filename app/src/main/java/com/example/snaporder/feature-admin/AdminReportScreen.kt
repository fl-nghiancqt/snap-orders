package com.example.snaporder.feature.admin

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
            if (uiState.dailyRevenueTrends.isNotEmpty()) {
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
                            val maxRevenue = uiState.dailyRevenueTrends.maxOfOrNull { it.revenue } ?: 1.0
                            
                            uiState.dailyRevenueTrends.forEach { dailyRevenue ->
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = formatDate(dailyRevenue.date),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SnapOrdersColors.TextSecondary
                                        )
                                        Text(
                                            text = formatPrice(dailyRevenue.revenue.toInt()),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = SnapOrdersColors.Primary
                                        )
                                    }
                                    // Simple bar chart
                                    BarChart(
                                        value = dailyRevenue.revenue,
                                        maxValue = maxRevenue,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
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
private fun BarChart(
    value: Double,
    maxValue: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = ((value / maxValue) * width.toDouble()).toFloat().coerceAtLeast(0f).coerceAtMost(width)
        
        // Draw bar
        drawRect(
            color = SnapOrdersColors.Primary,
            topLeft = Offset(0f, 0f),
            size = Size(barWidth, height)
        )
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
