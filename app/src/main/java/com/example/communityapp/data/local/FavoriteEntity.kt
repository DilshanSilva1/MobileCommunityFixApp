package com.example.communityapp.data.local

import androidx.room.Entity

// Composite primary key: one user can favorite many posts, one post can be favorited by many users
@Entity(tableName = "favorites", primaryKeys = ["userId", "postId"])
data class FavoriteEntity(
    val userId: String,
    val postId: String,
    val savedAtSeconds: Long = System.currentTimeMillis() / 1000
)
