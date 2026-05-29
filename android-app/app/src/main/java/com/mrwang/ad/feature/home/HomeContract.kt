package com.mrwang.ad.feature.home // Home 功能模块的包路径，便于按功能分层管理。

// Home 页面完整 UI 状态：ViewModel 维护它，UI 只根据它渲染。
data class HomeState(
    val currentUserId: Long = 0L, // 当前登录用户 ID；0L 通常表示未登录或尚未加载完成。
    val currentUserPhone: String = "", // 当前登录用户手机号（用于提交工单和校验）。
    val currentUserStudentId: String = "", // 当前登录用户学号（类型 3 工单会使用）。
    val studentId: String = "", // 首页“新用户绑定”输入框中的学号。
    val campusPhone: String = "", // 首页“新用户绑定”输入框中的校园卡手机号。
    val type3SmsContent: String = "", // 类型 3（宽带密码重置）短信原文输入内容。
    val isSubmittingType3: Boolean = false, // 是否正在提交类型 3 工单，用于按钮 loading/禁用。
    val isBinding: Boolean = false, // 是否正在提交绑定工单，用于防重复点击和状态展示。
    val bindCooldownSeconds: Int = 0, // 绑定提交冷却倒计时（秒），与后端限流协同。
    val boundStudentId: String = "", // 最近一次成功绑定的学号回显，给用户确认提交内容。
    val boundPhone: String = "", // 最近一次成功绑定的手机号回显，给用户确认提交内容。
    val latestTicketNo: String = "" // 最近一次成功提交的工单号，便于追踪处理进度。
)

// Home 页面所有“用户意图”定义：UI 发 Intent，ViewModel 消费后更新状态/触发副作用。
sealed interface HomeIntent {
    data class OnStudentIdChange(val value: String) : HomeIntent // 学号输入变化事件。
    data class OnCampusPhoneChange(val value: String) : HomeIntent // 校园手机号输入变化事件。
    data class OnType3SmsContentChange(val value: String) : HomeIntent // 类型 3 短信输入变化事件。
    data object OnBindSubmit : HomeIntent // 点击“绑定提交”事件。
    data object OnType3Submit : HomeIntent // 点击“类型 3 提交”事件。
}

// Home 页面一次性副作用事件：例如 Toast，不放进长期 State 里。
sealed interface HomeEffect {
    data class ShowMessage(val message: String) : HomeEffect // 提示消息事件（通常给 Snackbar/Toast）。
}
