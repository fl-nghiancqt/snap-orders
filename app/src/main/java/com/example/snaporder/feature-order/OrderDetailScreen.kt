package com.example.snaporder.feature.order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snaporder.core.model.CartItem
import com.example.snaporder.core.model.MenuItem
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
    val addItemsResult by viewModel.addItemsResult.collectAsState()
    val statusChangeResult by viewModel.statusChangeResult.collectAsState()
    val addMoreItemsDialogState by viewModel.addMoreItemsDialogState.collectAsState()
    val dialogCartItems by viewModel.dialogCartItems.collectAsState()
    
    // Load order when screen is opened
    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }
    
    // Handle add items result
    LaunchedEffect(addItemsResult) {
        addItemsResult?.let { result ->
            when (result) {
                is AddItemsResult.Success -> {
                    viewModel.clearAddItemsResult()
                    // Order will be reloaded automatically
                }
                is AddItemsResult.Error -> {
                    // Error is already shown in UI state
                }
            }
        }
    }
    
    // Handle status change result
    LaunchedEffect(statusChangeResult) {
        statusChangeResult?.let { result ->
            when (result) {
                is StatusChangeResult.Success -> {
                    viewModel.clearStatusChangeResult()
                    // Order will be reloaded automatically
                }
                is StatusChangeResult.Error -> {
                    // Error is already shown in UI state
                }
            }
        }
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
                        totalPrice = uiState.order!!.totalPrice.toInt(),
                        onAddMoreItemsClick = {
                            viewModel.openAddMoreItemsDialog(uiState.order!!)
                        },
                        onStatusChange = { newStatus ->
                            viewModel.changeOrderStatus(orderId, newStatus)
                        }
                    )
                }
            }
            
            // Add More Items Dialog
            addMoreItemsDialogState?.let { dialogState ->
                AddMoreItemsDialog(
                    dialogState = dialogState,
                    dialogCartItems = dialogCartItems,
                    onDismiss = {
                        viewModel.closeAddMoreItemsDialog()
                    },
                    onAddItem = { menuItem ->
                        viewModel.addItemToDialogCart(menuItem)
                    },
                    onUpdateQuantity = { cartItemId, quantity ->
                        viewModel.updateDialogCartItemQuantity(cartItemId, quantity)
                    },
                    onRemoveItem = { cartItemId ->
                        viewModel.removeItemFromDialogCart(cartItemId)
                    },
                    onConfirm = {
                        viewModel.confirmAddMoreItems(dialogState.order)
                    },
                    onFilterChange = { query ->
                        viewModel.updateDialogFilterQuery(query)
                    },
                    updatedBilling = viewModel.calculateUpdatedBilling(dialogState.order, dialogCartItems),
                    isLoading = uiState.isLoading
                )
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
    totalPrice: Int,
    onAddMoreItemsClick: () -> Unit,
    onStatusChange: (OrderStatus) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Order Info Section
        item {
            OrderInfoCard(
                order = order,
                onStatusChange = onStatusChange
            )
        }
        
        // Add More Items Button (only if order can accept new items)
        item {
            if (order.status == OrderStatus.CREATED || order.status == OrderStatus.PREPARING) {
                Button(
                    onClick = onAddMoreItemsClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SnapOrdersColors.Primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add More Items",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
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
private fun OrderInfoCard(
    order: Order,
    onStatusChange: (OrderStatus) -> Unit
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
            // Order ID (full width, no truncation)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Order ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = SnapOrdersColors.TextSecondary
                )
                Text(
                    text = order.id,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.TextPrimary,
                    maxLines = 1
                )
            }
            
            // Status selector (full width, separate row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status:",
                    style = MaterialTheme.typography.bodySmall,
                    color = SnapOrdersColors.TextSecondary
                )
                OrderStatusSelector(
                    currentStatus = order.status,
                    onStatusChange = onStatusChange
                )
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
 * Order Status Selector - Displays order status with color coding and allows changing status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderStatusSelector(
    currentStatus: OrderStatus,
    onStatusChange: (OrderStatus) -> Unit
) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    val (statusText, statusColor) = when (currentStatus) {
        OrderStatus.CREATED -> "CREATED" to Color(0xFF2196F3) // Blue
        OrderStatus.PREPARING -> "PREPARING" to Color(0xFFFF9800) // Orange
        OrderStatus.PAID -> "PAID" to Color(0xFF4CAF50) // Green
        OrderStatus.CANCELLED -> "CANCELLED" to Color(0xFFF44336) // Red
    }
    
    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = statusColor.copy(alpha = 0.1f),
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = statusColor
                )
                Text(
                    text = "▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            OrderStatus.values().forEach { status ->
                val (text, color) = when (status) {
                    OrderStatus.CREATED -> "CREATED" to Color(0xFF2196F3)
                    OrderStatus.PREPARING -> "PREPARING" to Color(0xFFFF9800)
                    OrderStatus.PAID -> "PAID" to Color(0xFF4CAF50)
                    OrderStatus.CANCELLED -> "CANCELLED" to Color(0xFFF44336)
                }
                
                DropdownMenuItem(
                    text = {
                        Text(
                            text = text,
                            color = if (status == currentStatus) color else SnapOrdersColors.TextPrimary
                        )
                    },
                    onClick = {
                        if (status != currentStatus) {
                            onStatusChange(status)
                        }
                        expanded = false
                    }
                )
            }
        }
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

