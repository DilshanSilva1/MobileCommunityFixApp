package com.example.communityapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.communityapp.R
import com.example.communityapp.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validate(email, password)) {
                viewModel.login(email, password)
            }
        }

        binding.tvGoToRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.auth_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.btnLogin.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is AuthState.Error -> {
                    binding.btnLogin.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.btnLogin.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun validate(email: String, password: String): Boolean {
        if (email.isBlank()) { binding.etEmail.error = "Required"; return false }
        if (password.isBlank()) { binding.etPassword.error = "Required"; return false }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}