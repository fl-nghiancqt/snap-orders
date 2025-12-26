package com.example.snaporder.core.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.snaporder.feature.menu.MenuScreen

/**
 * User navigation graph.
 * 
 * USER FLOW:
 * 1. Menu - Browse and add items to cart
 * 2. Cart - Review items, select table number, place order
 * 3. Order - View order confirmation and status
 * 4. History - View past orders
 * 
 * Users can also add more items to existing open orders from the menu.
 */
@Composable
fun UserNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.USER_MENU
    ) {
        composable(NavRoutes.USER_MENU) {
            MenuScreen(
                onCartClick = {
                    // Navigate to cart screen (will be implemented later)
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
        
        composable(NavRoutes.USER_CART) {
            PlaceholderScreen(title = "Cart", description = "Review items, select table, place order")
        }
        
        composable(NavRoutes.USER_ORDER) {
            PlaceholderScreen(title = "Order", description = "View order confirmation and status")
        }
        
        composable(NavRoutes.USER_HISTORY) {
            PlaceholderScreen(title = "History", description = "View past orders")
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

