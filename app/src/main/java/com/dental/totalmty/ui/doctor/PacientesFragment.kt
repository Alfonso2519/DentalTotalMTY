package com.dental.totalmty.ui.doctor

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dental.totalmty.databinding.FragmentPacientesBinding
import com.dental.totalmty.ui.shared.CitasAdapter
import com.dental.totalmty.viewmodel.CitasViewModel

class PacientesFragment : Fragment() {
    private var _binding: FragmentPacientesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CitasViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPacientesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Lista de pacientes únicos con sus citas
        viewModel.getAllCitas().observe(viewLifecycleOwner) { citas ->
            val pacientesMap = citas.groupBy { it.pacienteId }
            // Puedes implementar un adapter propio para pacientes aquí
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
