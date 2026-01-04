package com.example.snaporder.feature.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snaporder.core.model.MenuItem
import com.example.snaporder.ui.theme.SnapOrderTheme
import com.example.snaporder.ui.theme.SnapOrdersColors

/**
 * Menu Management Screen for Admin.
 * 
 * Features:
 * - Seed menu data to Firestore (one-time operation)
 * - View all menus from Firestore
 * - Toggle menu availability
 * - Refresh menu list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuManagementScreen(
    onBackClick: () -> Unit = {},
    viewModel: MenuManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val seedResult by viewModel.seedResult.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            MenuManagementTopAppBar(
                onBackClick = onBackClick,
                onRefresh = { viewModel.refresh() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SnapOrdersColors.Primary,
                contentColor = SnapOrdersColors.OnPrimary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Menu Item"
                )
            }
        },
        containerColor = SnapOrdersColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seed Section
            // SeedMenuCard(
            //     isSeeding = uiState.isSeeding,
            //     seedSuccess = uiState.seedSuccess,
            //     seedResult = seedResult,
            //     onSeedClick = { viewModel.seedMenus() }
            // )
            
            // Error message
            if (uiState.errorMessage != null) {
                ErrorCard(message = uiState.errorMessage ?: "Unknown error")
            }
            
            // Filter Input
            MenuFilterInput(
                filterQuery = uiState.filterQuery,
                onFilterChange = { viewModel.updateFilterQuery(it) }
            )
            
            // Menus List
            Text(
                text = "Menu Items (${uiState.filteredMenus.size}${if (uiState.filterQuery.isNotBlank()) " / ${uiState.menus.size}" else ""})",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = SnapOrdersColors.TextPrimary
            )
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SnapOrdersColors.Primary)
                }
            } else if (uiState.menus.isEmpty()) {
                EmptyMenusState()
            } else if (uiState.filteredMenus.isEmpty() && uiState.filterQuery.isNotBlank()) {
                EmptyFilterState(filterQuery = uiState.filterQuery)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredMenus) { menu ->
                        MenuItemRow(
                            menu = menu,
                            onToggleAvailability = { available ->
                                viewModel.updateMenuAvailability(menu.id, available)
                            },
                            onPriceChange = { newPrice ->
                                viewModel.updateMenuPrice(menu.id, newPrice)
                            },
                            onNameChange = { newName ->
                                viewModel.updateMenuName(menu.id, newName)
                            }
                        )
                    }
                }
            }
        }
        
        // Add Menu Item Dialog
        if (showAddDialog) {
            AddMenuItemDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, price, imageUrl, available ->
                    viewModel.createMenuItem(name, price, imageUrl, available)
                    showAddDialog = false
                }
            )
        }
    }
}

/**
 * Top App Bar for Menu Management Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuManagementTopAppBar(
    onBackClick: () -> Unit,
    onRefresh: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Menu Management",
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
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
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
 * Seed Menu Card - Button to seed menu data.
 */
@Composable
private fun SeedMenuCard(
    isSeeding: Boolean,
    seedSuccess: Boolean,
    seedResult: String?,
    onSeedClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seedSuccess) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                SnapOrdersColors.Surface
            }
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
            Text(
                text = "Seed Menu Data",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = SnapOrdersColors.TextPrimary
            )
            
            Text(
                text = "This will write ~60 menu items to Firestore. Use this only once to initialize the database.",
                style = MaterialTheme.typography.bodySmall,
                color = SnapOrdersColors.TextSecondary
            )
            
            if (seedResult != null) {
                Text(
                    text = seedResult,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (seedSuccess) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        SnapOrdersColors.Error
                    }
                )
            }
            
            Button(
                onClick = onSeedClick,
                enabled = !isSeeding,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SnapOrdersColors.Primary
                )
            ) {
                if (isSeeding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = SnapOrdersColors.OnPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seeding...")
                } else {
                    Text("Seed Menus to Firestore")
                }
            }
        }
    }
}

/**
 * Menu Item Row - Displays menu item with availability toggle, editable name and price.
 */
@Composable
private fun MenuItemRow(
    menu: MenuItem,
    onToggleAvailability: (Boolean) -> Unit,
    onPriceChange: (Double) -> Unit,
    onNameChange: (String) -> Unit
) {
    var showPriceDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Editable name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = menu.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SnapOrdersColors.TextPrimary,
                        modifier = Modifier.clickable { showNameDialog = true }
                    )
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Name",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { showNameDialog = true },
                        tint = SnapOrdersColors.Primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                
                // Editable price
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatPrice(menu.price.toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnapOrdersColors.TextSecondary,
                        modifier = Modifier.clickable { showPriceDialog = true }
                    )
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Price",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { showPriceDialog = true },
                        tint = SnapOrdersColors.Primary
                    )
                }
                
                Text(
                    text = "ID: ${menu.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SnapOrdersColors.TextSecondary
                )
            }
            
            // Availability toggle
            Switch(
                checked = menu.available,
                onCheckedChange = onToggleAvailability,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SnapOrdersColors.Primary,
                    checkedTrackColor = SnapOrdersColors.Primary.copy(alpha = 0.5f)
                )
            )
        }
    }
    
    // Name Edit Dialog
    if (showNameDialog) {
        NameEditDialog(
            currentName = menu.name,
            onDismiss = { showNameDialog = false },
            onConfirm = { newName ->
                onNameChange(newName)
                showNameDialog = false
            }
        )
    }
    
    // Price Edit Dialog
    if (showPriceDialog) {
        PriceEditDialog(
            currentPrice = menu.price,
            onDismiss = { showPriceDialog = false },
            onConfirm = { newPrice ->
                onPriceChange(newPrice)
                showPriceDialog = false
            }
        )
    }
}

