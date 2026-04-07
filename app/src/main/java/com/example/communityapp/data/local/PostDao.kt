package com.example.communityapp.data.local

import androidx.room.*

@Dao
interface PostDao {

    // --- Posts ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Query("SELECT * FROM posts ORDER BY createdAtSeconds DESC")
    suspend fun getAllPosts(): List<PostEntity>

    @Query("SELECT * FROM posts WHERE authorId = :authorId ORDER BY createdAtSeconds DESC")
    suspend fun getPostsByAuthor(authorId: String): List<PostEntity>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): PostEntity?

    @Query("UPDATE posts SET upvotes = :upvotes, upvotedBy = :upvotedBy WHERE id = :postId")
    suspend fun updateUpvotes(postId: String, upvotes: Int, upvotedBy: String)

    @Query("UPDATE posts SET status = :status WHERE id = :postId")
    suspend fun updateStatus(postId: String, status: String)

    @Query("DELETE FROM posts")
    suspend fun clearAll()

    // --- Favorites ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun removeFavorite(favorite: FavoriteEntity)

    @Query("SELECT * FROM favorites WHERE userId = :userId")
    suspend fun getFavoritesForUser(userId: String): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND postId = :postId)")
    suspend fun isFavorited(userId: String, postId: String): Boolean

    // Get full post data for all favorited posts of a user
    @Query("""
        SELECT p.* FROM posts p
        INNER JOIN favorites f ON p.id = f.postId
        WHERE f.userId = :userId
        ORDER BY f.savedAtSeconds DESC
    """)
    suspend fun getFavoritedPosts(userId: String): List<PostEntity>
}
