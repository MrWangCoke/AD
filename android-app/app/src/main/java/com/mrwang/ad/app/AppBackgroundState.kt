package com.mrwang.ad.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

// 背景配置持久化键：用于 SharedPreferences 存储。
private const val BACKGROUND_PREFS = "app_background"
private const val KEY_IMAGE_URI = "image_uri"
private const val KEY_OPACITY = "opacity"
private const val KEY_NAV_GLASS_OPACITY = "nav_glass_opacity"
private const val DEFAULT_OPACITY = 0.45f
private const val DEFAULT_NAV_GLASS_OPACITY = 0.12f

// 在 Compose 中记忆背景状态实例，生命周期与组合树绑定。
@Composable
fun rememberAppBackgroundState(): AppBackgroundState {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        AppBackgroundState(context)
    }
}

// 背景状态容器：
// 统一管理背景图 URI、背景透明度、底栏玻璃透明度，并自动落盘。
@Stable
class AppBackgroundState internal constructor(
    private val context: Context
) {
    // 使用应用级偏好存储，避免 Activity 重建后状态丢失。
    private val prefs = context.getSharedPreferences(BACKGROUND_PREFS, Context.MODE_PRIVATE)

    // 当前背景图 URI。使用 mutableStateOf 让 UI 自动响应更新。
    var imageUri: Uri? by mutableStateOf(prefs.getString(KEY_IMAGE_URI, null)?.let(Uri::parse))
        private set

    // 背景图显示透明度，范围 [0,1]。
    var opacity: Float by mutableFloatStateOf(prefs.getFloat(KEY_OPACITY, DEFAULT_OPACITY))
        private set

    // 底部导航玻璃效果透明度，范围 [0,1]。
    var navGlassOpacity: Float by mutableFloatStateOf(
        prefs.getFloat(KEY_NAV_GLASS_OPACITY, DEFAULT_NAV_GLASS_OPACITY)
    )
        private set

    // UI 直接显示百分比时使用，避免每个界面重复换算。
    val opacityPercent: Int
        get() = (opacity * 100).roundToInt()

    // UI 直接显示百分比时使用，避免每个界面重复换算。
    val navGlassOpacityPercent: Int
        get() = (navGlassOpacity * 100).roundToInt()

    // 更新背景图并持久化；同时尝试保留 URI 读权限，确保重启后可访问。
    fun updateImageUri(uri: Uri) {
        persistReadPermission(uri)
        imageUri = uri
        prefs.edit().putString(KEY_IMAGE_URI, uri.toString()).apply()
    }

    // 恢复默认背景图（移除用户自选 URI）。
    fun resetImage() {
        imageUri = null
        prefs.edit().remove(KEY_IMAGE_URI).apply()
    }

    // 更新背景透明度并持久化，强制限制在合法区间。
    fun updateOpacity(value: Float) {
        opacity = value.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_OPACITY, opacity).apply()
    }

    // 更新导航玻璃透明度并持久化，强制限制在合法区间。
    fun updateNavGlassOpacity(value: Float) {
        navGlassOpacity = value.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_NAV_GLASS_OPACITY, navGlassOpacity).apply()
    }

    // 向系统申请持久读权限。若 URI 不支持该权限，捕获异常避免崩溃。
    private fun persistReadPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
}
