package com.mrwang.ad.feature.message

// 消息页状态：标题和示例消息列表。
data class MessageState(
    val title: String = "消息",
    val messages: List<String> = listOf(
        "系统通知",
        "订单消息",
        "活动提醒"
    )
)

// 消息页用户意图。
sealed interface MessageIntent {
    // 点击某条消息。
    data class OnMessageClick(val message: String) : MessageIntent
}

// 消息页一次性副作用。
sealed interface MessageEffect {
    // 弹出提示。
    data class ShowMessage(val message: String) : MessageEffect
}
