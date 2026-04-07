package com.example.communityapp.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.communityapp.databinding.ActivityAuthBinding
import com.example.communityapp.ui.posts.MainActivity
import com.example.communityapp.utils.StatusBarUtils

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Force beige status bar on login/register screens too
        StatusBarUtils.applyBeige(this)

        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Authenticated -> navigateToMain()
                else -> {}
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.authContainer.id, LoginFragment())
                .commit()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}