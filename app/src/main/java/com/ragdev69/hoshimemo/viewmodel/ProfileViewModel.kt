package com.ragdev69.hoshimemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ragdev69.hoshimemo.data.model.UserProfile
import com.ragdev69.hoshimemo.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FirebaseRepository()

    val currentUser get() = repo.currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message

    private val _isLoggedIn = MutableStateFlow(repo.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    val profile: StateFlow<UserProfile?> = _isLoggedIn
        .flatMapLatest { loggedIn ->
            if (loggedIn) repo.getProfile()
            else flow { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
        try {
            val result = repo.createAccount(email, password)
            result.onSuccess { user ->
                val profile = UserProfile(
                    uid           = user.uid,
                    name          = name,
                    email         = email,
                    avatarInitial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "H"
                )
                repo.saveProfile(profile, explicitUid = user.uid)
                _isLoading.value = false
                _isLoggedIn.value = true
                _message.emit("Account created ✨")
            }.onFailure {
                _isLoading.value = false
                _message.emit(it.message ?: "Sign up failed")
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _message.emit(e.message ?: "Something went wrong")
        }
    }

    // Google Sign-In — called after getting idToken from Google
    fun signInWithGoogle(idToken: String) = viewModelScope.launch {
        _isLoading.value = true
        try {
            val result = repo.signInWithGoogle(idToken)
            result.onSuccess { user ->
                // Save profile if first time signing in with Google
                val profile = UserProfile(
                    uid           = user.uid,
                    name          = user.displayName ?: "",
                    email         = user.email ?: "",
                    avatarInitial = user.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "G"
                )
                repo.saveProfile(profile, explicitUid = user.uid)
                _isLoading.value = false
                _isLoggedIn.value = true
                _message.emit("Welcome! ✨")
            }.onFailure {
                _isLoading.value = false
                _message.emit(it.message ?: "Google sign in failed")
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _message.emit(e.message ?: "Something went wrong")
        }
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