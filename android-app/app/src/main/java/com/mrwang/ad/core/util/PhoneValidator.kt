package com.mrwang.ad.core.util

object PhoneValidator {
    private val mainlandPhoneRegex = Regex("^1[3-9]\\d{9}$")

    fun isValidMainlandPhone(value: String): Boolean {
        return mainlandPhoneRegex.matches(value.trim())
    }
}
