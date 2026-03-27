package com.example.moneychangersdatacollectionsystem.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneychangersdatacollectionsystem.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DashboardViewModel — Minimal ViewModel for the Dashboard screen.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /** 
     * The logged-in agent's name. 
     * Uses stateIn to properly handle lifecycle and initial state.
     */
    val agentName: StateFlow<String> = authRepository.currentUser
        .map { it?.fullName ?: "Agent" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Agent"
        )

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
