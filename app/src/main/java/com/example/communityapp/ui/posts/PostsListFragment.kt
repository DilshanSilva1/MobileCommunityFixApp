package com.example.communityapp.ui.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.communityapp.R
import com.example.communityapp.data.model.PostStatus
import com.example.communityapp.databinding.FragmentPostsListBinding
import com.example.communityapp.data.repository.SortOption

class PostsListFragment : Fragment() {
    private var _binding: FragmentPostsListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostsViewModel by activityViewModels()
    private lateinit var adapter: PostsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSortSpinner()
        setupFilterChips()
        observeViewModel()

        binding.fabNewPost.setOnClickListener {
            findNavController().navigate(R.id.action_postsListFragment_to_createPostFragment)
        }

        viewModel.loadPosts()
    }

    private fun setupRecyclerView() {
        adapter = PostsAdapter(
            onPostClick = { post ->
                val bundle = Bundle().apply { putString("postId", post.id) }
                findNavController().navigate(R.id.action_postsListFragment_to_postDetailFragment, bundle)
            },
            onUpvoteClick = { post -> viewModel.toggleUpvote(post.id) },
            onFavoriteClick = { post -> viewModel.toggleFavorite(post.id) },
            currentUserId = viewModel.currentUser?.id ?: ""
        )
        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = adapter
    }

    private fun setupSortSpinner() {
        val sortOptions = SortOption.values().map { it.displayName }
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = spinnerAdapter
        binding.spinnerSort.setSelection(0)

        binding.spinnerSort.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setSortOption(SortOption.values()[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener { viewModel.setStatusFilter(null) }
        binding.chipPending.setOnClickListener { viewModel.setStatusFilter(PostStatus.PENDING) }
        binding.chipResolved.setOnClickListener { viewModel.setStatusFilter(PostStatus.RESOLVED) }
        binding.chipUnresolved.setOnClickListener { viewModel.setStatusFilter(PostStatus.UNRESOLVED) }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            binding.tvEmptyState.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.favoriteStates.observe(viewLifecycleOwner) { states ->
            adapter.updateFavorites(states.filterValues { it }.keys)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}