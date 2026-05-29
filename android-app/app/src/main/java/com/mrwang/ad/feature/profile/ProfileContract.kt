package com.mrwang.ad.feature.profile

import com.mrwang.ad.data.remote.model.TicketResponse

// 个人页完整状态：
// 同时承载展示态（资料、工单）和表单态（登录/注册/重置/编辑）。
data class ProfileState(
    val title: String = "个人",
    val userId: Long = 0L,
    val name: String = "未登录",
    val studentId: String = "--",
    val phone: String = "--",
    val avatarUrl: String? = null,
    val isLoggedIn: Boolean = false,
    val editName: String = "",
    val editStudentId: String = "",
    val editPhone: String = "",
    val editAvatarUrl: String? = null,
    val loginPhone: String = "",
    val loginPassword: String = "",
    val resetStudentId: String = "",
    val resetPhone: String = "",
    val resetPassword: String = "",
    val resetConfirmPassword: String = "",
    val isResetMode: Boolean = false,
    val registerPhone: String = "",
    val registerPassword: String = "",
    val registerConfirmPassword: String = "",
    val isLoading: Boolean = false,
    val isTicketsLoading: Boolean = false,
    val tickets: List<TicketResponse> = emptyList()
)

// 个人页所有用户意图。
sealed interface ProfileIntent {
    data class OnLoginPhoneChange(val value: String) : ProfileIntent
    data class OnLoginPasswordChange(val value: String) : ProfileIntent
    data object OnForgotPasswordClick : ProfileIntent
    data class OnResetStudentIdChange(val value: String) : ProfileIntent
    data class OnResetPhoneChange(val value: String) : ProfileIntent
    data class OnResetPasswordChange(val value: String) : ProfileIntent
    data class OnResetConfirmPasswordChange(val value: String) : ProfileIntent
    data object OnResetPasswordSubmit : ProfileIntent
    data object OnResetPasswordCancel : ProfileIntent
    data class OnRegisterPhoneChange(val value: String) : ProfileIntent
    data class OnRegisterPasswordChange(val value: String) : ProfileIntent
    data class OnRegisterConfirmPasswordChange(val value: String) : ProfileIntent
    data object OnEditStart : ProfileIntent
    data class OnEditNameChange(val value: String) : ProfileIntent
    data class OnEditStudentIdChange(val value: String) : ProfileIntent
    data class OnEditPhoneChange(val value: String) : ProfileIntent
    data class OnEditAvatarChange(val value: String?) : ProfileIntent
    data object OnEditSave : ProfileIntent
    data object OnLoginSubmit : ProfileIntent
    data object OnRegisterSubmit : ProfileIntent
    data object OnLogoutClick : ProfileIntent
    data object OnRefreshTickets : ProfileIntent
}

// 个人页一次性副作用（提示/导航信号）。
sealed interface ProfileEffect {
    data class ShowMessage(val message: String) : ProfileEffect
    data object LoginSuccess : ProfileEffect
    data class RegisterSuccess(val phone: String) : ProfileEffect
    data object PasswordResetSuccess : ProfileEffect
    data object ProfileSaved : ProfileEffect
}
