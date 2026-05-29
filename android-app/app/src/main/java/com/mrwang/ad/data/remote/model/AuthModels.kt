package com.mrwang.ad.data.remote.model

// 注册请求体。
data class RegisterRequest(
    val phone: String,
    val password: String,
    val confirmPassword: String
)

// 登录请求体。
data class LoginRequest(
    val phone: String,
    val password: String
)

// 重置密码请求体。
data class ResetPasswordRequest(
    val studentId: String,
    val phone: String,
    val newPassword: String,
    val confirmPassword: String
)

// 新用户绑定请求体（默认 ticketType=1）。
data class BindUserRequest(
    val userId: Long,
    val studentId: String,
    val phone: String,
    val ticketType: Int = 1
)

// 创建工单通用请求体：
// type=3 时会用到 broadbandAccount/newPassword。
data class CreateTicketRequest(
    val ticketType: Int,
    val userId: Long,
    val studentId: String,
    val phone: String,
    val broadbandAccount: String? = null,
    val newPassword: String? = null
)

// 更新资料请求体。
data class UpdateProfileRequest(
    val phone: String,
    val name: String,
    val studentId: String,
    val avatarUrl: String?
)

// 用户响应模型（前后端共享主字段）。
data class UserResponse(
    val id: Long,
    val phone: String,
    val name: String,
    val studentId: String,
    val avatarUrl: String?
)

// 工单响应模型。
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
