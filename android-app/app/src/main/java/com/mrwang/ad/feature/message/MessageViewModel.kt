package com.mrwang.ad.feature.message


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 消息页 ViewModel（MVI 简版）：
// 维护静态示例列表，并在点击时发送提示副作用。
class MessageViewModel : ViewModel() {

    // 持续状态流。
    private val _state = MutableStateFlow(MessageState())
    val state: StateFlow<MessageState> = _state.asStateFlow()

    // 一次性事件流（Toast 等）。
    private val _effect = MutableSharedFlow<MessageEffect>()
    val effect: SharedFlow<MessageEffect> = _effect.asSharedFlow()

    // 处理用户意图。
    fun onIntent(intent: MessageIntent) {
        when (intent) {
            is MessageIntent.OnMessageClick -> {
                viewModelScope.launch {
                    _effect.emit(
                        MessageEffect.ShowMessage("你点击了：${intent.message}")
                    )
                }
            }
        }
    }
}
