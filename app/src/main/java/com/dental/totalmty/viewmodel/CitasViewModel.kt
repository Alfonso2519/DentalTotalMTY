package com.dental.totalmty.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.dental.totalmty.data.model.Cita
import com.dental.totalmty.data.repository.CitasRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

sealed class CitaState {
    object Idle    : CitaState()
    object Loading : CitaState()
    data class Success(val message: String) : CitaState()
    data class Error(val message: String)   : CitaState()
}

class CitasViewModel : ViewModel() {

    private val repository = CitasRepository()

    private val _citaState = MutableLiveData<CitaState>(CitaState.Idle)
    val citaState: LiveData<CitaState> = _citaState

    // ── Horas ocupadas en tiempo real ─────────────────────────────────────
    // Usamos MutableStateFlow. El truco: forzamos re-emisión poniendo ""
    // antes de la nueva fecha, así flatMapLatest siempre cancela el listener anterior
    // y abre uno nuevo incluso si la fecha es la misma.
    private val _fechaFlow = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val horasOcupadas: LiveData<List<String>> = _fechaFlow
        .flatMapLatest { fecha ->
            if (fecha.isBlank()) flowOf(emptyList())
            else repository.getHorasOcupadasFlow(fecha)
        }
        .asLiveData()

    fun cargarHorasOcupadas(fecha: String) {
        // Forzar re-apertura del snapshotListener aunque sea la misma fecha
        _fechaFlow.value = ""
        _fechaFlow.value = fecha
    }

    fun getCitasPaciente(pacienteId: String) = repository.getCitasPaciente(pacienteId).asLiveData()
    fun getCitasProximas(pacienteId: String) = repository.getCitasProximas(pacienteId).asLiveData()
    fun getAllCitas()  = repository.getAllCitas().asLiveData()
    fun getCitasHoy() = repository.getCitasHoy().asLiveData()
    fun getBloqueos() = repository.getBloqueos().asLiveData()

    fun agendarCita(cita: Cita) {
        _citaState.value = CitaState.Loading
        viewModelScope.launch {
            _citaState.value = repository.crearCita(cita).fold(
                onSuccess = { CitaState.Success("¡Cita agendada correctamente!") },
                onFailure = { CitaState.Error(it.message ?: "Error al agendar cita") }
            )
        }
    }

    fun cancelarCita(citaId: String) {
        _citaState.value = CitaState.Loading
        viewModelScope.launch {
            _citaState.value = repository.cancelarCita(citaId).fold(
                onSuccess = { CitaState.Success("Cita cancelada") },
                onFailure = { CitaState.Error("Error al cancelar") }
            )
        }
    }

    fun confirmarCita(citaId: String, duracionSlots: Int = 1) {
        viewModelScope.launch { repository.confirmarCita(citaId, duracionSlots) }
    }

    fun completarCita(citaId: String, notas: String) {
        viewModelScope.launch { repository.completarCita(citaId, notas) }
    }

    fun eliminarCita(citaId: String) {
        viewModelScope.launch { repository.eliminarCita(citaId) }
    }

    fun reactivarCita(citaId: String) {
        viewModelScope.launch { repository.reactivarCita(citaId) }
    }

    fun actualizarNotas(citaId: String, notas: String) {
        _citaState.value = CitaState.Loading
        viewModelScope.launch {
            _citaState.value = repository.actualizarNotas(citaId, notas).fold(
                onSuccess = { CitaState.Success("Notas guardadas") },
                onFailure = { CitaState.Error("Error al guardar notas") }
            )
        }
    }

    fun cambiarEstado(citaId: String, nuevoEstado: String) {
        viewModelScope.launch { repository.cambiarEstado(citaId, nuevoEstado) }
    }

    fun bloquearSlot(fecha: String, hora: String, duracionSlots: Int, motivo: String) {
        _citaState.value = CitaState.Loading
        viewModelScope.launch {
            _citaState.value = repository.bloquearSlot(fecha, hora, duracionSlots, motivo).fold(
                onSuccess = { CitaState.Success("Hora bloqueada correctamente") },
                onFailure = { CitaState.Error(it.message ?: "Error al bloquear") }
            )
        }
    }

    fun desbloquearSlot(citaId: String) {
        viewModelScope.launch { repository.desbloquearSlot(citaId) }
    }

    fun resetState() { _citaState.value = CitaState.Idle }
}
