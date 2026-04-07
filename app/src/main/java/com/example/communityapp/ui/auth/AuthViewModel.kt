package com.example.communityapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communityapp.data.model.User
import com.example.communityapp.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    init {
        if (repository.currentUser != null) {
            _authState.value = AuthState.Authenticated
            loadUserData()
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.login(email, password)
            if (result.isSuccess) {
                loadUserData()
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.register(email, password, displayName)
            if (result.isSuccess) {
                loadUserData()
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _currentUser.value = repository.getCurrentUserData()
        }
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}