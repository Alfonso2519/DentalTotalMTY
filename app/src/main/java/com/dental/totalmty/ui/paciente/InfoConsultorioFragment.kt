package com.dental.totalmty.ui.paciente

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.dental.totalmty.databinding.FragmentInfoConsultorioBinding

class InfoConsultorioFragment : Fragment() {
    private var _binding: FragmentInfoConsultorioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInfoConsultorioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnLlamar.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+528112345678")))
        }
        binding.btnWhatsapp.setOnClickListener {
            val url = "https://wa.me/528112345678?text=Hola, me gustaría agendar una cita"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
