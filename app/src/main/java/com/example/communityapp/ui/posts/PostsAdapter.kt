package com.example.communityapp.ui.posts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.communityapp.R
import com.example.communityapp.data.model.Post
import com.example.communityapp.data.model.PostStatus
import com.example.communityapp.databinding.ItemPostBinding
import java.text.SimpleDateFormat
import java.util.Locale

class PostsAdapter(
    private val onPostClick: (Post) -> Unit,
    private val onUpvoteClick: (Post) -> Unit,
    private val onFavoriteClick: ((Post) -> Unit)? = null,
    private var currentUserId: String,
    private var favoriteIds: Set<String> = emptySet()
) : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    fun updateFavorites(ids: Set<String>) {
        favoriteIds = ids
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(post: Post) {
            binding.tvTitle.text = post.title
            binding.tvDescription.text = post.description
            binding.tvLocation.text = post.location.ifBlank { "No location" }
            binding.tvAuthor.text = post.authorName
            binding.tvDate.text = dateFormat.format(post.createdAt.toDate())
            binding.tvUpvotes.text = post.upvotes.toString()

            // Upvote tint
            val hasUpvoted = post.upvotedBy.contains(currentUserId)
            binding.btnUpvote.setIconTintResource(
                if (hasUpvoted) R.color.upvote_active else R.color.upvote_inactive
            )

            // Favorite icon
            val isFavorited = favoriteIds.contains(post.id)
            binding.btnFavorite.setImageResource(
                if (isFavorited) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )

            // Status chip
            binding.chipStatus.text = post.postStatus.displayName
            val (bgColor, textColor) = when (post.postStatus) {
                PostStatus.PENDING -> Pair(R.color.status_pending_bg, R.color.status_pending_text)
                PostStatus.RESOLVED -> Pair(R.color.status_resolved_bg, R.color.status_resolved_text)
                PostStatus.UNRESOLVED -> Pair(R.color.status_unresolved_bg, R.color.status_unresolved_text)
            }
            binding.chipStatus.setChipBackgroundColorResource(bgColor)
            binding.chipStatus.setTextColor(ContextCompat.getColor(binding.root.context, textColor))

            if (post.imageUrl.isNotBlank()) {
                Glide.with(binding.root.context)
                    .load(post.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(binding.ivPostImage)
                binding.ivPostImage.visibility = android.view.View.VISIBLE
            } else {
                binding.ivPostImage.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onPostClick(post) }
            binding.btnUpvote.setOnClickListener { onUpvoteClick(post) }
            binding.btnFavorite.setOnClickListener { onFavoriteClick?.invoke(post) }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}