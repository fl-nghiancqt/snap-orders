package com.example.snaporder.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.snaporder.core.model.UserRole
import com.example.snaporder.core.navigation.NavRoutes
import com.example.snaporder.feature.auth.LoginResult
import kotlinx.coroutines.launch

/**
 * Login Screen - Luxury login form design
 * 
 * DESIGN PHILOSOPHY:
 * - Clean, minimal, luxury feel
 * - White background with soft olive green accents
 * - Premium restaurant/POS app aesthetic
 * - Smooth interactions and elegant typography
 * 
 * FLOW:
 * 1. User enters username/email and password
 * 2. Validates credentials against Firestore
 * 3. On success, navigates based on role:
 *    - USER → UserNavGraph (Menu screen)
 *    - ADMIN → AdminNavGraph (Dashboard)
 * 
 * VALIDATION:
 * - Username is required
 * - Password validation (optional for school project)
 * - User must exist in Firestore
 * - Role must be valid (USER or ADMIN)
 */
@Composable
fun AuthScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    // Form state
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Handle navigation after login
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            when (user.role) {
                UserRole.USER -> {
                    navController.navigate(NavRoutes.USER_MENU) {
                        popUpTo(NavRoutes.AUTH) { inclusive = true }
                    }
                }
                UserRole.ADMIN -> {
                    navController.navigate(NavRoutes.ADMIN_DASHBOARD) {
                        popUpTo(NavRoutes.AUTH) { inclusive = true }
                    }
                }
            }
        }
    }
    
    // Login Screen Layout
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background with smooth wave divider
        var screenHeight by remember { mutableStateOf(0f) }
        val density = LocalDensity.current
        
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    screenHeight = size.height.toFloat()
                }
        ) {
            val greenAreaHeight = screenHeight * 0.375f // ~37.5% of screen
            val waveAmplitude = with(density) { 30.dp.toPx() } // Wave height
            val waveFrequency = 1.5f // Controls wave smoothness
            val width = size.width.toFloat()
            
            // Create smooth sine-like wave path
            val path = Path().apply {
                // Start from top-left
                moveTo(0f, 0f)
                
                // Draw to start of wave
                lineTo(0f, greenAreaHeight)
                
                // Draw smooth sine-like wave using quadratic curves
                val waveSegments = 20 // More segments for smoother curve
                val segmentWidth = width / waveSegments
                
                for (i in 0..waveSegments) {
                    val x = i * segmentWidth
                    val normalizedX = x / width // 0 to 1
                    
                    // Calculate y position using sine function for smooth wave
                    val y = greenAreaHeight + waveAmplitude * kotlin.math.sin(
                        normalizedX * kotlin.math.PI.toFloat() * waveFrequency
                    )
                    
                    if (i == 0) {
                        // First point
                        lineTo(x, y)
                    } else {
                        // Use quadratic curve for smooth transitions
                        val prevX = (i - 1) * segmentWidth
                        val prevNormalizedX = prevX / width
                        val prevY = greenAreaHeight + waveAmplitude * kotlin.math.sin(
                            prevNormalizedX * kotlin.math.PI.toFloat() * waveFrequency
                        )
                        
                        // Control point for smooth curve
                        val controlX = (prevX + x) / 2f
                        val controlNormalizedX = controlX / width
                        val controlY = greenAreaHeight + waveAmplitude * kotlin.math.sin(
                            controlNormalizedX * kotlin.math.PI.toFloat() * waveFrequency
                        )
                        
                        quadraticTo(controlX, controlY, x, y)
                    }
                }
                
                // Complete the path to bottom-right and back to top-right
                lineTo(width, screenHeight)
                lineTo(width, 0f)
                close()
            }
            
            // Fill top area with primary green
            drawPath(
                path = path,
                color = Color(0xFF8FAE5D), // Primary green
                style = Fill
            )
        }
        
        // Main content - centered in white area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // Login header text (centered)
                Text(
                    text = "Snap Order",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subtitle
                Text(
                    text = "Sign in your account to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Email field
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Email",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text("name@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium, // 14.dp rounded corners
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        ),
                        enabled = !uiState.isLoading
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Password field
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Enter your password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (passwordVisible) {
                                        "Hide password"
                                    } else {
                                        "Show password"
                                    },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (username.isNotBlank()) {
                                    coroutineScope.launch {
                                        viewModel.login(username, password)
                                    }
                                }
                            }
                        ),
                        enabled = !uiState.isLoading
                    )
                }
                
                // Error message
                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Login button - centered
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        coroutineScope.launch {
                            val result = viewModel.login(username, password)
                            when (result) {
                                is LoginResult.Success -> {
                                    // Navigate based on role
                                    if (result.role == UserRole.ADMIN) {
                                        navController.navigate(NavRoutes.ADMIN_DASHBOARD) {
                                            popUpTo(NavRoutes.AUTH) { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate(NavRoutes.USER_MENU) {
                                            popUpTo(NavRoutes.AUTH) { inclusive = true }
                                        }
                                    }
                                }
                                is LoginResult.Failure -> {
                                    // Error is already shown in UI state
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = username.isNotBlank() && !uiState.isLoading,
                    shape = MaterialTheme.shapes.medium, // 14.dp rounded corners
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "LOGIN",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }