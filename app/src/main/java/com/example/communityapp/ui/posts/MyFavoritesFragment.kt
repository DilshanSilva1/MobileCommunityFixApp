package com.example.communityapp.ui.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.communityapp.R
import com.example.communityapp.databinding.FragmentMyFavoritesBinding

class MyFavoritesFragment : Fragment() {
    private var _binding: FragmentMyFavoritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostsViewModel by activityViewModels()
    private lateinit var adapter: PostsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PostsAdapter(
            onPostClick = { post ->
                val bundle = Bundle().apply { putString("postId", post.id) }
                findNavController().navigate(R.id.action_myFavoritesFragment_to_postDetailFragment, bundle)
            },
            onUpvoteClick = { post -> viewModel.toggleUpvote(post.id) },
            currentUserId = viewModel.currentUser?.id ?: ""
        )
        binding.rvFavorites.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFavorites.adapter = adapter

        viewModel.favoritedPosts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            binding.tvEmptyState.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PostsUiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is PostsUiState.Success -> binding.progressBar.visibility = View.GONE
                is PostsUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                else -> binding.progressBar.visibility = View.GONE
            }
        }

        viewModel.loadFavoritedPosts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
