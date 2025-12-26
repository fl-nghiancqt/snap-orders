package com.example.snaporder.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.snaporder.feature.auth.AuthViewModel
import com.example.snaporder.feature.cart.CartScreen
import com.example.snaporder.feature.menu.MenuScreen
import com.example.snaporder.feature.order.OrderResultScreen

/**
 * Main navigation graph for SnapOrder app.
 * 
 * ROLE-BASED NAVIGATION:
 * - Starts at AUTH screen for user selection
 * - After login, routes to appropriate flow based on role:
 *   - USER → Menu → Cart → Order → History
 *   - ADMIN → Admin Dashboard
 * 
 * This graph handles all navigation routes.
 * Navigation to user/admin flows is triggered from the Auth screen
 * after successful login validation.
 */
@Composable
fun SnapOrderNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    // Observe current user to determine if logged in
    val currentUser by authViewModel.currentUser.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = NavRoutes.AUTH
    ) {
        // Auth screen - user selection and login
        composable(NavRoutes.AUTH) {
            com.example.snaporder.feature.auth.AuthScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }
        
        // User Menu screen
        composable(NavRoutes.USER_MENU) {
            MenuScreen(
                onCartClick = {
                    navController.navigate(NavRoutes.USER_CART)
                },
                onBackClick = {
                    // Navigate back to auth (logout)
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(NavRoutes.USER_MENU) { inclusive = true }
                    }
                }
            )
        }
        
        // User Cart screen
        composable(NavRoutes.USER_CART) {
            CartScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onPlaceOrderClick = {
                    // TODO: Navigate to order result screen
                    navController.navigate(NavRoutes.USER_ORDER)
                }
            )
        }
        
        // User Order Result screen
        composable(NavRoutes.USER_ORDER) {
            OrderResultScreen(
                onBackToMenuClick = {
                    navController.navigate(NavRoutes.USER_MENU) {
                        popUpTo(NavRoutes.USER_MENU) { inclusive = false }
                    }
                },
                onViewOrderDetailClick = {
                    // TODO: Navigate to order detail screen
                    // For now, just show a toast or navigate to history
                }
            )
        }
        
        // User History screen
        composable(NavRoutes.USER_HISTORY) {
            PlaceholderScreen(title = "History", description = "View past orders")
        }
        
        // Admin flow - nested navigation handled by AdminNavGraph
        composable(NavRoutes.ADMIN_DASHBOARD) {
            AdminNavGraph(navController = navController)
        }
    }
}

/**
 * Placeholder screen for navigation testing
 */
@Composable
private fun PlaceholderScreen(title: String, description: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
