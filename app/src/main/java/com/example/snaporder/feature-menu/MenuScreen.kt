package com.example.snaporder.feature.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.snaporder.core.navigation.BottomNavigationBar
import com.example.snaporder.core.navigation.NavRoutes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.snaporder.core.model.MenuItem
import com.example.snaporder.ui.theme.SnapOrderTheme
import com.example.snaporder.ui.theme.SnapOrdersColors

/**
 * Menu Screen - Displays available menu items in a grid layout.
 * 
 * LAYOUT STRUCTURE:
 * 1. Top App Bar with title, subtitle, and cart icon with badge
 * 2. LazyVerticalGrid (2 columns) showing menu items
 * 3. Loading, empty, and error states
 * 
 * DESIGN:
 * - Modern, minimal white-green theme
 * - Rounded cards for menu items
 * - Clean spacing and typography
 */
@Composable
fun MenuScreen(
    navController: NavController? = null,
    onCartClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    viewModel: MenuViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController?.currentBackStackEntryAsState() ?: kotlinx.coroutines.flow.flowOf(null).collectAsState(initial = null)
    val currentRoute = navBackStackEntry?.destination?.route
    
    Scaffold(
        topBar = {
            MenuTopAppBar(
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                cartItemCount = uiState.cartItemCount,
                onCartClick = {
                    viewModel.onCartClick()
                    onCartClick()
                },
                onHistoryClick = onHistoryClick,
                onProfileClick = onProfileClick
            )
        },
        containerColor = SnapOrdersColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.items.isEmpty() -> {
                    LoadingState()
                }
                uiState.errorMessage != null && uiState.items.isEmpty() -> {
                    ErrorState(
                        message = uiState.errorMessage ?: "Unknown error",
                        onRetry = { 
                            // Flow will automatically retry on next emission
                            // No manual action needed
                        }
                    )
                }
                uiState.items.isEmpty() && !uiState.isLoading -> {
                    EmptyState()
                }
                else -> {
                    MenuGrid(
                        items = uiState.items,
                        onAddToCart = { itemId ->
                            viewModel.onAddToCart(itemId)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Top App Bar for Menu Screen.
 * 
 * Features:
 * - Title: "SnapOrders"
 * - Subtitle: "Menu"
 * - Cart icon with badge showing item count
 * - Back button on the left
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuTopAppBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "SnapOrders",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.TextPrimary
                )
                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.bodySmall,
                    color = SnapOrdersColors.TextSecondary
                )
            }
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
            containerColor = SnapOrdersColors.Background,
            titleContentColor = SnapOrdersColors.TextPrimary
        )
    )
}

/**
 * Menu Grid - Displays menu items in a 2-column grid.
 */
@Composable
private fun MenuGrid(
    items: List<MenuItem>,
    onAddToCart: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { item ->
            MenuItemCard(
                item = item,
                onAddToCart = { onAddToCart(item.id) }
            )
        }
    }
}

/**
 * Menu Item Card - Individual card for each menu item.
 * 
 * UI ELEMENTS:
 * - Image placeholder (using AsyncImage with Coil)
 * - Food name
 * - Price (bold, formatted)
 * - Add (+) button
 * - Disabled state when item is unavailable
 */
@Composable
private fun MenuItemCard(
    item: MenuItem,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
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
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder when no image URL
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = SnapOrdersColors.TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Food name
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SnapOrdersColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Price
            Text(
                text = formatPrice(item.price.toInt()),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = SnapOrdersColors.Primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Add button
            Button(
                onClick = onAddToCart,
                enabled = item.available,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (item.available) {
                        SnapOrdersColors.Primary
                    } else {
                        SnapOrdersColors.Outline
                    },
                    contentColor = if (item.available) {
                        SnapOrdersColors.OnPrimary
                    } else {
                        SnapOrdersColors.TextSecondary
                    },
                    disabledContainerColor = SnapOrdersColors.Outline,
                    disabledContentColor = SnapOrdersColors.TextSecondary
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (item.available) "+" else "Unavailable",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

/**
 * Loading State - Shows progress indicator while loading menu.
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
 * Error State - Shows error message with retry option.
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
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = SnapOrdersColors.Error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SnapOrdersColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
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
 * Empty State - Shows message when no menu items are available.
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Restaurant,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SnapOrdersColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No menu available",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SnapOrdersColors.TextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Format price to Vietnamese currency format.
 * Example: 30000 -> "30,000 đ"
 */
private fun formatPrice(price: Int): String {
    return "${price.toString().reversed().chunked(3).joinToString(",").reversed()} đ"
}

/**
 * Preview composable for MenuScreen.
 * Uses sample data for preview.
 */
@Preview(showBackground = true)
@Composable
private fun MenuScreenPreview() {
    SnapOrderTheme {
        // Preview with sample data
        val sampleItems = listOf(
            MenuItem(
                id = "1",
                name = "Trà đào",
                price = 30000.0,
                available = true,
                imageUrl = ""
            ),
            MenuItem(
                id = "2",
                name = "Bạc xỉu",
                price = 35000.0,
                available = true,
                imageUrl = ""
            ),
            MenuItem(
                id = "3",
                name = "Cà phê sữa",
                price = 25000.0,
                available = false,
                imageUrl = ""
            )
        )
        
        Scaffold(
            topBar = {
            MenuTopAppBar(
                onBackClick = {}
            )
            },
            containerColor = SnapOrdersColors.Background
        ) { paddingValues ->
            MenuGrid(
                items = sampleItems,
                onAddToCart = {}
            )
        }
    }
}

