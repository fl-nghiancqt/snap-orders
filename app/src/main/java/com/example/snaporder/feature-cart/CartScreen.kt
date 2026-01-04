package com.example.snaporder.feature.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snaporder.core.di.UserSessionEntryPoint
import com.example.snaporder.core.model.CartItem
import com.example.snaporder.core.model.OrderDraft
import com.example.snaporder.core.session.UserSessionManager
import com.example.snaporder.ui.theme.SnapOrderTheme
import com.example.snaporder.ui.theme.SnapOrdersColors
import dagger.hilt.android.EntryPointAccessors

/**
 * Cart Screen - Displays cart items, allows quantity adjustment,
 * table number input, and order placement.
 * 
 * LAYOUT STRUCTURE:
 * 1. Top App Bar ("Your Order" with back icon)
 * 2. Cart Items List (LazyColumn with CartItemRow)
 * 3. Table Number Input (OutlinedTextField)
 * 4. Order Summary (Subtotal, Service fee, Total)
 * 5. Place Order Button (full width, primary color)
 * 6. Empty Cart State (icon, text, "Back to Menu" button)
 * 
 * DESIGN:
 * - Modern, minimal white-green theme
 * - Rounded cards for cart items
 * - Clean spacing and typography
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBackClick: () -> Unit = {},
    onPlaceOrderClick: (orderId: String) -> Unit = {},
    viewModel: CartViewModel = hiltViewModel(),
    userSessionManager: UserSessionManager = rememberUserSessionManager()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by userSessionManager.currentUser.collectAsStateWithLifecycle()
    val orderPlacementResult by viewModel.orderPlacementResult.collectAsStateWithLifecycle()
    
    // Handle navigation when order is successfully placed
    LaunchedEffect(orderPlacementResult) {
        when (val result = orderPlacementResult) {
            is CartViewModel.OrderPlacementResult.Success -> {
                viewModel.clearOrderPlacementResult()
                onPlaceOrderClick(result.orderId)
            }
            is CartViewModel.OrderPlacementResult.Error -> {
                // Error is already shown in UI state
            }
            null -> {
                // No result yet
            }
        }
    }
    
    Scaffold(
        topBar = {
            CartTopAppBar(onBackClick = onBackClick)
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
                uiState.errorMessage != null && uiState.orderDraft == null -> {
                    ErrorState(
                        message = uiState.errorMessage ?: "Unknown error",
                        onRetry = { /* TODO: Retry logic */ }
                    )
                }
                uiState.orderDraft == null || uiState.orderDraft?.items?.isEmpty() == true -> {
                    EmptyCartState(onBackToMenu = onBackClick)
                }
                else -> {
                    CartContent(
                        orderDraft = uiState.orderDraft!!,
                        currentUser = currentUser,
                        viewModel = viewModel,
                        onIncreaseQuantity = { itemId ->
                            viewModel.onIncreaseQuantity(itemId)
                        },
                        onDecreaseQuantity = { itemId ->
                            viewModel.onDecreaseQuantity(itemId)
                        },
                        onTableNumberChange = { value ->
                            viewModel.onTableNumberChange(value)
                        },
                        onPlaceOrderClick = {
                            android.util.Log.d("CartScreen", "Place Order button clicked")
                            android.util.Log.d("CartScreen", "currentUser: ${currentUser?.id}, name: ${currentUser?.name}")
                            android.util.Log.d("CartScreen", "orderDraft: ${uiState.orderDraft}, isValid: ${uiState.orderDraft?.isValid}")
                            val userId = currentUser?.id ?: ""
                            if (userId.isNotBlank()) {
                                android.util.Log.d("CartScreen", "Calling viewModel.onPlaceOrderClick with userId='$userId'")
                                viewModel.onPlaceOrderClick(userId)
                            } else {
                                android.util.Log.w("CartScreen", "Cannot place order: userId is blank. User not logged in?")
                                // Set error message via ViewModel
                                // viewModel.setErrorMessage("Please log in to place an order")
                                viewModel.onPlaceOrderClick(userId)
                            }
                        },
                        errorMessage = uiState.errorMessage
                    )
                }
            }
        }
    }
}

/**
 * Top App Bar for Cart Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartTopAppBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Your Order",
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
 * Main cart content with items, table input, summary, and button.
 */
