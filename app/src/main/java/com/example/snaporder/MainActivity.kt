package com.example.snaporder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.snaporder.core.navigation.SnapOrderNavGraph
import com.example.snaporder.data.firebase.FirebaseConnectionTest
import com.example.snaporder.data.firebase.FirestoreMenuSeeder
import com.example.snaporder.feature.auth.AuthViewModel
import com.example.snaporder.ui.theme.SnapOrderTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for SnapOrder app.
 * Uses Jetpack Compose and Hilt for dependency injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Temporary: Test Firestore connection
        FirebaseConnectionTest.test()
        
        // WARNING: One-time seed script - Remove this call after successful execution!
        // This will write ~60 menu items to Firestore "menus" collection
        // FirestoreMenuSeeder.seedMenusToFirestore()
        
        setContent {
            SnapOrderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    SnapOrderNavGraph(
                        navController = navController,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}