package com.mrwang.ad.feature.home


data class HomeState(
    val studentId: String = "",
    val campusPhone: String = "",
    val isBinding: Boolean = false,
    val boundStudentId: String = "",
    val boundPhone: String = ""
)

sealed interface HomeIntent {
    data class OnStudentIdChange(val value: String) : HomeIntent
    data class OnCampusPhoneChange(val value: String) : HomeIntent
    data object OnBindSubmit : HomeIntent
}

sealed interface HomeEffect {
    data class ShowMessage(val message: String) : HomeEffect
}