@Composable
private fun CartContent(
    orderDraft: OrderDraft,
    currentUser: com.example.snaporder.core.model.User?,
    viewModel: CartViewModel,
    onIncreaseQuantity: (String) -> Unit,
    onDecreaseQuantity: (String) -> Unit,
    onTableNumberChange: (String) -> Unit,
    onPlaceOrderClick: () -> Unit,
    errorMessage: String?
) {
    // Local state for table number text input
    // Sync with orderDraft when it changes (e.g., after validation)
    var tableNumberText by remember(orderDraft.tableNumber) {
        mutableStateOf(orderDraft.tableNumber?.toString() ?: "")
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Error message (if any)
        if (errorMessage != null) {
            item {
                ErrorCard(message = errorMessage)
            }
        }
        
        // Cart items
        items(orderDraft.items) { item ->
            CartItemRow(
                item = item,
                onIncreaseQuantity = { onIncreaseQuantity(item.id) },
                onDecreaseQuantity = { onDecreaseQuantity(item.id) }
            )
        }
        
        // Table number input
        item {
            TableNumberInput(
                value = tableNumberText,
                onValueChange = { newValue ->
                    tableNumberText = newValue
                    onTableNumberChange(newValue)
                }
            )
        }
        
        // Order summary
        item {
            OrderSummary(
                subtotal = orderDraft.subtotal,
                serviceFee = orderDraft.serviceFee,
                totalPrice = orderDraft.totalPrice
            )
        }
        
        // Place order button
        item {
            // Log button state for debugging
            LaunchedEffect(orderDraft.isValid, orderDraft.items.size, orderDraft.tableNumber) {
                android.util.Log.d("CartScreen", "PlaceOrderButton state - enabled: ${orderDraft.isValid}, items: ${orderDraft.items.size}, table: ${orderDraft.tableNumber}")
            }
            
            PlaceOrderButton(
                enabled = orderDraft.isValid,
                onClick = onPlaceOrderClick
            )
        }
        
        // DEBUG: Test order button (for debugging Firestore order creation)
        item {
            DebugTestOrderButton(
                onClick = {
                    val userId = currentUser?.id ?: "test_user"
                    viewModel.createTestOrderDirectly(userId)
                }
            )
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Cart Item Row - Individual row for each cart item.
 * 
 * UI ELEMENTS:
 * - Item name
 * - Unit price
 * - Quantity selector (- qty +)
 * - Subtotal (price * quantity)
 */
@Composable
fun CartItemRow(
    item: CartItem,
    onIncreaseQuantity: () -> Unit,
    onDecreaseQuantity: () -> Unit
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item info (name and unit price)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = SnapOrdersColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatPrice(item.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnapOrdersColors.TextSecondary
                )
            }
            
            // Quantity selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Decrease button
                IconButton(
                    onClick = onDecreaseQuantity,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.titleLarge,
                        color = SnapOrdersColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Quantity display
                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.TextPrimary,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
                
                // Increase button
                IconButton(
                    onClick = onIncreaseQuantity,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.titleLarge,
                        color = SnapOrdersColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Subtotal
            Text(
                text = formatPrice(item.subtotal),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = SnapOrdersColors.Primary,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Table Number Input - OutlinedTextField for table number.
 */
@Composable
private fun TableNumberInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Table Number") },
        placeholder = { Text("Enter table number") },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SnapOrdersColors.Primary,
            unfocusedBorderColor = SnapOrdersColors.Outline
        )
    )
}

/**
 * Order Summary - Shows subtotal, service fee, and total.
 */
@Composable
private fun OrderSummary(
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
}

/**
 * Place Order Button - Full width, primary color.
 */
@Composable
private fun PlaceOrderButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = {
            android.util.Log.d("CartScreen", "PlaceOrderButton: onClick triggered, enabled=$enabled")
            if (enabled) {
                onClick()
            } else {
                android.util.Log.w("CartScreen", "PlaceOrderButton: Button is disabled, cannot place order")
            }
        },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SnapOrdersColors.Primary,
            disabledContainerColor = SnapOrdersColors.Outline
        )
    ) {
        Text(
            text = "Place Order",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (enabled) {
                SnapOrdersColors.OnPrimary
            } else {
                SnapOrdersColors.TextSecondary
            }
        )
    }
}

/**
 * DEBUG: Test Order Button - Creates a test order directly in Firestore.
 * This button is for debugging purposes only.
 */
@Composable
private fun DebugTestOrderButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text(
            text = "🔧 DEBUG: Create Test Order",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onError
        )
    }
}

/**
 * Empty Cart State - Shows when cart is empty.
 */
@Composable
private fun EmptyCartState(
    onBackToMenu: () -> Unit
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
            Icon(
                imageVector = Icons.Filled.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SnapOrdersColors.TextSecondary
            )
            Text(
                text = "Your cart is empty",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SnapOrdersColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onBackToMenu,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SnapOrdersColors.Primary
                )
            ) {
                Text(
                    text = "Back to Menu",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

/**
 * Loading State - Shows while loading cart items.
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
 * Error Card - Shows error message in a card.
 */
@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Remember UserSessionManager instance.
 * This provides access to the shared user session from any composable.
 */
@Composable
private fun rememberUserSessionManager(): UserSessionManager {
    val context = LocalContext.current
    return remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            UserSessionEntryPoint::class.java
        ).userSessionManager()
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
 * Preview with sample data.
 */
@Preview(showBackground = true)
@Composable
private fun CartScreenPreview() {
    SnapOrderTheme {
        CartScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun CartItemRowPreview() {
    SnapOrderTheme {
        CartItemRow(
            item = CartItem(
                id = "1",
                menuItemId = "1",
                name = "Trà đào",
                price = 30000,
                quantity = 2
            ),
            onIncreaseQuantity = {},
            onDecreaseQuantity = {}
        )
    }
}

