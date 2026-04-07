package com.example.communityapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.communityapp.data.local.AppDatabase
import com.example.communityapp.data.local.FavoriteEntity
import com.example.communityapp.data.local.PostEntity
import com.example.communityapp.data.model.Post
import com.example.communityapp.data.model.PostStatus
import com.example.communityapp.utils.NetworkUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class PostRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val postsCollection = db.collection("posts")
    private val dao = AppDatabase.getInstance(context).postDao()

    // ─── Create Post ─────────────────────────────────────────────────────────

    suspend fun createPost(post: Post, imageUri: Uri?): Result<String> {
        return try {
            // post.imageUrl already holds the local file path set in CreatePostFragment.
            // Strip it for Firestore (other devices can't read this device's internal storage)
            // but keep it in Room so images display locally on this device.
            val firestorePost = post.copy(imageUrl = "")

            if (NetworkUtils.isOnline(context)) {
                val docRef = postsCollection.add(firestorePost).await()
                // Save to Room with the LOCAL path so images show on this device
                val savedPost = post.copy(id = docRef.id)
                dao.insertPost(PostEntity.fromPost(savedPost))
                Result.success(docRef.id)
            } else {
                val tempId = "local_${System.currentTimeMillis()}"
                val localPost = post.copy(id = tempId)
                dao.insertPost(PostEntity.fromPost(localPost))
                Result.success(tempId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Get Posts (online = Firestore + cache, offline = Room) ──────────────

    suspend fun getPosts(
        sortBy: SortOption = SortOption.DATE_DESC,
        statusFilter: PostStatus? = null
    ): Result<List<Post>> {
        return try {
            if (NetworkUtils.isOnline(context)) {
                val snapshot = if (statusFilter != null) {
                    postsCollection.whereEqualTo("status", statusFilter.name).get().await()
                } else {
                    val field = when (sortBy) {
                        SortOption.DATE_DESC, SortOption.DATE_ASC -> "createdAt"
                        SortOption.UPVOTES_DESC, SortOption.UPVOTES_ASC -> "upvotes"
                    }
                    val order = when (sortBy) {
                        SortOption.DATE_DESC, SortOption.UPVOTES_DESC -> Query.Direction.DESCENDING
                        SortOption.DATE_ASC, SortOption.UPVOTES_ASC -> Query.Direction.ASCENDING
                    }
                    postsCollection.orderBy(field, order).get().await()
                }
                var posts = snapshot.toObjects(Post::class.java)
                if (statusFilter != null) {
                    posts = when (sortBy) {
                        SortOption.DATE_DESC -> posts.sortedByDescending { it.createdAt.seconds }
                        SortOption.DATE_ASC -> posts.sortedBy { it.createdAt.seconds }
                        SortOption.UPVOTES_DESC -> posts.sortedByDescending { it.upvotes }
                        SortOption.UPVOTES_ASC -> posts.sortedBy { it.upvotes }
                    }
                }
                dao.insertPosts(posts.map { PostEntity.fromPost(it) })
                Result.success(posts)
            } else {
                val cached = dao.getAllPosts().map { it.toPost() }
                val filtered = if (statusFilter != null) cached.filter { it.status == statusFilter.name } else cached
                val sorted = when (sortBy) {
                    SortOption.DATE_DESC -> filtered.sortedByDescending { it.createdAt.seconds }
                    SortOption.DATE_ASC -> filtered.sortedBy { it.createdAt.seconds }
                    SortOption.UPVOTES_DESC -> filtered.sortedByDescending { it.upvotes }
                    SortOption.UPVOTES_ASC -> filtered.sortedBy { it.upvotes }
                }
                Result.success(sorted)
            }
        } catch (e: Exception) {
            return try { Result.success(dao.getAllPosts().map { it.toPost() }) }
            catch (r: Exception) { Result.failure(e) }
        }
    }

    // ─── My Posts ────────────────────────────────────────────────────────────

    suspend fun getPostsByAuthor(authorId: String): Result<List<Post>> {
        return try {
            if (NetworkUtils.isOnline(context)) {
                val snapshot = postsCollection
                    .whereEqualTo("authorId", authorId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().await()
                val posts = snapshot.toObjects(Post::class.java)
                dao.insertPosts(posts.map { PostEntity.fromPost(it) })
                Result.success(posts)
            } else {
                Result.success(dao.getPostsByAuthor(authorId).map { it.toPost() })
            }
        } catch (e: Exception) {
            return try { Result.success(dao.getPostsByAuthor(authorId).map { it.toPost() }) }
            catch (r: Exception) { Result.failure(e) }
        }
    }

    // ─── Update Status ───────────────────────────────────────────────────────

    suspend fun updatePostStatus(postId: String, status: PostStatus): Result<Unit> {
        return try {
            if (NetworkUtils.isOnline(context)) {
                postsCollection.document(postId).update(
                    mapOf("status" to status.name, "updatedAt" to Timestamp.now())
                ).await()
            }
            dao.updateStatus(postId, status.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Toggle Upvote ───────────────────────────────────────────────────────

    suspend fun toggleUpvote(postId: String, userId: String): Result<Unit> {
        return try {
            if (NetworkUtils.isOnline(context)) {
                val postRef = postsCollection.document(postId)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val post = snapshot.toObject(Post::class.java)
                        ?: throw IllegalStateException("Post not found")
                    val upvotedBy = post.upvotedBy.toMutableList()
                    val newUpvotes: Int
                    if (upvotedBy.contains(userId)) {
                        upvotedBy.remove(userId)
                        newUpvotes = maxOf(0, post.upvotes - 1)
                    } else {
                        upvotedBy.add(userId)
                        newUpvotes = post.upvotes + 1
                    }
                    transaction.update(postRef, "upvotedBy", upvotedBy)
                    transaction.update(postRef, "upvotes", newUpvotes)
                }.await()
                val updated = postsCollection.document(postId).get().await().toObject(Post::class.java)
                updated?.let { dao.insertPost(PostEntity.fromPost(it)) }
            } else {
                val local = dao.getPostById(postId) ?: return Result.failure(Exception("Post not found locally"))
                val list = if (local.upvotedBy.isBlank()) mutableListOf() else local.upvotedBy.split(",").toMutableList()
                val newUpvotes: Int
                if (list.contains(userId)) { list.remove(userId); newUpvotes = maxOf(0, local.upvotes - 1) }
                else { list.add(userId); newUpvotes = local.upvotes + 1 }
                dao.updateUpvotes(postId, newUpvotes, list.joinToString(","))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Get Single Post ─────────────────────────────────────────────────────

    suspend fun getPostById(postId: String): Result<Post> {
        return try {
            if (NetworkUtils.isOnline(context)) {
                val snapshot = postsCollection.document(postId).get().await()
                val post = snapshot.toObject(Post::class.java) ?: return Result.failure(Exception("Post not found"))
                dao.insertPost(PostEntity.fromPost(post))
                Result.success(post)
            } else {
                val local = dao.getPostById(postId) ?: return Result.failure(Exception("Post not available offline"))
                Result.success(local.toPost())
            }
        } catch (e: Exception) {
            return try {
                val local = dao.getPostById(postId) ?: return Result.failure(e)
                Result.success(local.toPost())
            } catch (r: Exception) { Result.failure(e) }
        }
    }

    // ─── Favorites (Room-only — local per device) ────────────────────────────

    suspend fun toggleFavorite(postId: String, userId: String): Result<Boolean> {
        return try {
            val already = dao.isFavorited(userId, postId)
            if (already) {
                dao.removeFavorite(FavoriteEntity(userId, postId))
                Result.success(false)
            } else {
                dao.addFavorite(FavoriteEntity(userId, postId))
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFavorited(postId: String, userId: String): Boolean {
        return try { dao.isFavorited(userId, postId) } catch (e: Exception) { false }
    }

    suspend fun getFavoritedPosts(userId: String): Result<List<Post>> {
        return try {
            Result.success(dao.getFavoritedPosts(userId).map { it.toPost() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

enum class SortOption(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    UPVOTES_DESC("Most Upvotes"),
    UPVOTES_ASC("Least Upvotes")
}