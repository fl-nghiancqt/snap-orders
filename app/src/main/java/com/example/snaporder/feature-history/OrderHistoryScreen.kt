package com.example.snaporder.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.model.OrderStatus
import com.example.snaporder.ui.theme.SnapOrderTheme
import com.example.snaporder.ui.theme.SnapOrdersColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Order History Screen - Displays list of past orders for the current user.
 * 
 * LAYOUT STRUCTURE:
 * 1. Top App Bar ("Order History" with optional back icon)
 * 2. Orders List (LazyColumn with OrderHistoryCard)
 * 3. Empty State (icon, text when no orders)
 * 
 * DESIGN:
 * - Modern, minimal white-green theme
 * - Rounded cards for order items
 * - Clean spacing and typography
 * - Color-coded status chips
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBackClick: () -> Unit = {},
    onOrderClick: (String) -> Unit = {},
    viewModel: OrderHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            OrderHistoryTopAppBar(onBackClick = onBackClick)
        },
        containerColor = SnapOrdersColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.errorMessage != null && uiState.orders.isEmpty() -> {
                    ErrorState(
                        message = uiState.errorMessage ?: "Unknown error",
                        onRetry = { viewModel.onRefresh() }
                    )
                }
                uiState.orders.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    OrdersList(
                        orders = uiState.orders,
                        onOrderClick = { orderId ->
                            viewModel.onOrderClick(orderId)
                            onOrderClick(orderId)
                        },
                        onRefresh = { viewModel.onRefresh() }
                    )
                }
            }
        }
    }
}

/**
 * Top App Bar for Order History Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderHistoryTopAppBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Order History",
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
}

/**
 * Orders List - LazyColumn displaying order history cards.
 */
@Composable
private fun OrdersList(
    orders: List<Order>,
    onOrderClick: (String) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(orders) { order ->
            OrderHistoryCard(
                order = order,
                onClick = { onOrderClick(order.id) }
            )
        }
    }
}

/**
 * Order History Card - Individual card for each order in history.
 * 
 * UI ELEMENTS:
 * - Order ID
 * - Table number
 * - Order status (color-coded chip)
 * - Total price (bold)
 * - Created time (formatted)
 * - Right arrow icon (for detail navigation)
 */
@Composable
fun OrderHistoryCard(
    order: Order,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SnapOrdersColors.Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Order info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Order ID (horizontal label-value, no truncation)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Order ID:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SnapOrdersColors.TextSecondary
                    )
                    Text(
                        text = order.id,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = SnapOrdersColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Visible
                    )
                }
                
                // Table Number (horizontal label-value)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Table:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SnapOrdersColors.TextSecondary
                    )
                    Text(
                        text = "#${order.tableNumber}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SnapOrdersColors.TextPrimary
                    )
                }
                
                // Status chip
                OrderStatusChip(status = order.status)
                
                // Created time
                Text(
                    text = formatCreatedTime(order.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = SnapOrdersColors.TextSecondary
                )
            }
            
            // Right side: Total price and arrow
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Total price
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatPrice(order.totalPrice.toInt()),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = SnapOrdersColors.Primary
                    )
                }
                
                // Arrow icon (pointing right)
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "View details",
                    tint = SnapOrdersColors.TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(180f) // Rotate to point right
                )
            }
        }
    }
}

/**
 * Order Status Chip - Displays order status with color coding.
 */
@Composable
private fun OrderStatusChip(status: OrderStatus) {
    val (statusText, statusColor) = when (status) {
        OrderStatus.CREATED -> "CREATED" to Color(0xFF2196F3) // Blue
        OrderStatus.PREPARING -> "PREPARING" to Color(0xFFFF9800) // Orange
        OrderStatus.PAID -> "PAID" to Color(0xFF4CAF50) // Green
        OrderStatus.CANCELLED -> "CANCELLED" to Color(0xFFF44336) // Red
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = statusColor.copy(alpha = 0.1f),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = statusColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Format created time for display.
 * Shows "Today", "Yesterday", or formatted date.
 */
private fun formatCreatedTime(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return "Unknown"
    
    val orderTime = Date(timestamp.seconds * 1000)
    val now = Date()
    val calendar = Calendar.getInstance()
    
    // Check if today
    calendar.time = now
    val todayStart = calendar.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
    
    // Check if yesterday
    calendar.time = now
    val yesterdayStart = calendar.apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
    
    return when {
        orderTime >= todayStart -> {
            // Today - show time
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            "Today ${timeFormat.format(orderTime)}"
        }
        orderTime >= yesterdayStart -> {
            // Yesterday - show time
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            "Yesterday ${timeFormat.format(orderTime)}"
        }
        else -> {
            // Older - show date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateFormat.format(orderTime)
        }
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
 * Loading State - Shows while loading orders.
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = SnapOrdersColors.Primary
        )
    }
}

/**
 * Empty State - Shows when no orders exist.
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Receipt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SnapOrdersColors.TextSecondary
            )
            Text(
                text = "No orders yet",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SnapOrdersColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your order history will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = SnapOrdersColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Error State - Shows error message.
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = SnapOrdersColors.Error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SnapOrdersColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SnapOrdersColors.Primary
                )
            ) {
                Text("Retry")
            }
        }
    }
}

/**
 * Preview with sample order data.
 */
@Preview(showBackground = true)
@Composable
private fun OrderHistoryScreenPreview() {
    SnapOrderTheme {
        OrderHistoryScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderHistoryCardPreview() {
    SnapOrderTheme {
        val sampleOrder = Order(
            id = "ORD-001",
            tableNumber = 5,
            status = OrderStatus.PAID,
            items = emptyList(),
            totalPrice = 105000.0,
            createdAt = com.google.firebase.Timestamp.now(),
            userId = "user_123"
        )
        
        OrderHistoryCard(
            order = sampleOrder,
            onClick = {}
        )
    }
}

