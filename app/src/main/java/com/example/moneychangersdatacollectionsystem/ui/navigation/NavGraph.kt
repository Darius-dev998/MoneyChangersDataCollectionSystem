package com.example.moneychangersdatacollectionsystem.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.moneychangersdatacollectionsystem.domain.repository.AuthRepository
import com.example.moneychangersdatacollectionsystem.ui.screens.auth.LoginScreen
import com.example.moneychangersdatacollectionsystem.ui.screens.collection.CollectionFormScreen
import com.example.moneychangersdatacollectionsystem.ui.screens.collection.CollectionViewModel
import com.example.moneychangersdatacollectionsystem.ui.screens.collection.HistoryScreen
import com.example.moneychangersdatacollectionsystem.ui.screens.collection.ManualCropScreen
import com.example.moneychangersdatacollectionsystem.ui.screens.dashboard.DashboardScreen
import com.example.moneychangersdatacollectionsystem.ui.screens.splash.SplashScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash
    ) {
        composable<Screen.Splash> {
            SplashScreen(
                authRepository = authRepository,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login) {
                        popUpTo(Screen.Splash) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard) {
                        popUpTo(Screen.Splash) { inclusive = true }
                    }
                }
            )
        }
        
        composable<Screen.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.Dashboard> {
            DashboardScreen(
                onNavigateToForm = {
                    navController.navigate(Screen.CollectionFlow)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History)
                },
                onLogout = {
                    navController.navigate(Screen.Login) {
                        popUpTo(Screen.Dashboard) { inclusive = true }
                    }
                }
            )
        }

        // --- Collection Flow Nested Graph (ViewModel Scoping) ---
        navigation<Screen.CollectionFlow>(
            startDestination = Screen.CollectionForm
        ) {
            composable<Screen.CollectionForm> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.CollectionFlow>()
                }
                val sharedViewModel: CollectionViewModel = hiltViewModel(parentEntry)
                val navigateToCropper by sharedViewModel.navigateToCropper.collectAsState()

                if (navigateToCropper) {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.ManualCrop)
                        sharedViewModel.resetCropperNavigation()
                    }
                }
                
                CollectionFormScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    viewModel = sharedViewModel
                )
            }

            composable<Screen.ManualCrop> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.CollectionFlow>()
                }
                val sharedViewModel: CollectionViewModel = hiltViewModel(parentEntry)
                
                val imageUri by sharedViewModel.currentPhotoToCrop.collectAsState()
                if (imageUri != null) {
                    ManualCropScreen(
                        imageUri = imageUri!!,
                        onCropSuccess = { croppedUri ->
                            sharedViewModel.onPhotoCropped(croppedUri)
                            navController.popBackStack()
                        },
                        onCancel = {
                            sharedViewModel.resetCropperNavigation()
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
        
        composable<Screen.History> {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
