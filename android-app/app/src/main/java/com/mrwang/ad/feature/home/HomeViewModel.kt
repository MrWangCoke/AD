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
            HomeIntent.OnBindSubmit -> bindUser()
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
                currentUserPhone = user?.phone.orEmpty()
            )
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _effect.emit(HomeEffect.ShowMessage(message))
        }
    }
}
