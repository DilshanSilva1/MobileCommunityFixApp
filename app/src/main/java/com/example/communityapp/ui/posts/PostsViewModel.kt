package com.example.communityapp.ui.posts

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.communityapp.data.model.Post
import com.example.communityapp.data.model.PostStatus
import com.example.communityapp.data.model.User
import com.example.communityapp.data.repository.PostRepository
import com.example.communityapp.data.repository.SortOption
import kotlinx.coroutines.launch

// AndroidViewModel gives us applicationContext safely — needed for Room and NetworkUtils
class PostsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application.applicationContext)

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _myPosts = MutableLiveData<List<Post>>()
    val myPosts: LiveData<List<Post>> = _myPosts

    private val _favoritedPosts = MutableLiveData<List<Post>>()
    val favoritedPosts: LiveData<List<Post>> = _favoritedPosts

    private val _uiState = MutableLiveData<PostsUiState>()
    val uiState: LiveData<PostsUiState> = _uiState

    private val _selectedPost = MutableLiveData<Post?>()
    val selectedPost: LiveData<Post?> = _selectedPost

    // Tracks which posts the current user has favorited (postId -> Boolean)
    private val _favoriteStates = MutableLiveData<Map<String, Boolean>>(emptyMap())
    val favoriteStates: LiveData<Map<String, Boolean>> = _favoriteStates

    var currentUser: User? = null
    var currentSortOption = SortOption.DATE_DESC
    var currentStatusFilter: PostStatus? = null

    // ─── All Posts ────────────────────────────────────────────────────────────

    fun loadPosts() {
        _uiState.value = PostsUiState.Loading
        viewModelScope.launch {
            val result = repository.getPosts(currentSortOption, currentStatusFilter)
            if (result.isSuccess) {
                val posts = result.getOrDefault(emptyList())
                _posts.value = posts
                _uiState.value = PostsUiState.Success
                // Refresh favorite states for loaded posts
                refreshFavoriteStates(posts.map { it.id })
            } else {
                _uiState.value = PostsUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load posts")
            }
        }
    }

    fun setSortOption(sortOption: SortOption) { currentSortOption = sortOption; loadPosts() }
    fun setStatusFilter(status: PostStatus?) { currentStatusFilter = status; loadPosts() }

    // ─── Create Post ──────────────────────────────────────────────────────────

    fun createPost(post: Post, imageUri: Uri?) {
        _uiState.value = PostsUiState.Loading
        viewModelScope.launch {
            val result = repository.createPost(post, imageUri)
            if (result.isSuccess) {
                _uiState.value = PostsUiState.PostCreated
                loadPosts()
            } else {
                _uiState.value = PostsUiState.Error(result.exceptionOrNull()?.message ?: "Failed to create post")
            }
        }
    }

    // ─── Upvote ───────────────────────────────────────────────────────────────

    fun toggleUpvote(postId: String) {
        val userId = currentUser?.id ?: return
        viewModelScope.launch {
            repository.toggleUpvote(postId, userId)
            loadPosts()
        }
    }

    // ─── Status (moderator) ───────────────────────────────────────────────────

    fun updatePostStatus(postId: String, status: PostStatus) {
        viewModelScope.launch {
            repository.updatePostStatus(postId, status)
            loadPosts()
        }
    }

    // ─── Post Detail ──────────────────────────────────────────────────────────

    fun loadPostById(postId: String) {
        viewModelScope.launch {
            val result = repository.getPostById(postId)
            _selectedPost.value = if (result.isSuccess) result.getOrNull() else null
        }
    }

    // ─── My Posts ─────────────────────────────────────────────────────────────

    fun loadMyPosts() {
        val authorId = currentUser?.id ?: return
        _uiState.value = PostsUiState.Loading
        viewModelScope.launch {
            val result = repository.getPostsByAuthor(authorId)
            if (result.isSuccess) {
                _myPosts.value = result.getOrDefault(emptyList())
                _uiState.value = PostsUiState.Success
            } else {
                _uiState.value = PostsUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load your posts")
            }
        }
    }

    // ─── Favorites ────────────────────────────────────────────────────────────

    fun toggleFavorite(postId: String) {
        val userId = currentUser?.id ?: return
        viewModelScope.launch {
            val result = repository.toggleFavorite(postId, userId)
            if (result.isSuccess) {
                val isFav = result.getOrDefault(false)
                val current = _favoriteStates.value?.toMutableMap() ?: mutableMapOf()
                current[postId] = isFav
                _favoriteStates.value = current
            }
        }
    }

    fun loadFavoritedPosts() {
        val userId = currentUser?.id ?: return
        _uiState.value = PostsUiState.Loading
        viewModelScope.launch {
            val result = repository.getFavoritedPosts(userId)
            if (result.isSuccess) {
                _favoritedPosts.value = result.getOrDefault(emptyList())
                _uiState.value = PostsUiState.Success
            } else {
                _uiState.value = PostsUiState.Error("Failed to load favorites")
            }
        }
    }

    private fun refreshFavoriteStates(postIds: List<String>) {
        val userId = currentUser?.id ?: return
        viewModelScope.launch {
            val states = postIds.associateWith { repository.isFavorited(it, userId) }
            _favoriteStates.value = states
        }
    }

    fun isFavorited(postId: String): Boolean {
        return _favoriteStates.value?.get(postId) == true
    }
}

sealed class PostsUiState {
    object Loading : PostsUiState()
    object Success : PostsUiState()
    object PostCreated : PostsUiState()
    data class Error(val message: String) : PostsUiState()
}