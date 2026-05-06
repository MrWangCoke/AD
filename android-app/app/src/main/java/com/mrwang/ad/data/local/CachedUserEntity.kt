package com.mrwang.ad.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mrwang.ad.data.remote.model.UserResponse

@Entity(tableName = "cached_user")
data class CachedUserEntity(
    @PrimaryKey val slot: Int = 0,
    val id: Long,
    val phone: String,
    val name: String,
    val studentId: String,
    val avatarUrl: String?
)

fun CachedUserEntity.toUserResponse(): UserResponse {
    return UserResponse(
        id = id,
        phone = phone,
        name = name,
        studentId = studentId,
        avatarUrl = avatarUrl
    )
}

fun UserResponse.toCachedUserEntity(): CachedUserEntity {
    return CachedUserEntity(
        id = id,
        phone = phone,
        name = name,
        studentId = studentId,
        avatarUrl = avatarUrl
    )
}
