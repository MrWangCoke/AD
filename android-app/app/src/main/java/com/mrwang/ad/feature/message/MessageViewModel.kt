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

class MessageViewModel : ViewModel() {

    private val _state = MutableStateFlow(MessageState())
    val state: StateFlow<MessageState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<MessageEffect>()
    val effect: SharedFlow<MessageEffect> = _effect.asSharedFlow()

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