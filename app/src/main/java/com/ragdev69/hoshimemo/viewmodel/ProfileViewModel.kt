package com.ragdev69.hoshimemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ragdev69.hoshimemo.data.model.UserProfile
import com.ragdev69.hoshimemo.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FirebaseRepository()

    val currentUser get() = repo.currentUser

    val profile: StateFlow<UserProfile?> = repo.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message

    private val _isLoggedIn = MutableStateFlow(repo.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun signIn(email: String, password: String) = viewModelScope.launch {
        _isLoading.value = true
        val result = repo.signInWithEmail(email, password)
        result.onSuccess {
            _isLoggedIn.value = true
            _message.emit("Welcome back! ✨")
        }.onFailure {
            _message.emit(it.message ?: "Sign in failed")
        }
        _isLoading.value = false
    }

    fun createAccount(email: String, password: String, name: String) = viewModelScope.launch {
        _isLoading.value = true
        val result = repo.createAccount(email, password)
        result.onSuccess { user ->
            _isLoggedIn.value = true
            saveProfile(UserProfile(
                uid   = user.uid,
                name  = name,
                email = email,
                avatarInitial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "H"
            ))
            _message.emit("Account created ✨")
        }.onFailure {
            _message.emit(it.message ?: "Sign up failed")
        }
        _isLoading.value = false
    }

    fun signOut() = viewModelScope.launch {
        repo.signOut()
        _isLoggedIn.value = false
        _message.emit("Signed out")
    }

    fun saveProfile(profile: UserProfile) = viewModelScope.launch {
        _isLoading.value = true
        try {
            repo.saveProfile(profile)
            _message.emit("Profile saved ✓")
        } catch (e: Exception) {
            _message.emit(e.message ?: "Failed to save profile")
        } finally {
            _isLoading.value = false
        }
    }
}