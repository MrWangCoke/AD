package com.mrwang.ad.data.remote.model

data class RegisterRequest(
    val phone: String,
    val password: String,
    val confirmPassword: String
)

data class LoginRequest(
    val phone: String,
    val password: String
)

data class BindUserRequest(
    val studentId: String,
    val phone: String
)

data class UserResponse(
    val id: Long,
    val phone: String,
    val name: String,
    val studentId: String,
    val avatarUrl: String?
)
