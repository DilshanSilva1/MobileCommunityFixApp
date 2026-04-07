package com.example.communityapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.communityapp.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val displayName = binding.etDisplayName.text.toString().trim()
            if (validate(email, password, displayName)) {
                viewModel.register(email, password, displayName)
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.btnRegister.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is AuthState.Error -> {
                    binding.btnRegister.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.btnRegister.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun validate(email: String, password: String, displayName: String): Boolean {
        if (displayName.isBlank()) { binding.etDisplayName.error = "Required"; return false }
        if (email.isBlank()) { binding.etEmail.error = "Required"; return false }
        if (password.length < 6) { binding.etPassword.error = "Min 6 characters"; return false }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
