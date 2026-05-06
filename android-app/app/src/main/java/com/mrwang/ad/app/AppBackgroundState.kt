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

private const val BACKGROUND_PREFS = "app_background"
private const val KEY_IMAGE_URI = "image_uri"
private const val KEY_OPACITY = "opacity"
private const val KEY_NAV_GLASS_OPACITY = "nav_glass_opacity"
private const val DEFAULT_OPACITY = 0.45f
private const val DEFAULT_NAV_GLASS_OPACITY = 0.12f

@Composable
fun rememberAppBackgroundState(): AppBackgroundState {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        AppBackgroundState(context)
    }
}

@Stable
class AppBackgroundState internal constructor(
    private val context: Context
) {
    private val prefs = context.getSharedPreferences(BACKGROUND_PREFS, Context.MODE_PRIVATE)

    var imageUri: Uri? by mutableStateOf(prefs.getString(KEY_IMAGE_URI, null)?.let(Uri::parse))
        private set

    var opacity: Float by mutableFloatStateOf(prefs.getFloat(KEY_OPACITY, DEFAULT_OPACITY))
        private set

    var navGlassOpacity: Float by mutableFloatStateOf(
        prefs.getFloat(KEY_NAV_GLASS_OPACITY, DEFAULT_NAV_GLASS_OPACITY)
    )
        private set

    val opacityPercent: Int
        get() = (opacity * 100).roundToInt()

    val navGlassOpacityPercent: Int
        get() = (navGlassOpacity * 100).roundToInt()

    fun updateImageUri(uri: Uri) {
        persistReadPermission(uri)
        imageUri = uri
        prefs.edit().putString(KEY_IMAGE_URI, uri.toString()).apply()
    }

    fun resetImage() {
        imageUri = null
        prefs.edit().remove(KEY_IMAGE_URI).apply()
    }

    fun updateOpacity(value: Float) {
        opacity = value.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_OPACITY, opacity).apply()
    }

    fun updateNavGlassOpacity(value: Float) {
        navGlassOpacity = value.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_NAV_GLASS_OPACITY, navGlassOpacity).apply()
    }

    private fun persistReadPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
}
