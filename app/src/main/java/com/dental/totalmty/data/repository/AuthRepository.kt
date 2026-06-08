package com.dental.totalmty.data.repository

import com.dental.totalmty.data.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser

    suspend fun loginWithEmail(email: String, password: String): Result<Usuario> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid    = result.user?.uid ?: return Result.failure(Exception("UID nulo"))
            val usuario = getUsuario(uid) ?: return Result.failure(Exception("No se encontró el perfil"))
            Result.success(usuario)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun registerWithEmail(email: String, password: String, nombre: String, telefono: String): Result<Usuario> {
        return try {
            val result  = auth.createUserWithEmailAndPassword(email, password).await()
            val uid     = result.user?.uid ?: return Result.failure(Exception("UID nulo"))
            val usuario = Usuario(uid = uid, nombre = nombre, email = email, telefono = telefono, rol = Usuario.ROL_PACIENTE)
            guardarUsuario(usuario)
            Result.success(usuario)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun loginWithGoogle(idToken: String): Result<Usuario> {
        return try {
            val credential   = GoogleAuthProvider.getCredential(idToken, null)
            val authResult   = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("No se obtuvo usuario de Firebase"))
            val uid          = firebaseUser.uid
            val existing     = getUsuario(uid)
            if (existing != null) return Result.success(existing)
            val nuevo = Usuario(
                uid         = uid,
                nombre      = firebaseUser.displayName ?: "Paciente",
                email       = firebaseUser.email ?: "",
                fotoPerfil  = firebaseUser.photoUrl?.toString() ?: "",
                telefono    = "",
                rol         = Usuario.ROL_PACIENTE
            )
            guardarUsuario(nuevo)
            Result.success(nuevo)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── NUEVO: actualizar nombre y teléfono ──────────────────────────────
    suspend fun updatePerfil(uid: String, nombre: String, telefono: String): Result<Unit> {
        return try {
            db.collection("usuarios").document(uid)
                .update(mapOf("nombre" to nombre, "telefono" to telefono))
                .await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun guardarUsuario(usuario: Usuario) {
        db.collection("usuarios").document(usuario.uid).set(usuario).await()
    }

    suspend fun getUsuario(uid: String): Usuario? {
        return try {
            val doc = db.collection("usuarios").document(uid).get().await()
            doc.toObject(Usuario::class.java)
        } catch (e: Exception) { null }
    }

    fun logout() = auth.signOut()
}
