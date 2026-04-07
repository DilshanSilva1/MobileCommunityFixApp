package com.example.communityapp.ui.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.communityapp.databinding.FragmentProfileBinding
import com.example.communityapp.ui.auth.AuthViewModel

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val postsViewModel: PostsViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            binding.tvProfileName.text = user.displayName
            binding.tvProfileEmail.text = user.email
            binding.tvProfileRole.text = if (user.moderator) "Role: Moderator" else "Role: Member"
            binding.tvProfileId.text = "User ID: ${user.id}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
