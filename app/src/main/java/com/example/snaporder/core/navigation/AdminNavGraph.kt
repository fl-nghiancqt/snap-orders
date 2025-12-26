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

/**
 * Admin navigation graph.
 * 
 * ADMIN FLOW:
 * - Dashboard: View all orders from all tables
 * - Manage order statuses (CREATED → PREPARING → PAID)
 * - View order details and items
 * - Monitor all active orders
 * 
 * Admins have full access to all orders and can update their statuses.
 */
@Composable
fun AdminNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.ADMIN_DASHBOARD
    ) {
        composable(NavRoutes.ADMIN_DASHBOARD) {
            PlaceholderScreen(title = "Admin Dashboard", description = "View all orders and manage statuses")
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

