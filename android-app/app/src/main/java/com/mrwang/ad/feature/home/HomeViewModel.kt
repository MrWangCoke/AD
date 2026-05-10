package com.mrwang.ad.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mrwang.ad.core.util.PhoneValidator
import com.mrwang.ad.data.local.UserSessionRepository
import com.mrwang.ad.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userSessionRepository = UserSessionRepository(application)

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

    init {
        restoreCachedUser()
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.OnStudentIdChange -> _state.update { it.copy(studentId = intent.value) }
            is HomeIntent.OnCampusPhoneChange -> _state.update { it.copy(campusPhone = intent.value) }
            is HomeIntent.OnType3SmsContentChange -> _state.update { it.copy(type3SmsContent = intent.value) }
            HomeIntent.OnBindSubmit -> bindUser()
            HomeIntent.OnType3Submit -> submitType3Ticket()
        }
    }

    private fun bindUser() {
        viewModelScope.launch {
            refreshCachedUser()
            val snapshot = _state.value
            val studentId = snapshot.studentId.trim()
            val phone = snapshot.campusPhone.trim()

            if (snapshot.currentUserId <= 0L) {
                _effect.emit(HomeEffect.ShowMessage("请先登录后再提交工单"))
                return@launch
            }
            if (studentId.isBlank()) {
                _effect.emit(HomeEffect.ShowMessage("请输入学号"))
                return@launch
            }
            if (phone.isBlank()) {
                _effect.emit(HomeEffect.ShowMessage("请输入新办的电信校园卡手机号"))
                return@launch
            }
            if (!PhoneValidator.isValidMainlandPhone(phone)) {
                _effect.emit(HomeEffect.ShowMessage("请输入正确的11位手机号"))
                return@launch
            }

            _state.update { it.copy(isBinding = true) }
            val result = authRepository.bindUser(
                userId = snapshot.currentUserId,
                studentId = studentId,
                phone = phone
            )
            _state.update { it.copy(isBinding = false) }

            result
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            studentId = "",
                            campusPhone = "",
                            boundStudentId = user.studentId.ifBlank { studentId },
                            boundPhone = user.phone.orEmpty().ifBlank { phone },
                            latestTicketNo = user.ticketNo
                        )
                    }
                    _effect.emit(HomeEffect.ShowMessage("绑定工单已提交"))
                }
                .onFailure { error ->
                    _effect.emit(HomeEffect.ShowMessage(error.message ?: "绑定失败"))
                }
        }
    }

    private fun restoreCachedUser() {
        viewModelScope.launch {
            refreshCachedUser()
        }
    }

    private suspend fun refreshCachedUser() {
        val user = userSessionRepository.getCachedUser()
        _state.update {
            it.copy(
                currentUserId = user?.id ?: 0L,
                currentUserPhone = user?.phone.orEmpty(),
                currentUserStudentId = user?.studentId.orEmpty()
            )
        }
    }

    private fun submitType3Ticket() {
        viewModelScope.launch {
            refreshCachedUser()
            val snapshot = _state.value
            val smsContent = snapshot.type3SmsContent.trim()

            if (snapshot.currentUserId <= 0L) {
                _effect.emit(HomeEffect.ShowMessage("请先登录后再提交工单"))
                return@launch
            }
            if (snapshot.currentUserStudentId.isBlank()) {
                _effect.emit(HomeEffect.ShowMessage("请先在个人中心完善学号"))
                return@launch
            }
            if (snapshot.currentUserPhone.isBlank()) {
                _effect.emit(HomeEffect.ShowMessage("请先在个人中心完善手机号"))
                return@launch
            }
            if (smsContent.isBlank()) {
                _effect.emit(HomeEffect.ShowMessage("请先粘贴短信内容"))
                return@launch
            }

            val broadbandAccount = parseBroadbandAccount(smsContent)
            val newPassword = parseBroadbandPassword(smsContent)
            if (broadbandAccount == null || newPassword == null) {
                _effect.emit(HomeEffect.ShowMessage("短信内容识别失败，请检查是否为完整模板"))
                return@launch
            }

            _state.update { it.copy(isSubmittingType3 = true) }
            val result = authRepository.createBroadbandPasswordResetTicket(
                userId = snapshot.currentUserId,
                studentId = snapshot.currentUserStudentId,
                phone = snapshot.currentUserPhone,
                broadbandAccount = broadbandAccount,
                newPassword = newPassword
            )
            _state.update { it.copy(isSubmittingType3 = false) }

            result
                .onSuccess { ticket ->
                    _state.update {
                        it.copy(
                            type3SmsContent = "",
                            latestTicketNo = ticket.ticketNo
                        )
                    }
                    _effect.emit(HomeEffect.ShowMessage("宽带密码重置工单已提交"))
                }
                .onFailure { error ->
                    _effect.emit(HomeEffect.ShowMessage(error.message ?: "工单提交失败"))
                }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _effect.emit(HomeEffect.ShowMessage(message))
        }
    }

    private fun parseBroadbandAccount(message: String): String? {
        val match = Regex("为(\\d+)的").find(message) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private fun parseBroadbandPassword(message: String): String? {
        val match = Regex("重置为(\\d{6})").find(message) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.length == 6 }
    }
}