private fun formatPrice(price: Double): String {
    return formatPrice(price.toInt())
}

/**
 * Add More Items Dialog - Dialog for adding more items to an existing order.
 * Shows current order items, menu items to add, selected items, and updated billing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMoreItemsDialog(
    dialogState: AddMoreItemsDialogState,
    dialogCartItems: List<CartItem>,
    onDismiss: () -> Unit,
    onAddItem: (MenuItem) -> Unit,
    onUpdateQuantity: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onConfirm: () -> Unit,
    onFilterChange: (String) -> Unit,
    updatedBilling: UpdatedBilling,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add More Items",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current Order Items Section
                item {
                    Text(
                        text = "Current Order Items",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = SnapOrdersColors.TextPrimary
                    )
                }
                
                items(dialogState.order.items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SnapOrdersColors.Surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                                    text = "${item.quantity} × ${formatPrice(item.price)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SnapOrdersColors.TextSecondary
                                )
                            }
                            Text(
                                text = formatPrice(item.totalPrice),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SnapOrdersColors.Primary
                            )
                        }
                    }
                }
                
                // New Items to Add Section
                if (dialogCartItems.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "New Items to Add",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = SnapOrdersColors.Primary
                        )
                    }
                    
                    items(dialogCartItems) { cartItem ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = SnapOrdersColors.Primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cartItem.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = SnapOrdersColors.TextPrimary
                                    )
                                    Text(
                                        text = formatPrice(cartItem.price),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SnapOrdersColors.TextSecondary
                                    )
                                }
                                
                                // Quantity selector
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onUpdateQuantity(cartItem.id, cartItem.quantity - 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Remove,
                                            contentDescription = "Decrease",
                                            modifier = Modifier.size(18.dp),
                                            tint = SnapOrdersColors.Primary
                                        )
                                    }
                                    Text(
                                        text = "${cartItem.quantity}",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.width(30.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    IconButton(
                                        onClick = { onUpdateQuantity(cartItem.id, cartItem.quantity + 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Increase",
                                            modifier = Modifier.size(18.dp),
                                            tint = SnapOrdersColors.Primary
                                        )
                                    }
                                }
                                
                                Text(
                                    text = formatPrice(cartItem.price * cartItem.quantity),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = SnapOrdersColors.Primary,
                                    modifier = Modifier.width(80.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
                
                // Menu Items Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select Items to Add",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = SnapOrdersColors.TextPrimary
                    )
                }
                
                // Filter Input
                item {
                    OutlinedTextField(
                        value = dialogState.filterQuery,
                        onValueChange = onFilterChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search menu items by name...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = SnapOrdersColors.TextSecondary
                            )
                        },
                        trailingIcon = {
                            if (dialogState.filterQuery.isNotEmpty()) {
                                IconButton(onClick = { onFilterChange("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear",
                                        tint = SnapOrdersColors.TextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SnapOrdersColors.Primary,
                            unfocusedBorderColor = SnapOrdersColors.Outline,
                            focusedContainerColor = SnapOrdersColors.Surface,
                            unfocusedContainerColor = SnapOrdersColors.Surface
                        )
                    )
                }
                
                items(dialogState.filteredMenuItems.filter { it.available }) { menuItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SnapOrdersColors.Surface
                        ),
                        onClick = { onAddItem(menuItem) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = menuItem.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = SnapOrdersColors.TextPrimary
                                )
                                Text(
                                    text = formatPrice(menuItem.price),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SnapOrdersColors.Primary
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add",
                                tint = SnapOrdersColors.Primary
                            )
                        }
                    }
                }
                
                // Updated Billing Section
                if (dialogCartItems.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = SnapOrdersColors.Primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Updated Billing",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = SnapOrdersColors.TextPrimary
                                )
                                
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
                                        text = formatPrice(updatedBilling.subtotal),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SnapOrdersColors.TextPrimary
                                    )
                                }
                                
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
                                        text = formatPrice(updatedBilling.serviceFee),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SnapOrdersColors.TextPrimary
                                    )
                                }
                                
                                HorizontalDivider()
                                
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
                                        text = formatPrice(updatedBilling.total),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = SnapOrdersColors.Primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = dialogCartItems.isNotEmpty() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SnapOrdersColors.Primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Add Items to Order")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        containerColor = SnapOrdersColors.Background,
        shape = RoundedCornerShape(20.dp)
    )
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

