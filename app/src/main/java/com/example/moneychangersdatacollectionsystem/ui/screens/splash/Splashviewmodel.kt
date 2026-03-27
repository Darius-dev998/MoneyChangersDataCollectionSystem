package com.example.moneychangersdatacollectionsystem.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneychangersdatacollectionsystem.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SplashViewModel — Resolves initial navigation target at app launch.
 *
 * WHY THIS EXISTS:
 * The original SplashScreen injected LoginViewModel but never used it,
 * and always called onNavigateToLogin() unconditionally. Logged-in agents
 * had to re-enter credentials on every single app launch.
 *
 * This ViewModel checks whether a valid local agent session exists
 * (a row in the Room agents table, put there on last successful login).
 * Navigation is then determined by the result:
 *   — Agent row present  → navigate to Dashboard (skip login)
 *   — No agent row       → navigate to Login
 *
 * The check runs on IO dispatcher to avoid blocking the main thread.
 * The result is only emitted once — the LaunchedEffect in SplashScreen
 * reads it and navigates immediately after the splash animation completes.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    sealed class NavigationTarget {
        object Undecided : NavigationTarget()
        object GoToLogin : NavigationTarget()
        object GoToDashboard : NavigationTarget()
    }

    private val _navigationTarget = MutableStateFlow<NavigationTarget>(NavigationTarget.Undecided)
    val navigationTarget: StateFlow<NavigationTarget> = _navigationTarget.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            // firstOrNull() collects one emission from the Flow and cancels.
            // If a local agent row exists (inserted on last successful login),
            // navigate to Dashboard. Otherwise navigate to Login.
            val user = authRepository.currentUser.firstOrNull()
            _navigationTarget.value = if (user != null) {
                NavigationTarget.GoToDashboard
            } else {
                NavigationTarget.GoToLogin
            }
        }
    }
}