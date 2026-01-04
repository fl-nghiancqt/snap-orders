package com.example.snaporder.feature.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

/**
 * Order Result Screen - Displays order confirmation after successful placement.
 * 
 * LAYOUT STRUCTURE:
 * 1. Success Header (icon, title, order ID)
 * 2. Order Summary Card (table, status, items, totals)
 * 3. Action Buttons (Back to Menu, View Order Detail)
 * 
 * DESIGN:
 * - Modern, minimal white-green theme
 * - Rounded cards
 * - Clean spacing and typography
 * - Success state with checkmark icon
 */
@Composable
fun OrderResultScreen(
    orderId: String,
    onBackToMenuClick: () -> Unit = {},
    onViewOrderDetailClick: () -> Unit = {},
    viewModel: OrderResultViewModel = hiltViewModel()
) {
    // Load order when orderId changes
    androidx.compose.runtime.LaunchedEffect(orderId) {
        if (orderId.isNotBlank()) {
            viewModel.loadOrder(orderId)
        }
    }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SnapOrdersColors.Background)
    ) {
        when {
            uiState.isLoading -> {
                LoadingState()
            }
            uiState.errorMessage != null -> {
                ErrorState(
                    message = uiState.errorMessage ?: "Unknown error",
                    onRetry = { 
                        if (orderId.isNotBlank()) {
                            viewModel.loadOrder(orderId)
                        }
                    }
                )
            }
            uiState.order != null -> {
                val order = uiState.order!!
                // Calculate subtotal from items
                val subtotal = order.items.sumOf { it.totalPrice }.toInt()
                // Calculate service fee: totalPrice - subtotal
                val serviceFee = (order.totalPrice - subtotal).toInt()
                
                OrderResultContent(
                    order = order,
                    subtotal = subtotal,
                    serviceFee = serviceFee,
                    onBackToMenuClick = {
                        viewModel.onBackToMenuClick()
                        onBackToMenuClick()
                    },
                    onViewOrderDetailClick = {
                        viewModel.onViewOrderDetailClick()
                        onViewOrderDetailClick()
                    }
                )
            }
        }
    }
}

/**
 * Main order result content with success header, summary, and actions.
 */
@Composable
private fun OrderResultContent(
    order: Order,
    subtotal: Int,
    serviceFee: Int,
    onBackToMenuClick: () -> Unit,
    onViewOrderDetailClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Success Header
        SuccessHeader(orderId = order.id)
        
        // Order Summary Card
        OrderSummaryCard(
            order = order,
            subtotal = subtotal,
            serviceFee = serviceFee,
            totalPrice = order.totalPrice.toInt()
        )
        
        // Action Buttons
        ActionButtons(
            onBackToMenuClick = onBackToMenuClick,
            onViewOrderDetailClick = onViewOrderDetailClick
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Success Header - Large checkmark icon, title, and order ID.
 */
@Composable
private fun SuccessHeader(orderId: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success icon
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = SnapOrdersColors.Primary
        )
        
        // Success title
        Text(
            text = "Order Placed Successfully",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = SnapOrdersColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        
        // Order ID
        Text(
            text = "Order ID: $orderId",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = SnapOrdersColors.Primary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Order Summary Card - Displays order details.
 */
@Composable
fun OrderSummaryCard(
    order: Order,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Table number and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Table",
                        style = MaterialTheme.typography.bodySmall,
                        color = SnapOrdersColors.TextSecondary
                    )
                    Text(
                        text = "#${order.tableNumber}",
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
            
            // Order items
            Text(
                text = "Items",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SnapOrdersColors.TextPrimary
            )
            
            order.items.forEach { item ->
                OrderItemRow(item = item)
            }
            
            HorizontalDivider(color = SnapOrdersColors.Outline)
            
            // Price breakdown
            PriceBreakdown(
                subtotal = subtotal,
                serviceFee = serviceFee,
                totalPrice = totalPrice
            )
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
private fun OrderItemRow(item: OrderItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.menuItemName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SnapOrdersColors.TextPrimary
            )
            Text(
                text = "${formatPrice(item.price.toInt())} x ${item.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = SnapOrdersColors.TextSecondary
            )
        }
        
        Text(
            text = formatPrice(item.totalPrice.toInt()),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = SnapOrdersColors.Primary
        )
    }
}

/**
 * Price Breakdown - Shows subtotal, service fee, and total.
 */
@Composable
private fun PriceBreakdown(
    subtotal: Int,
    serviceFee: Int,
    totalPrice: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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

/**
 * Action Buttons - Back to Menu and View Order Detail.
 */
@Composable
private fun ActionButtons(
    onBackToMenuClick: () -> Unit,
    onViewOrderDetailClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary button: Back to Menu
        Button(
            onClick = onBackToMenuClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnapOrdersColors.Primary
            )
        ) {
            Text(
                text = "Back to Menu",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = SnapOrdersColors.OnPrimary
            )
        }
        
        // Secondary button: View Order Detail
        OutlinedButton(
            onClick = onViewOrderDetailClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SnapOrdersColors.Primary
            )
        ) {
            Text(
                text = "View Order Detail",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
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
 * Format price in VND format.
 * Example: 30000 -> "30,000 đ"
 */
private fun formatPrice(price: Int): String {
    return "${price.toString().reversed().chunked(3).joinToString(",").reversed()} đ"
}

/**
 * Preview with sample order data.
 */
@Preview(showBackground = true)
@Composable
private fun OrderResultScreenPreview() {
    SnapOrderTheme {
        OrderResultScreen(orderId = "ORD-001")
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderSummaryCardPreview() {
    SnapOrderTheme {
        val sampleOrder = Order(
            id = "ORD-001",
            tableNumber = 5,
            status = OrderStatus.CREATED,
            items = listOf(
                OrderItem(
                    menuItemId = "1",
                    menuItemName = "Trà đào",
                    price = 30000.0,
                    quantity = 2
                ),
                OrderItem(
                    menuItemId = "2",
                    menuItemName = "Bạc xỉu",
                    price = 35000.0,
                    quantity = 1
                )
            ),
            totalPrice = 105000.0,
            createdAt = com.google.firebase.Timestamp.now(),
            userId = "user_123"
        )
        
        OrderSummaryCard(
            order = sampleOrder,
            subtotal = 95000,
            serviceFee = 10000,
            totalPrice = 105000
        )
    }
}

