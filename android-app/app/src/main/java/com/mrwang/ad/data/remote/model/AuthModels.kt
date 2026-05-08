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
    val userId: Long,
    val studentId: String,
    val phone: String,
    val ticketType: Int = 1
)

data class UpdateProfileRequest(
    val phone: String,
    val name: String,
    val studentId: String,
    val avatarUrl: String?
)

data class UserResponse(
    val id: Long,
    val phone: String,
    val name: String,
    val studentId: String,
    val avatarUrl: String?
)

data class TicketResponse(
    val id: Long,
    val ticketNo: String,
    val studentId: String,
    val ticketType: Int,
    val status: Int,
    val broadbandAccount: String?,
    val newPassword: String?,
    val phone: String?,
    val resultMessage: String?,
    val createdAt: String?,
    val updatedAt: String?
)
