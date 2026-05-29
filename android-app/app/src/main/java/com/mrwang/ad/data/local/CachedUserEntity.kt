package com.mrwang.ad.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mrwang.ad.data.remote.model.UserResponse

// 本地缓存用户实体（Room）。
@Entity(tableName = "cached_user")
data class CachedUserEntity(
    // 固定主键槽位：始终是单用户缓存。
    @PrimaryKey val slot: Int = 0,
    val id: Long,
    val phone: String,
    val name: String,
    val studentId: String,
    val avatarUrl: String?
)

// Room 实体 -> 网络模型（便于上层复用统一 UserResponse 结构）。
fun CachedUserEntity.toUserResponse(): UserResponse {
    return UserResponse(
        id = id,
        phone = phone,
        name = name,
        studentId = studentId,
        avatarUrl = avatarUrl
    )
}

// 网络模型 -> Room 实体（登录/注册成功后持久化到本地）。
fun UserResponse.toCachedUserEntity(): CachedUserEntity {
    return CachedUserEntity(
        id = id,
        phone = phone,
        name = name,
        studentId = studentId,
        avatarUrl = avatarUrl
    )
}
