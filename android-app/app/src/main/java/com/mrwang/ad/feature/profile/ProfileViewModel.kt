package com.mrwang.ad.feature.profile


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mrwang.ad.core.util.PhoneValidator
import com.mrwang.ad.data.local.UserSessionRepository
import com.mrwang.ad.data.remote.model.UserResponse
import com.mrwang.ad.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userSessionRepository = UserSessionRepository(application)

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect: SharedFlow<ProfileEffect> = _effect.asSharedFlow()

    init {
        restoreCachedUser()
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.OnLoginPhoneChange -> _state.update { it.copy(loginPhone = intent.value) }
            is ProfileIntent.OnLoginPasswordChange -> _state.update { it.copy(loginPassword = intent.value) }
            ProfileIntent.OnForgotPasswordClick -> startResetPassword()
            is ProfileIntent.OnResetStudentIdChange -> _state.update { it.copy(resetStudentId = intent.value) }
            is ProfileIntent.OnResetPhoneChange -> _state.update { it.copy(resetPhone = intent.value) }
            is ProfileIntent.OnResetPasswordChange -> _state.update { it.copy(resetPassword = intent.value) }
            is ProfileIntent.OnResetConfirmPasswordChange -> _state.update { it.copy(resetConfirmPassword = intent.value) }
            ProfileIntent.OnResetPasswordSubmit -> resetPassword()
            ProfileIntent.OnResetPasswordCancel -> cancelResetPassword()
            is ProfileIntent.OnRegisterPhoneChange -> _state.update { it.copy(registerPhone = intent.value) }
            is ProfileIntent.OnRegisterPasswordChange -> _state.update { it.copy(registerPassword = intent.value) }
            is ProfileIntent.OnRegisterConfirmPasswordChange -> _state.update { it.copy(registerConfirmPassword = intent.value) }
            ProfileIntent.OnEditStart -> startEdit()
            is ProfileIntent.OnEditNameChange -> _state.update { it.copy(editName = intent.value) }
            is ProfileIntent.OnEditStudentIdChange -> _state.update { it.copy(editStudentId = intent.value) }
            is ProfileIntent.OnEditPhoneChange -> _state.update { it.copy(editPhone = intent.value) }
            is ProfileIntent.OnEditAvatarChange -> _state.update { it.copy(editAvatarUrl = intent.value) }
            ProfileIntent.OnEditSave -> saveProfile()
            ProfileIntent.OnLoginSubmit -> login()
            ProfileIntent.OnRegisterSubmit -> register()
            ProfileIntent.OnRefreshTickets -> refreshTickets()

            ProfileIntent.OnLogoutClick -> {
                viewModelScope.launch {
                    userSessionRepository.clearUser()
                    _state.update {
                        it.copy(
                            userId = 0L,
                            name = "未登录",
                            studentId = "--",
                            phone = "--",
                            avatarUrl = null,
                            isLoggedIn = false,
                            editName = "",
                            editStudentId = "",
                            editPhone = "",
                            editAvatarUrl = null,
                            loginPassword = "",
                            resetStudentId = "",
                            resetPhone = "",
                            resetPassword = "",
                            resetConfirmPassword = "",
                            isResetMode = false,
                            tickets = emptyList()
                        )
                    }
                    _effect.emit(ProfileEffect.ShowMessage("已退出登录"))
                }
            }
        }
    }

    private fun startEdit() {
        val snapshot = _state.value
        _state.update {
            it.copy(
                editName = snapshot.name.takeIf { value -> value != "未登录" }.orEmpty(),
                editStudentId = snapshot.studentId.takeIf { value -> value != "--" }.orEmpty(),
                editPhone = snapshot.phone.takeIf { value -> value != "--" }.orEmpty(),
                editAvatarUrl = snapshot.avatarUrl
            )
        }
    }

    private fun saveProfile() {
        val snapshot = _state.value
        if (!snapshot.isLoggedIn) {
            emitMessage("请先登录")
            return
        }

        val name = snapshot.editName.trim()
        val studentId = snapshot.editStudentId.trim()
        val phone = snapshot.editPhone.trim()
        if (name.isBlank()) {
            emitMessage("请输入姓名")
            return
        }
        if (studentId.isBlank()) {
            emitMessage("请输入学号")
            return
        }
        if (phone.isBlank()) {
            emitMessage("请输入手机号")
            return
        }
        if (!PhoneValidator.isValidMainlandPhone(phone)) {
            emitMessage("请输入正确的11位手机号")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = authRepository.updateProfile(
                id = snapshot.userId,
                phone = phone,
                name = name,
                studentId = studentId,
                avatarUrl = snapshot.editAvatarUrl
            )
            _state.update { it.copy(isLoading = false) }
            result
                .onSuccess { user ->
                    applyUser(user)
                    userSessionRepository.saveUser(user)
                    loadTickets(user.id)
                    _effect.emit(ProfileEffect.ShowMessage("资料已保存"))
                    _effect.emit(ProfileEffect.ProfileSaved)
                }
                .onFailure { error ->
                    _effect.emit(ProfileEffect.ShowMessage(error.message ?: "保存失败"))
                }
        }
    }

    private fun restoreCachedUser() {
        viewModelScope.launch {
            userSessionRepository.getCachedUser()?.let { user ->
                applyUser(user)
                loadTickets(user.id)
            }
        }
    }

    private fun login() {
        val snapshot = _state.value
        if (snapshot.loginPhone.isBlank() || snapshot.loginPassword.isBlank()) {
            emitMessage("请输入手机号和密码")
            return
        }
        val phone = snapshot.loginPhone.trim()
        if (!PhoneValidator.isValidMainlandPhone(phone)) {
            emitMessage("请输入正确的11位手机号")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = authRepository.login(
                phone = phone,
                password = snapshot.loginPassword
            )
            _state.update { it.copy(isLoading = false) }
            result
                .onSuccess { user ->
                    applyUser(user)
                    userSessionRepository.saveUser(user)
                    loadTickets(user.id)
                    _effect.emit(ProfileEffect.ShowMessage("登录成功"))
                    _effect.emit(ProfileEffect.LoginSuccess)
                }
                .onFailure { error ->
                    _effect.emit(ProfileEffect.ShowMessage(error.message ?: "登录失败"))
                }
        }
    }

    private fun startResetPassword() {
        val snapshot = _state.value
        _state.update {
            it.copy(
                isResetMode = true,
                resetStudentId = if (snapshot.studentId != "--") snapshot.studentId else "",
                resetPhone = snapshot.loginPhone.ifBlank { if (snapshot.phone != "--") snapshot.phone else "" },
                resetPassword = "",
                resetConfirmPassword = ""
            )
        }
    }

    private fun cancelResetPassword() {
        _state.update {
            it.copy(
                isResetMode = false,
                resetStudentId = "",
                resetPhone = "",
                resetPassword = "",
                resetConfirmPassword = ""
            )
        }
    }

    private fun resetPassword() {
        val snapshot = _state.value
        val studentId = snapshot.resetStudentId.trim()
        val phone = snapshot.resetPhone.trim()
        val newPassword = snapshot.resetPassword
        val confirmPassword = snapshot.resetConfirmPassword

        if (studentId.isBlank()) {
            emitMessage("请输入学号")
            return
        }
        if (phone.isBlank()) {
            emitMessage("请输入绑定手机号")
            return
        }
        if (!PhoneValidator.isValidMainlandPhone(phone)) {
            emitMessage("请输入正确的11位手机号")
            return
        }
        if (newPassword.isBlank() || confirmPassword.isBlank()) {
            emitMessage("请输入新密码和确认密码")
            return
        }
        if (newPassword != confirmPassword) {
            emitMessage("两次密码不一致")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = authRepository.resetPassword(
                studentId = studentId,
                phone = phone,
                newPassword = newPassword,
                confirmPassword = confirmPassword
            )
            _state.update { it.copy(isLoading = false) }
            result
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            loginPhone = user.phone,
                            loginPassword = "",
                            resetStudentId = "",
                            resetPhone = "",
                            resetPassword = "",
                            resetConfirmPassword = "",
                            isResetMode = false
                        )
                    }
                    _effect.emit(ProfileEffect.ShowMessage("密码已重置，请使用新密码登录"))
                    _effect.emit(ProfileEffect.PasswordResetSuccess)
                }
                .onFailure { error ->
                    _effect.emit(ProfileEffect.ShowMessage(error.message ?: "密码重置失败"))
                }
        }
    }

    private fun register() {
        val snapshot = _state.value
        if (snapshot.registerPhone.isBlank() || snapshot.registerPassword.isBlank() || snapshot.registerConfirmPassword.isBlank()) {
            emitMessage("请完整填写注册信息")
            return
        }
        val phone = snapshot.registerPhone.trim()
        if (!PhoneValidator.isValidMainlandPhone(phone)) {
            emitMessage("请输入正确的11位手机号")
            return
        }
        if (snapshot.registerPassword != snapshot.registerConfirmPassword) {
            emitMessage("两次密码不一致")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = authRepository.register(
                phone = phone,
                password = snapshot.registerPassword,
                confirmPassword = snapshot.registerConfirmPassword
            )
            _state.update { it.copy(isLoading = false) }
            result
                .onSuccess { user ->
                    applyUser(user)
                    userSessionRepository.saveUser(user)
                    loadTickets(user.id)
                    _state.update {
                        it.copy(
                            loginPhone = user.phone,
                            loginPassword = "",
                            resetStudentId = "",
                            resetPhone = "",
                            resetPassword = "",
                            resetConfirmPassword = "",
                            isResetMode = false,
                            registerPhone = "",
                            registerPassword = "",
                            registerConfirmPassword = ""
                        )
                    }
                    _effect.emit(ProfileEffect.ShowMessage("注册成功"))
                    _effect.emit(ProfileEffect.RegisterSuccess(user.phone))
                }
                .onFailure { error ->
                    _effect.emit(ProfileEffect.ShowMessage(error.message ?: "注册失败"))
                }
        }
    }

    private fun refreshTickets() {
        val userId = _state.value.userId
        if (userId <= 0L) {
            emitMessage("请先登录")
            return
        }
        viewModelScope.launch {
            loadTickets(userId)
        }
    }

    private suspend fun loadTickets(userId: Long) {
        _state.update { it.copy(isTicketsLoading = true) }
        val result = authRepository.getUserTickets(userId)
        _state.update { it.copy(isTicketsLoading = false) }
        result
            .onSuccess { tickets ->
                _state.update {
                    it.copy(
                        tickets = tickets.sortedWith(
                            compareByDescending<com.mrwang.ad.data.remote.model.TicketResponse> { ticket ->
                                ticket.createdAt.orEmpty()
                            }.thenByDescending { ticket -> ticket.id }
                        )
                    )
                }
            }
            .onFailure { error ->
                _effect.emit(ProfileEffect.ShowMessage(error.message ?: "工单加载失败"))
            }
    }

    private fun applyUser(user: UserResponse) {
        _state.update {
            it.copy(
                userId = user.id,
                name = user.name,
                studentId = user.studentId,
                phone = user.phone,
                avatarUrl = user.avatarUrl,
                isLoggedIn = true,
                editName = user.name,
                editStudentId = user.studentId,
                editPhone = user.phone,
                editAvatarUrl = user.avatarUrl,
                loginPassword = ""
            )
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _effect.emit(ProfileEffect.ShowMessage(message))
        }
    }
}
