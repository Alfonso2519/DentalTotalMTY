package com.dental.totalmty.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.dental.totalmty.data.repository.AuthRepository
import com.dental.totalmty.databinding.ActivitySplashBinding
import com.dental.totalmty.ui.auth.AuthActivity
import com.dental.totalmty.ui.doctor.DoctorActivity
import com.dental.totalmty.ui.paciente.MainActivity
import com.dental.totalmty.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        playEntranceAnimation()
        Handler(Looper.getMainLooper()).postDelayed({ checkUserSession() }, 2000)
    }

    private fun playEntranceAnimation() {
        val logoAlpha   = ObjectAnimator.ofFloat(binding.ivLogo,      "alpha",        0f, 1f).apply { duration = 700; interpolator = DecelerateInterpolator() }
        val logoScaleX  = ObjectAnimator.ofFloat(binding.ivLogo,      "scaleX",       0.5f, 1f).apply { duration = 700; interpolator = DecelerateInterpolator() }
        val logoScaleY  = ObjectAnimator.ofFloat(binding.ivLogo,      "scaleY",       0.5f, 1f).apply { duration = 700; interpolator = DecelerateInterpolator() }
        val nameAlpha   = ObjectAnimator.ofFloat(binding.tvBrandName,  "alpha",        0f, 1f).apply { duration = 600; startDelay = 500 }
        val nameY       = ObjectAnimator.ofFloat(binding.tvBrandName,  "translationY", 30f, 0f).apply { duration = 600; startDelay = 500 }
        val tagAlpha    = ObjectAnimator.ofFloat(binding.tvTagline,    "alpha",        0f, 1f).apply { duration = 600; startDelay = 700 }
        val progAlpha   = ObjectAnimator.ofFloat(binding.progressBar,  "alpha",        0f, 1f).apply { duration = 400; startDelay = 1200 }
        AnimatorSet().apply { playTogether(logoAlpha, logoScaleX, logoScaleY, nameAlpha, nameY, tagAlpha, progAlpha); start() }
    }

    private fun checkUserSession() {
        val currentUser = authRepository.currentUser
        if (currentUser == null) { goToAuth(); return }

        CoroutineScope(Dispatchers.IO).launch {
            val usuario = authRepository.getUsuario(currentUser.uid)
            withContext(Dispatchers.Main) {
                if (usuario == null) {
                    goToAuth()
                } else {
                    SessionManager.usuario = usuario
                    if (usuario.rol == "doctor") goToDoctor() else goToMain()
                }
            }
        }
    }

    private fun goToAuth()   { go(AuthActivity::class.java) }
    private fun goToMain()   { go(MainActivity::class.java) }
    private fun goToDoctor() { go(DoctorActivity::class.java) }

    private fun <T> go(cls: Class<T>) {
        startActivity(Intent(this, cls).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
