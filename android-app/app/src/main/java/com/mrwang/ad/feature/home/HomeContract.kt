package com.mrwang.ad.feature.home


data class HomeState(
    val currentUserId: Long = 0L,
    val currentUserPhone: String = "",
    val currentUserStudentId: String = "",
    val studentId: String = "",
    val campusPhone: String = "",
    val type3SmsContent: String = "",
    val isSubmittingType3: Boolean = false,
    val isBinding: Boolean = false,
    val boundStudentId: String = "",
    val boundPhone: String = "",
    val latestTicketNo: String = ""
)

sealed interface HomeIntent {
    data class OnStudentIdChange(val value: String) : HomeIntent
    data class OnCampusPhoneChange(val value: String) : HomeIntent
    data class OnType3SmsContentChange(val value: String) : HomeIntent
    data object OnBindSubmit : HomeIntent
    data object OnType3Submit : HomeIntent
}

sealed interface HomeEffect {
    data class ShowMessage(val message: String) : HomeEffect
}