/**
 * Dialog for editing menu item name.
 */
@Composable
private fun NameEditDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = SnapOrdersColors.Background
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Name",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.TextPrimary
                )
                
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { newValue ->
                        nameText = newValue
                        errorMessage = null
                    },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SnapOrdersColors.Primary,
                        unfocusedBorderColor = SnapOrdersColors.Outline
                    ),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let {
                        { Text(it, color = SnapOrdersColors.Error) }
                    }
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SnapOrdersColors.TextPrimary
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val trimmedName = nameText.trim()
                            if (trimmedName.isBlank()) {
                                errorMessage = "Name cannot be empty"
                            } else {
                                onConfirm(trimmedName)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SnapOrdersColors.Primary
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for editing menu item price.
 */
@Composable
private fun PriceEditDialog(
    currentPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var priceText by remember { mutableStateOf(currentPrice.toInt().toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = SnapOrdersColors.Background
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Price",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.TextPrimary
                )
                
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { newValue ->
                        // Only allow numeric input
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            priceText = newValue
                            errorMessage = null
                        }
                    },
                    label = { Text("Price (VND)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SnapOrdersColors.Primary,
                        unfocusedBorderColor = SnapOrdersColors.Outline
                    ),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let {
                        { Text(it, color = SnapOrdersColors.Error) }
                    }
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SnapOrdersColors.TextPrimary
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val price = priceText.toIntOrNull()
                            if (price == null || price < 0) {
                                errorMessage = "Please enter a valid price"
                            } else {
                                onConfirm(price.toDouble())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SnapOrdersColors.Primary
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Error Card - Shows error message.
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
 * Menu Filter Input - Search/filter menu items by name.
 */
@Composable
private fun MenuFilterInput(
    filterQuery: String,
    onFilterChange: (String) -> Unit
) {
    OutlinedTextField(
        value = filterQuery,
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
            if (filterQuery.isNotEmpty()) {
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
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SnapOrdersColors.Primary,
            unfocusedBorderColor = SnapOrdersColors.Outline,
            focusedContainerColor = SnapOrdersColors.Surface,
            unfocusedContainerColor = SnapOrdersColors.Surface
        )
    )
}

/**
 * Empty Menus State - Shows when no menus exist.
 */
@Composable
private fun EmptyMenusState() {
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
                text = "No menus found",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SnapOrdersColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Click 'Seed Menus to Firestore' to populate menu data",
                style = MaterialTheme.typography.bodyMedium,
                color = SnapOrdersColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Empty Filter State - Shows when filter returns no results.
 */
@Composable
private fun EmptyFilterState(filterQuery: String) {
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
                text = "No results found",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SnapOrdersColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "No menu items match \"$filterQuery\"",
                style = MaterialTheme.typography.bodyMedium,
                color = SnapOrdersColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Dialog for adding a new menu item.
 */
@Composable
private fun AddMenuItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Boolean) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var imageUrlText by remember { mutableStateOf("") }
    var available by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = SnapOrdersColors.Background
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add New Menu Item",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SnapOrdersColors.TextPrimary
                )
                
                // Name field
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { newValue ->
                        nameText = newValue
                        errorMessage = null
                    },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SnapOrdersColors.Primary,
                        unfocusedBorderColor = SnapOrdersColors.Outline
                    ),
                    isError = errorMessage != null && nameText.isBlank()
                )
                
                // Price field
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { newValue ->
                        // Only allow numeric input
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            priceText = newValue
                            errorMessage = null
                        }
                    },
                    label = { Text("Price (VND) *") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SnapOrdersColors.Primary,
                        unfocusedBorderColor = SnapOrdersColors.Outline
                    ),
                    isError = errorMessage != null && priceText.isBlank()
                )
                
                // Image URL field (optional)
                OutlinedTextField(
                    value = imageUrlText,
                    onValueChange = { imageUrlText = it },
                    label = { Text("Image URL (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SnapOrdersColors.Primary,
                        unfocusedBorderColor = SnapOrdersColors.Outline
                    )
                )
                
                // Availability toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnapOrdersColors.TextPrimary
                    )
                    Switch(
                        checked = available,
                        onCheckedChange = { available = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SnapOrdersColors.Primary,
                            checkedTrackColor = SnapOrdersColors.Primary.copy(alpha = 0.5f)
                        )
                    )
                }
                
                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = SnapOrdersColors.Error
                    )
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SnapOrdersColors.TextPrimary
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val trimmedName = nameText.trim()
                            val price = priceText.toIntOrNull()
                            
                            when {
                                trimmedName.isBlank() -> {
                                    errorMessage = "Name cannot be empty"
                                }
                                price == null || price < 0 -> {
                                    errorMessage = "Please enter a valid price"
                                }
                                else -> {
                                    onConfirm(trimmedName, price.toDouble(), imageUrlText.trim(), available)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SnapOrdersColors.Primary
                        )
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

/**
 * Format price in VND format.
 */
private fun formatPrice(price: Int): String {
    return "${price.toString().reversed().chunked(3).joinToString(",").reversed()} đ"
}

/**
 * Preview
 */
@Preview(showBackground = true)
@Composable
private fun MenuManagementScreenPreview() {
    SnapOrderTheme {
        MenuManagementScreen()
    }
}

