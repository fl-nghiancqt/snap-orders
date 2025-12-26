package com.example.snaporder.feature.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snaporder.core.model.Order
import com.example.snaporder.core.model.OrderItem
import com.example.snaporder.core.model.OrderStatus
import com.example.snaporder.ui.theme.SnapOrderTheme
import com.example.snaporder.ui.theme.SnapOrdersColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Order Detail Screen - Displays detailed information about a specific order.
 * 
 * LAYOUT STRUCTURE:
 * 1. Top App Bar ("Order Detail" with back icon)
 * 2. Order Info Section (Order ID, Table, Status, Created time)
 * 3. Ordered Items List (LazyColumn with OrderItemRow)
 * 4. Price Summary Section (Subtotal, Service fee, Total)
 * 
 * DESIGN:
 * - Modern, minimal white-green theme
 * - Rounded cards
 * - Clean spacing and typography
 * - Color-coded status chips
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    onBackClick: () -> Unit = {},
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Load order when screen is opened
    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }
    
    Scaffold(
        topBar = {
            OrderDetailTopAppBar(onBackClick = onBackClick)
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
                uiState.errorMessage != null && uiState.order == null -> {
                    ErrorState(
                        message = uiState.errorMessage ?: "Unknown error",
                        onRetry = { viewModel.onRefresh() }
                    )
                }
                uiState.order != null -> {
                    OrderDetailContent(
                        order = uiState.order!!,
                        subtotal = viewModel.calculateSubtotal(uiState.order!!),
                        serviceFee = viewModel.getServiceFee(uiState.order!!),
                        totalPrice = uiState.order!!.totalPrice.toInt()
                    )
                }
            }
        }
    }
}

/**
 * Top App Bar for Order Detail Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderDetailTopAppBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Order Detail",
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
 * Main order detail content with info, items, and summary.
 */
@Composable
private fun OrderDetailContent(
    order: Order,
    subtotal: Int,
    serviceFee: Int,
    totalPrice: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Order Info Section
        item {
            OrderInfoCard(
                order = order
            )
        }
        
        // Ordered Items List
        item {
            Text(
                text = "Ordered Items",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = SnapOrdersColors.TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        items(order.items) { item ->
            OrderDetailItemRow(item = item)
        }
        
        // Price Summary Section
        item {
            PriceSummaryCard(
                subtotal = subtotal,
                serviceFee = serviceFee,
                totalPrice = totalPrice
            )
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Order Info Card - Displays order ID, table, status, and created time.
 */
@Composable
private fun OrderInfoCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
            // Order ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order ID",
                        style = MaterialTheme.typography.bodySmall,
                        color = SnapOrdersColors.TextSecondary
                    )
                    Text(
                        text = order.id,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = SnapOrdersColors.TextPrimary
                    )
                }
                
                // Status chip
                OrderStatusChip(status = order.status)
            }
            
            HorizontalDivider(color = SnapOrdersColors.Outline)
            
            // Table number and Created time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Table",
                        style = MaterialTheme.typography.bodySmall,
                        color = SnapOrdersColors.TextSecondary
                    )
                    Text(
                        text = "#${order.tableNumber}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SnapOrdersColors.TextPrimary
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Created",
                        style = MaterialTheme.typography.bodySmall,
                        color = SnapOrdersColors.TextSecondary
                    )
                    Text(
                        text = formatCreatedTime(order.createdAt),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SnapOrdersColors.TextPrimary
                    )
                }
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
        shape = RoundedCornerShape(12.dp),
        color = statusColor.copy(alpha = 0.1f),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = statusColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Order Item Row - Displays individual order item.
 */
@Composable
private fun OrderDetailItemRow(item: OrderItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = SnapOrdersColors.Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item name and details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.menuItemName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = SnapOrdersColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Quantity: ${item.quantity} × ${formatPrice(item.price.toInt())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SnapOrdersColors.TextSecondary
                )
            }
            
            // Subtotal
            Text(
                text = formatPrice(item.totalPrice.toInt()),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = SnapOrdersColors.Primary
            )
        }
    }
}

/**
 * Price Summary Card - Shows subtotal, service fee, and total.
 */
@Composable
private fun PriceSummaryCard(
    subtotal: Int,
    serviceFee: Int,
    totalPrice: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Price Summary",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = SnapOrdersColors.TextPrimary
            )
            
            HorizontalDivider(color = SnapOrdersColors.Outline)
            
            // Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Subtotal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnapOrdersColors.TextSecondary
                )
                Text(
                    text = formatPrice(subtotal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnapOrdersColors.TextPrimary
                )
            }
            
            // Service fee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Service Fee",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnapOrdersColors.TextSecondary
                )
                Text(
                    text = formatPrice(serviceFee),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnapOrdersColors.TextPrimary
                )
            }
            
            HorizontalDivider(color = SnapOrdersColors.Outline)
            
            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.TextPrimary
                )
                Text(
                    text = formatPrice(totalPrice),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.Primary
                )
            }
        }
    }
}

/**
 * Format created time for display.
 */
private fun formatCreatedTime(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return "Unknown"
    
    val orderTime = Date(timestamp.seconds * 1000)
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return dateFormat.format(orderTime)
}

/**
 * Format price in VND format.
 * Example: 30000 -> "30,000 đ"
 */
private fun formatPrice(price: Int): String {
    return "${price.toString().reversed().chunked(3).joinToString(",").reversed()} đ"
}

/**
 * Loading State - Shows while loading order data.
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
private fun OrderDetailScreenPreview() {
    SnapOrderTheme {
        OrderDetailScreen(orderId = "ORD-003")
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderDetailItemRowPreview() {
    SnapOrderTheme {
        OrderDetailItemRow(
            item = OrderItem(
                menuItemId = "1",
                menuItemName = "Trà đào",
                price = 30000.0,
                quantity = 2
            )
        )
    }
}

