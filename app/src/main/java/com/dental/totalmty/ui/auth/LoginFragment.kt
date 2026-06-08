package com.dental.totalmty.ui.auth

import android.animation.ObjectAnimator
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dental.totalmty.R
import com.dental.totalmty.databinding.FragmentLoginBinding
import com.dental.totalmty.viewmodel.AuthState
import com.dental.totalmty.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private var googleSignInClient: GoogleSignInClient? = null

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn
                    .getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                val token = account.idToken
                if (token != null) {
                    viewModel.loginWithGoogle(token)
                } else {
                    showError("No se obtuvo token de Google")
                    viewModel.resetState()
                }
            } catch (e: ApiException) {
                showError("Error Google Sign-In (código ${e.statusCode})")
                viewModel.resetState()
            }
        } else {
            viewModel.resetState()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar GoogleSignInClient UNA sola vez
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        setupClickListeners()
        observeAuthState()
        playEntranceAnimation()
    }

    private fun playEntranceAnimation() {
        ObjectAnimator.ofFloat(binding.ivLogo, "translationY", -40f, 0f).apply {
            duration = 800; interpolator = DecelerateInterpolator(); start()
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            animatePress(it)
            val email    = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            viewModel.login(email, password)
        }

        binding.btnGoogle.setOnClickListener {
            animatePress(it)
            launchGoogleSignIn()
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFragment_to_registerFragment, null,
                androidx.navigation.NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .build()
            )
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            if (_binding == null) return@observe
            when (state) {
                is AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled  = false
                    binding.btnGoogle.isEnabled = false
                    binding.tvError.visibility  = View.GONE
                }
                is AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    setButtonsEnabled(true)
                    (activity as? AuthActivity)?.onLoginSuccess(state.usuario)
                }
                is AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    setButtonsEnabled(true)
                    showError(state.message)
                    viewModel.resetState()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    setButtonsEnabled(true)
                }
            }
        }
    }

    private fun launchGoogleSignIn() {
        // Sin revokeAccess — igual que MyDrugs que funciona con todas las cuentas
        val client = googleSignInClient ?: return
        googleSignInLauncher.launch(client.signInIntent)
    }

    private fun showError(msg: String) {
        if (_binding == null) return
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(binding.tvError, "translationX", 0f, 10f, -10f, 6f, -6f, 0f)
            .apply { duration = 400; start() }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnLogin.isEnabled  = enabled
        binding.btnGoogle.isEnabled = enabled
    }

    private fun animatePress(v: View) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
            .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(80).start() }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
