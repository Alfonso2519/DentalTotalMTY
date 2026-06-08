package com.dental.totalmty.ui.paciente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dental.totalmty.R
import com.dental.totalmty.databinding.FragmentHomeBinding
import com.dental.totalmty.ui.shared.CitasAdapter
import com.dental.totalmty.utils.SessionManager
import com.dental.totalmty.viewmodel.CitasViewModel
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CitasViewModel by viewModels()
    private lateinit var adapter: CitasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        loadData()
    }

    private fun setupUI() {
        val usuario = SessionManager.usuario
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val saludo = when {
            hora < 12 -> "¡Buenos días,"
            hora < 18 -> "¡Buenas tardes,"
            else -> "¡Buenas noches,"
        }
        binding.tvGreeting.text = saludo
        binding.tvUserName.text = "${usuario?.nombre ?: "Paciente"}!"

        binding.btnNuevaCita.setOnClickListener {
            animateButton(it)
            findNavController().navigate(R.id.action_homeFragment_to_nuevaCitaFragment)
        }
        binding.cardMisCitas.setOnClickListener {
            animateButton(it)
            findNavController().navigate(R.id.nav_appointment)
        }
        binding.cardHistorial.setOnClickListener {
            animateButton(it)
            findNavController().navigate(R.id.nav_history)
        }
        binding.cardInfo.setOnClickListener {
            animateButton(it)
            findNavController().navigate(R.id.nav_info)
        }
        binding.cardPerfil.setOnClickListener {
            animateButton(it)
            findNavController().navigate(R.id.nav_profile)
        }

        binding.swipeRefresh.setColorSchemeResources(R.color.cyan_primary)
        binding.swipeRefresh.setOnRefreshListener { loadData() }
    }

    private fun setupRecyclerView() {
        adapter = CitasAdapter(onCitaClick = {}, showDoctorName = false)
        binding.rvCitasProximas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCitasProximas.adapter = adapter
    }

    private fun loadData() {
        val uid = SessionManager.usuario?.uid ?: run {
            binding.swipeRefresh.isRefreshing = false
            return
        }

        // FIX: usar observe con tag único para evitar múltiples observers al refrescar
        viewModel.getCitasProximas(uid).observe(viewLifecycleOwner) { citas ->
            if (_binding == null) return@observe  // FIX: evitar crash si el fragment ya fue destruido
            binding.swipeRefresh.isRefreshing = false

            if (citas.isEmpty()) {
                binding.tvNoCitas.visibility = View.VISIBLE
                binding.rvCitasProximas.visibility = View.GONE
                binding.tvProximaCita.text = "No tienes citas próximas"
            } else {
                binding.tvNoCitas.visibility = View.GONE
                binding.rvCitasProximas.visibility = View.VISIBLE
                adapter.submitList(citas.take(3))

                val proxima = citas.first()
                try {
                    val fechaFormat = SimpleDateFormat("dd 'de' MMMM", Locale("es")).format(
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(proxima.fecha) ?: Date()
                    )
                    binding.tvProximaCita.text = "${proxima.servicio} • $fechaFormat a las ${proxima.hora}"
                } catch (e: Exception) {
                    binding.tvProximaCita.text = "${proxima.servicio} • ${proxima.fecha} a las ${proxima.hora}"
                }
            }
        }
    }

    private fun animateButton(view: View) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
            .withEndAction { view.animate().scaleX(1f).scaleY(1f).setDuration(80).start() }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
