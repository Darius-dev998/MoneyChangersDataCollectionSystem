package com.example.moneychangersdatacollectionsystem.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

/**
 * SplashScreen — App entry point with auth-aware navigation.
 *
 * FIXES APPLIED:
 *  #1  Auth check RESTORED via SplashViewModel. [Critical Logic Break]
 *      The previous version always called onNavigateToLogin() unconditionally,
 *      with a comment "For simplicity we'll assume navigation is handled."
 *      It was not handled. Every logged-in agent had to re-authenticate on
 *      every app launch, which is unacceptable for a field data collection
 *      system used in areas with poor connectivity.
 *
 *      SplashViewModel.checkSession() reads authRepository.currentUser
 *      (a Room agents table query). If an agent row exists from the last
 *      successful login → GoToDashboard. If not → GoToLogin.
 *
 *  #2  LoginViewModel injection REMOVED — it was injected but never used.
 *      Replaced with SplashViewModel which is the correct ViewModel for
 *      this screen's responsibility.
 *
 *  #3  Unused imports removed: AuthRepository, firstOrNull.
 *
 * NAVIGATION FLOW:
 *   App launch → SplashScreen
 *     ↓ animation (1.5s)
 *     ↓ min display delay (500ms)
 *     ↓ wait for session check (SplashViewModel)
 *     → GoToDashboard  (agent row in local DB)
 *     → GoToLogin      (no local agent session)
 */
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    // FIX #2: SplashViewModel — correct ViewModel for this screen
    viewModel: SplashViewModel = hiltViewModel()
) {
    val alpha = remember { Animatable(0f) }
    val navigationTarget by viewModel.navigationTarget.collectAsState()

    LaunchedEffect(key1 = true) {
        // Run animation and minimum display concurrently with the session check.
        // The ViewModel starts the check immediately in init{}, so by the time
        // the animation finishes, the result is usually already available.
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500)
        )
        delay(500) // Minimum display time after animation completes
    }

    // FIX #1: Navigate based on actual session state from SplashViewModel.
    // Wait until both the animation has finished AND the session check has resolved.
    LaunchedEffect(navigationTarget, alpha.value) {
        if (alpha.value >= 1f && navigationTarget != SplashViewModel.NavigationTarget.Undecided) {
            when (navigationTarget) {
                SplashViewModel.NavigationTarget.GoToDashboard -> onNavigateToDashboard()
                SplashViewModel.NavigationTarget.GoToLogin -> onNavigateToLogin()
                SplashViewModel.NavigationTarget.Undecided -> { /* still loading */ }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .alpha(alpha.value),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "MCDCS",
                color = Color.Black,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.alpha(alpha.value)
            )

            Text(
                text = "MONEY CHANGERS DATA COLLECTION",
                color = Color.Gray,
                fontSize = 16.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}