package com.mrwang.ad.core.util

// 手机号校验工具：
// 当前规则限定中国大陆 11 位手机号（13-19 段）。
object PhoneValidator {
    private val mainlandPhoneRegex = Regex("^1[3-9]\\d{9}$")

    // 忽略首尾空白后执行正则匹配。
    fun isValidMainlandPhone(value: String): Boolean {
        return mainlandPhoneRegex.matches(value.trim())
    }
}
