package com.example.moneychangersdatacollectionsystem.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneychangersdatacollectionsystem.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess.asStateFlow()

    fun onEmailChanged(email: String) {
        _email.value = email
    }

    fun onPasswordChanged(password: String) {
        _password.value = password
    }

    fun login() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = authRepository.login(_email.value, _password.value)

            _isLoading.value = false
            result.onSuccess {
                _loginSuccess.value = true
            }.onFailure {
                _error.value = it.message ?: "An unknown error occurred"
            }
        }
    }
}