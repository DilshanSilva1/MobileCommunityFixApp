package com.example.communityapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.communityapp.data.model.Post
import com.example.communityapp.data.model.PostStatus

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val authorId: String,
    val authorName: String,
    val upvotes: Int,
    // Store as comma-separated string — Room can't store List<String> natively
    val upvotedBy: String,
    val status: String,
    val createdAtSeconds: Long,
    val updatedAtSeconds: Long
) {
    fun toPost(): Post {
        return Post(
            id = id,
            title = title,
            description = description,
            imageUrl = imageUrl,
            location = location,
            latitude = latitude,
            longitude = longitude,
            authorId = authorId,
            authorName = authorName,
            upvotes = upvotes,
            upvotedBy = if (upvotedBy.isBlank()) emptyList() else upvotedBy.split(","),
            status = status,
            createdAt = com.google.firebase.Timestamp(createdAtSeconds, 0),
            updatedAt = com.google.firebase.Timestamp(updatedAtSeconds, 0)
        )
    }

    companion object {
        fun fromPost(post: Post): PostEntity {
            return PostEntity(
                id = post.id,
                title = post.title,
                description = post.description,
                imageUrl = post.imageUrl,
                location = post.location,
                latitude = post.latitude,
                longitude = post.longitude,
                authorId = post.authorId,
                authorName = post.authorName,
                upvotes = post.upvotes,
                upvotedBy = post.upvotedBy.joinToString(","),
                status = post.status,
                createdAtSeconds = post.createdAt.seconds,
                updatedAtSeconds = post.updatedAt.seconds
            )
        }
    }
}
