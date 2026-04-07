package com.example.communityapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class User(
    @DocumentId val id: String = "",
    val email: String = "",
    val displayName: String = "",
    // @PropertyName prevents Firestore stripping the 'is' prefix (Java bean naming convention).
    // Without this, Firestore looks for field "moderator" instead of "isModerator".
    @get:PropertyName("isModerator")
    @set:PropertyName("isModerator")
    var moderator: Boolean = false
)