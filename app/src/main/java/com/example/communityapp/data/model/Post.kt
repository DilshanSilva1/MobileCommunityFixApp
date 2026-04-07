package com.example.communityapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Post(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val authorId: String = "",
    val authorName: String = "",
    val upvotes: Int = 0,
    val upvotedBy: List<String> = emptyList(),
    // Stored as a plain String in Firestore to avoid enum deserialization issues
    val status: String = PostStatus.PENDING.name,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    // Computed property — converts the stored string back to the enum safely
    val postStatus: PostStatus
        get() = try {
            PostStatus.valueOf(status)
        } catch (e: IllegalArgumentException) {
            PostStatus.PENDING
        }
}

enum class PostStatus(val displayName: String) {
    PENDING("Pending"),
    RESOLVED("Resolved"),
    UNRESOLVED("Unresolved")
}