package com.example.snaporder.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.snaporder.feature.admin.MenuManagementScreen
import com.example.snaporder.feature.auth.AuthViewModel
import com.example.snaporder.feature.cart.CartScreen
import com.example.snaporder.feature.history.OrderHistoryScreen
import com.example.snaporder.feature.menu.MenuScreen
import com.example.snaporder.feature.order.OrderDetailScreen
import com.example.snaporder.feature.order.OrderResultScreen
import com.example.snaporder.ui.theme.SnapOrdersColors

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
                onHistoryClick = {
                    navController.navigate(NavRoutes.USER_HISTORY)
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
                    // Navigate to order detail screen
                    // Note: In a real app, we'd pass the orderId from the order result
                    // For now, navigate to history where user can select an order
                    navController.navigate(NavRoutes.USER_HISTORY)
                }
            )
        }
        
        // User History screen
        composable(NavRoutes.USER_HISTORY) {
            OrderHistoryScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onOrderClick = { orderId ->
                    navController.navigate(NavRoutes.userOrderDetail(orderId))
                }
            )
        }
        
        // User Order Detail screen
        composable(
            route = NavRoutes.USER_ORDER_DETAIL,
            arguments = listOf(
                navArgument("orderId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailScreen(
                orderId = orderId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Admin Dashboard screen
        composable(NavRoutes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                onMenuManagementClick = {
                    navController.navigate(NavRoutes.ADMIN_MENU_MANAGEMENT)
                }
            )
        }
        
        // Admin Menu Management screen
        composable(NavRoutes.ADMIN_MENU_MANAGEMENT) {
            MenuManagementScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Admin Dashboard Screen - Main admin screen with navigation options.
 */
@Composable
private fun AdminDashboardScreen(
    onMenuManagementClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SnapOrdersColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Admin Dashboard",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = SnapOrdersColors.TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Menu Management Button
            Button(
                onClick = onMenuManagementClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SnapOrdersColors.Primary
                )
            ) {
                Text(
                    text = "Menu Management",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.OnPrimary
                )
            }
        }
    }
}
