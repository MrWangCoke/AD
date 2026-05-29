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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Home 页业务中枢（MVI）：
// - 输入：HomeIntent（用户动作）
// - 输出：HomeState（持续状态）+ HomeEffect（一次性事件）
class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    // 网络仓库：负责调用后端认证/工单接口。
    private val authRepository = AuthRepository()
    // 本地会话仓库：读取当前缓存登录用户（id/phone/studentId）。
    private val userSessionRepository = UserSessionRepository(application)
    // 绑定冷却计时任务；重复开始前会取消旧任务。
    private var bindCooldownJob: Job? = null

    // 对内可变状态流。
    private val _state = MutableStateFlow(HomeState())
    // 对外只读状态流，防止外部误改。
    val state: StateFlow<HomeState> = _state.asStateFlow()

    // 对内一次性事件流（Toast、导航等）。
    private val _effect = MutableSharedFlow<HomeEffect>()
    // 对外只读 effect 流。
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

    init {
        // ViewModel 初始化时恢复已缓存用户，保证 Home 可立即感知登录态。
        restoreCachedUser()
    }

    // Intent 分发中心：所有 UI 动作只从这里进入业务层。
    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.OnStudentIdChange -> _state.update { it.copy(studentId = intent.value) }
            is HomeIntent.OnCampusPhoneChange -> _state.update { it.copy(campusPhone = intent.value) }
            is HomeIntent.OnType3SmsContentChange -> _state.update { it.copy(type3SmsContent = intent.value) }
            HomeIntent.OnBindSubmit -> bindUser()
            HomeIntent.OnType3Submit -> submitType3Ticket()
        }
    }

    // 提交“新用户绑定”工单流程：
    // 1) 刷新本地用户快照
    // 2) 前置校验（登录、冷却、字段合法性）
    // 3) 调接口
    // 4) 根据 Result 更新状态并发消息
    private fun bindUser() {
        viewModelScope.launch {
            // 先同步一次用户缓存，避免用到过期 ID/手机号。
            refreshCachedUser()
            // 拍快照：在一次提交流程内保持读取一致性，避免中途状态漂移。
            val snapshot = _state.value
            val studentId = snapshot.studentId.trim()
            val phone = snapshot.campusPhone.trim()

            // 未登录直接拦截。
            if (snapshot.currentUserId <= 0L) {
                _effect.emit(HomeEffect.ShowMessage("请先登录后再提交工单"))
                return@launch
            }
            // 冷却期内拦截，和后端限流双保险。
            if (snapshot.bindCooldownSeconds > 0) {
                _effect.emit(HomeEffect.ShowMessage("多次提交请等待${snapshot.bindCooldownSeconds}秒"))
                return@launch
            }
            // 基础字段校验。
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

            // 进入提交态，驱动按钮/输入框禁用。
            _state.update { it.copy(isBinding = true) }
            val result = authRepository.bindUser(
                userId = snapshot.currentUserId,
                studentId = studentId,
                phone = phone
            )
            // 请求完成后退出提交态。
            _state.update { it.copy(isBinding = false) }

            result
                .onSuccess { user ->
                    // 成功后清空输入，并记录最近提交回显信息。
                    _state.update {
                        it.copy(
                            studentId = "",
                            campusPhone = "",
                            boundStudentId = user.studentId.ifBlank { studentId },
                            boundPhone = user.phone.orEmpty().ifBlank { phone },
                            latestTicketNo = user.ticketNo
                        )
                    }
                    // 客户端本地 60 秒冷却，降低重复提交概率。
                    startBindCooldown()
                    _effect.emit(HomeEffect.ShowMessage("绑定工单已提交"))
                }
                .onFailure { error ->
                    val message = error.message ?: "绑定失败"
                    // 若后端已经判定频繁提交，也开启本地冷却以保持体验一致。
                    if (message.contains("多次提交") || message.contains("等待一分钟")) {
                        startBindCooldown()
                    }
                    _effect.emit(HomeEffect.ShowMessage(message))
                }
        }
    }

    // 启动时恢复缓存用户信息（异步执行，不阻塞主线程）。
    private fun restoreCachedUser() {
        viewModelScope.launch {
            refreshCachedUser()
        }
    }

    // 从本地会话仓库拉取用户并写入 HomeState。
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

    // 提交类型 3（宽带密码重置）工单流程。
    private fun submitType3Ticket() {
        viewModelScope.launch {
            refreshCachedUser()
            val snapshot = _state.value
            val smsContent = snapshot.type3SmsContent.trim()

            // 登录与资料完整性校验（类型3依赖学号+手机号）。
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

            // 从短信模板中解析宽带账号和新密码。
            val broadbandAccount = parseBroadbandAccount(smsContent)
            val newPassword = parseBroadbandPassword(smsContent)
            if (broadbandAccount == null || newPassword == null) {
                _effect.emit(HomeEffect.ShowMessage("短信内容识别失败，请检查是否为完整模板"))
                return@launch
            }

            // 标记提交中。
            _state.update { it.copy(isSubmittingType3 = true) }
            val result = authRepository.createBroadbandPasswordResetTicket(
                userId = snapshot.currentUserId,
                studentId = snapshot.currentUserStudentId,
                phone = snapshot.currentUserPhone,
                broadbandAccount = broadbandAccount,
                newPassword = newPassword
            )
            // 无论成功失败都先退出提交中态。
            _state.update { it.copy(isSubmittingType3 = false) }

            result
                .onSuccess { ticket ->
                    // 成功后清空短信输入并记录最新工单号。
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

    // 通用提示函数（当前文件暂未使用，作为可复用辅助方法保留）。
    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _effect.emit(HomeEffect.ShowMessage(message))
        }
    }

    // 开启绑定冷却倒计时：
    // 每秒更新一次 bindCooldownSeconds，直到归零。
    private fun startBindCooldown(seconds: Int = 60) {
        // 若已有旧倒计时，先取消避免多计时器并发写状态。
        bindCooldownJob?.cancel()
        bindCooldownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _state.update { it.copy(bindCooldownSeconds = remaining) }
                delay(1_000)
            }
            _state.update { it.copy(bindCooldownSeconds = 0) }
        }
    }

    // 正则解析短信中的宽带账号：匹配“为123456的”中的数字部分。
    private fun parseBroadbandAccount(message: String): String? {
        val match = Regex("为(\\d+)的").find(message) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    // 正则解析短信中的新密码：匹配“重置为123456”中的 6 位数字密码。
    private fun parseBroadbandPassword(message: String): String? {
        val match = Regex("重置为(\\d{6})").find(message) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.length == 6 }
    }
}
