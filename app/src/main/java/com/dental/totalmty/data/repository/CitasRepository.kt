package com.dental.totalmty.data.repository

import com.dental.totalmty.data.model.Cita
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CitasRepository {

    private val db       = FirebaseFirestore.getInstance()
    private val citasRef = db.collection("citas")

    // ── CREAR CITA — ID determinístico por slot para bloqueo atómico ─────
    suspend fun crearCita(cita: Cita): Result<Cita> {
        return try {
            val ocupadas = getHorasOcupadas(cita.fecha)
            val slots    = Cita.calcularSlotsOcupados(cita.hora, cita.duracionSlots)
            if (slots.any { it in ocupadas }) {
                return Result.failure(Exception("Este horario ya está ocupado. Por favor elige otro."))
            }
            val slotKey   = "${cita.fecha}_${cita.hora.replace(":", "")}"
            val docRef    = citasRef.document(slotKey)
            val citaConId = cita.copy(id = docRef.id)
            docRef.set(citaConId).await()
            Result.success(citaConId)
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            Result.failure(Exception("Ese horario acaba de ser tomado por otro paciente. Por favor elige otro."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── HORAS OCUPADAS (lectura única) — para validaciones internas ──────
    suspend fun getHorasOcupadas(fecha: String): List<String> {
        return try {
            val snap     = citasRef.whereEqualTo("fecha", fecha).get().await()
            val ocupadas = mutableListOf<String>()
            for (doc in snap.documents) {
                val estado   = doc.getString("estado") ?: continue
                if (estado != Cita.ESTADO_PENDIENTE && estado != Cita.ESTADO_CONFIRMADA) continue
                val pacienteId = doc.getString("pacienteId") ?: ""
                val hora     = doc.getString("hora")              ?: continue
                val duracion = when (pacienteId) {
                    Cita.PACIENTE_DIA_BLOQUEADO -> Cita.TODOS_LOS_SLOTS.size  // día completo
                    else -> doc.getLong("duracionSlots")?.toInt() ?: 1
                }
                ocupadas.addAll(Cita.calcularSlotsOcupados(hora, duracion))
            }
            ocupadas.distinct()
        } catch (e: Exception) { emptyList() }
    }

    // ── HORAS OCUPADAS EN TIEMPO REAL — snapshotListener ─────────────────
    fun getHorasOcupadasFlow(fecha: String): Flow<List<String>> = callbackFlow {
        val listener = citasRef.whereEqualTo("fecha", fecha)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val ocupadas = mutableListOf<String>()
                for (doc in snap.documents) {
                    val estado   = doc.getString("estado") ?: continue
                    if (estado != Cita.ESTADO_PENDIENTE && estado != Cita.ESTADO_CONFIRMADA) continue
                    val pacienteId = doc.getString("pacienteId") ?: ""
                    val hora     = doc.getString("hora")              ?: continue
                    val duracion = when (pacienteId) {
                        Cita.PACIENTE_DIA_BLOQUEADO -> Cita.TODOS_LOS_SLOTS.size
                        else -> doc.getLong("duracionSlots")?.toInt() ?: 1
                    }
                    ocupadas.addAll(Cita.calcularSlotsOcupados(hora, duracion))
                }
                trySend(ocupadas.distinct())
            }
        awaitClose { listener.remove() }
    }

    // ── CONFIRMAR con duración ────────────────────────────────────────────
    suspend fun confirmarCita(citaId: String, duracionSlots: Int = 1): Result<Unit> {
        return try {
            citasRef.document(citaId).update(mapOf(
                "estado"        to Cita.ESTADO_CONFIRMADA,
                "duracionSlots" to duracionSlots
            )).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun actualizarNotas(citaId: String, notas: String): Result<Unit> {
        return try {
            citasRef.document(citaId).update("notas", notas).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun cambiarEstado(citaId: String, nuevoEstado: String): Result<Unit> {
        return try {
            citasRef.document(citaId).update("estado", nuevoEstado).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun getCitasPaciente(pacienteId: String): Flow<List<Cita>> = callbackFlow {
        val listener = citasRef.whereEqualTo("pacienteId", pacienteId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents
                    ?.mapNotNull { it.toObject(Cita::class.java) }
                    ?.sortedWith(compareBy({ it.fecha }, { it.hora }))
                    ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getCitasProximas(pacienteId: String): Flow<List<Cita>> = callbackFlow {
        val today    = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val listener = citasRef.whereEqualTo("pacienteId", pacienteId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents
                    ?.mapNotNull { it.toObject(Cita::class.java) }
                    ?.filter { it.fecha >= today && (it.estado == Cita.ESTADO_PENDIENTE || it.estado == Cita.ESTADO_CONFIRMADA) }
                    ?.sortedWith(compareBy({ it.fecha }, { it.hora }))
                    ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getAllCitas(): Flow<List<Cita>> = callbackFlow {
        val listener = citasRef.addSnapshotListener { snap, _ ->
            trySend(snap?.documents
                ?.mapNotNull { it.toObject(Cita::class.java) }
                ?.sortedWith(compareBy({ it.fecha }, { it.hora }))
                ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    fun getCitasHoy(): Flow<List<Cita>> = callbackFlow {
        val today    = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val listener = citasRef.whereEqualTo("fecha", today)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents
                    ?.mapNotNull { it.toObject(Cita::class.java) }
                    ?.sortedBy { it.hora }
                    ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun cancelarCita(citaId: String): Result<Unit> {
        return try {
            citasRef.document(citaId).update("estado", Cita.ESTADO_CANCELADA).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun completarCita(citaId: String, notas: String): Result<Unit> {
        return try {
            citasRef.document(citaId).update(mapOf(
                "estado" to Cita.ESTADO_COMPLETADA,
                "notas"  to notas
            )).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun eliminarCita(citaId: String): Result<Unit> {
        return try { citasRef.document(citaId).delete().await(); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun reactivarCita(citaId: String): Result<Unit> {
        return try {
            citasRef.document(citaId).update("estado", Cita.ESTADO_PENDIENTE).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun bloquearSlot(fecha: String, hora: String, duracionSlots: Int, motivo: String): Result<Unit> {
        return try {
            val ocupadas = getHorasOcupadas(fecha)
            val slots    = Cita.calcularSlotsOcupados(hora, duracionSlots)
            if (slots.any { it in ocupadas }) {
                return Result.failure(Exception("Ese horario ya está ocupado"))
            }
            val slotKey = "${fecha}_${hora.replace(":", "")}"
            val docRef  = citasRef.document(slotKey)
            docRef.set(Cita(
                id             = docRef.id,
                pacienteId     = "BLOQUEADO",
                pacienteNombre = motivo.ifBlank { "Hora bloqueada" },
                fecha          = fecha,
                hora           = hora,
                servicio       = motivo.ifBlank { "Hora bloqueada" },
                estado         = Cita.ESTADO_CONFIRMADA,
                duracionSlots  = duracionSlots
            )).await()
            Result.success(Unit)
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            Result.failure(Exception("Ese horario ya está ocupado"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun desbloquearSlot(citaId: String): Result<Unit> = eliminarCita(citaId)

    fun getBloqueos(): Flow<List<Cita>> = callbackFlow {
        val listener = citasRef
            .whereEqualTo("pacienteId", "BLOQUEADO")
            .whereEqualTo("estado", Cita.ESTADO_CONFIRMADA)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents
                    ?.mapNotNull { it.toObject(Cita::class.java) }
                    ?.sortedWith(compareBy({ it.fecha }, { it.hora }))
                    ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun getCita(citaId: String): Result<Cita> {
        return try {
            val cita = citasRef.document(citaId).get().await().toObject(Cita::class.java)
                ?: throw Exception("Cita no encontrada")
            Result.success(cita)
        } catch (e: Exception) { Result.failure(e) }
    }
}
