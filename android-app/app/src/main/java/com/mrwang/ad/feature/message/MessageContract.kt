package com.mrwang.ad.feature.message


data class MessageState(
    val title: String = "消息",
    val messages: List<String> = listOf(
        "系统通知",
        "订单消息",
        "活动提醒"
    )
)

sealed interface MessageIntent {
    data class OnMessageClick(val message: String) : MessageIntent
}

sealed interface MessageEffect {
    data class ShowMessage(val message: String) : MessageEffect
}