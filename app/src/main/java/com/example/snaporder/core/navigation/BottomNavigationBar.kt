package com.example.snaporder.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.snaporder.ui.theme.SnapOrdersColors

/**
 * Bottom Navigation Bar with Cart, History, Profile icons and Order button in the middle.
 * 
 * LAYOUT:
 * - Cart icon (left)
 * - History icon (left-center)
 * - Order button (center, elevated)
 * - Profile icon (right-center)
 * - (Right side reserved for future)
 */
@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    cartItemCount: Int = 0,
    onCartClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProfileClick: () -> Unit,
    onOrderClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        color = SnapOrdersColors.Background,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cart Icon
            BottomNavItem(
                icon = Icons.Filled.ShoppingCart,
                label = "Cart",
                isSelected = currentRoute == NavRoutes.USER_CART,
                badgeCount = if (cartItemCount > 0) cartItemCount else null,
                onClick = onCartClick
            )
            
            // History Icon
            BottomNavItem(
                icon = Icons.Filled.History,
                label = "History",
                isSelected = currentRoute == NavRoutes.USER_HISTORY,
                onClick = onHistoryClick
            )
            
            // Order Button (Center, Elevated)
            OrderButton(
                onClick = onOrderClick,
                modifier = Modifier
                    .size(56.dp)
                    .offset(y = (-12).dp) // Elevate above other icons
            )
            
            // Profile Icon
            BottomNavItem(
                icon = Icons.Filled.Person,
                label = "Profile",
                isSelected = currentRoute == NavRoutes.USER_PROFILE,
                onClick = onProfileClick
            )
            
            // Spacer for balance (right side)
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

/**
 * Individual bottom navigation item.
 */
@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .height(56.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) {
                        SnapOrdersColors.Primary
                    } else {
                        SnapOrdersColors.TextSecondary
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Badge for cart item count
            if (badgeCount != null && badgeCount > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-4).dp),
                    containerColor = SnapOrdersColors.Error
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = SnapOrdersColors.OnPrimary
                    )
                }
            }
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                SnapOrdersColors.Primary
            } else {
                SnapOrdersColors.TextSecondary
            },
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Order Button - Elevated button in the center of bottom navigation.
 */
@Composable
private fun OrderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape),
        containerColor = SnapOrdersColors.Primary,
        shape = CircleShape
    ) {
        Icon(
            imageVector = Icons.Filled.ShoppingBag,
            contentDescription = "Order",
            tint = SnapOrdersColors.OnPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}

