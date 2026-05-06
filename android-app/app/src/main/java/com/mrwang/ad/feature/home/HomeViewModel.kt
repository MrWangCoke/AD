package com.mrwang.ad.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrwang.ad.core.util.PhoneValidator
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
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.OnStudentIdChange -> _state.update { it.copy(studentId = intent.value) }
            is HomeIntent.OnCampusPhoneChange -> _state.update { it.copy(campusPhone = intent.value) }
            HomeIntent.OnBindSubmit -> bindUser()
        }
    }

    private fun bindUser() {
        val snapshot = _state.value
        val studentId = snapshot.studentId.trim()
        val phone = snapshot.campusPhone.trim()

        if (studentId.isBlank()) {
            emitMessage("请输入学号")
            return
        }
        if (phone.isBlank()) {
            emitMessage("请输入新办的电信校园卡手机号")
            return
        }
        if (!PhoneValidator.isValidMainlandPhone(phone)) {
            emitMessage("请输入正确的11位手机号")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isBinding = true) }
            val result = authRepository.bindUser(
                studentId = studentId,
                phone = phone
            )
            _state.update { it.copy(isBinding = false) }

            result
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            boundStudentId = user.studentId.ifBlank { studentId },
                            boundPhone = user.phone.ifBlank { phone }
                        )
                    }
                    _effect.emit(HomeEffect.ShowMessage("绑定成功"))
                }
                .onFailure { error ->
                    _effect.emit(HomeEffect.ShowMessage(error.message ?: "绑定失败"))
                }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _effect.emit(HomeEffect.ShowMessage(message))
        }
    }
}
