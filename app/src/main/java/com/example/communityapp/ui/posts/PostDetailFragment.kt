package com.example.communityapp.ui.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.communityapp.data.model.PostStatus
import com.example.communityapp.databinding.FragmentPostDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale

class PostDetailFragment : Fragment() {
    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostsViewModel by activityViewModels()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = requireArguments().getString("postId") ?: return
        viewModel.loadPostById(postId)

        viewModel.selectedPost.observe(viewLifecycleOwner) { post ->
            post ?: return@observe
            binding.tvTitle.text = post.title
            binding.tvDescription.text = post.description
            binding.tvLocation.text = if (post.location.isBlank()) "No location specified" else post.location
            binding.tvAuthor.text = "Posted by: ${post.authorName}"
            binding.tvDate.text = dateFormat.format(post.createdAt.toDate())
            binding.tvUpvotes.text = "${post.upvotes} upvotes"
            binding.tvStatus.text = "Status: ${post.postStatus.displayName}"

            val hasUpvoted = post.upvotedBy.contains(viewModel.currentUser?.id ?: "")
            binding.btnUpvote.text = if (hasUpvoted) "Remove Upvote" else "Upvote"

            if (post.imageUrl.isNotBlank()) {
                Glide.with(this).load(post.imageUrl).centerCrop().into(binding.ivPostImage)
                binding.ivPostImage.visibility = View.VISIBLE
            } else {
                binding.ivPostImage.visibility = View.GONE
            }

            // Show moderator controls only for moderator users
            val isModerator = viewModel.currentUser?.moderator == true
            binding.moderatorGroup.visibility = if (isModerator) View.VISIBLE else View.GONE

            binding.btnSetPending.setOnClickListener {
                viewModel.updatePostStatus(post.id, PostStatus.PENDING)
                Toast.makeText(requireContext(), "Status set to Pending", Toast.LENGTH_SHORT).show()
            }
            binding.btnSetResolved.setOnClickListener {
                viewModel.updatePostStatus(post.id, PostStatus.RESOLVED)
                Toast.makeText(requireContext(), "Status set to Resolved", Toast.LENGTH_SHORT).show()
            }
            binding.btnSetUnresolved.setOnClickListener {
                viewModel.updatePostStatus(post.id, PostStatus.UNRESOLVED)
                Toast.makeText(requireContext(), "Status set to Unresolved", Toast.LENGTH_SHORT).show()
            }

            binding.btnUpvote.setOnClickListener {
                viewModel.toggleUpvote(post.id)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}