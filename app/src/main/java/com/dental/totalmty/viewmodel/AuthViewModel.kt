package com.dental.totalmty.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dental.totalmty.data.model.Usuario
import com.dental.totalmty.data.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    data class Success(val usuario: Usuario) : AuthState()
    data class Error(val message: String)    : AuthState()
    object Idle : AuthState()
}

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    private val _passwordResetState = MutableLiveData<AuthState>(AuthState.Idle)
    val passwordResetState: LiveData<AuthState> = _passwordResetState

    private val _perfilState = MutableLiveData<AuthState>(AuthState.Idle)
    val perfilState: LiveData<AuthState> = _perfilState

    val isLoggedIn get() = repository.currentUser != null

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Por favor completa todos los campos"); return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.loginWithEmail(email, password)
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error("Correo o contraseña incorrectos") }
            )
        }
    }

    fun register(email: String, password: String, nombre: String, telefono: String) {
        if (email.isBlank() || password.isBlank() || nombre.isBlank() || telefono.isBlank()) {
            _authState.value = AuthState.Error("Por favor completa todos los campos"); return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("La contraseña debe tener al menos 6 caracteres"); return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.registerWithEmail(email, password, nombre, telefono)
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Error al crear cuenta") }
            )
        }
    }

    fun loginWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.loginWithGoogle(idToken)
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error("Error con Google Sign-In: ${it.message}") }
            )
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _passwordResetState.value = AuthState.Error("Ingresa tu correo electrónico"); return
        }
        _passwordResetState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.sendPasswordReset(email)
            _passwordResetState.value = result.fold(
                onSuccess = { AuthState.Success(Usuario()) },
                onFailure = { AuthState.Error("Error al enviar correo. Verifica el email.") }
            )
        }
    }

    // ── NUEVO: guardar nombre y teléfono en Firestore + SessionManager ──
    fun updatePerfil(uid: String, nombre: String, telefono: String) {
        if (nombre.isBlank()) {
            _perfilState.value = AuthState.Error("El nombre no puede estar vacío"); return
        }
        _perfilState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.updatePerfil(uid, nombre.trim(), telefono.trim())
            _perfilState.value = result.fold(
                onSuccess = { AuthState.Success(Usuario()) },
                onFailure = { AuthState.Error("Error al guardar cambios: ${it.message}") }
            )
        }
    }

    fun checkCurrentUser(onResult: (Usuario?) -> Unit) {
        val user = repository.currentUser
        if (user == null) { onResult(null); return }
        viewModelScope.launch {
            val usuario = repository.getUsuario(user.uid)
            onResult(usuario)
        }
    }

    fun logout() = repository.logout()

    fun resetState()       { _authState.value      = AuthState.Idle }
    fun resetPerfilState() { _perfilState.value     = AuthState.Idle }
}
