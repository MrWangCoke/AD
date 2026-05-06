package com.mrwang.ad.feature.profile

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.mrwang.ad.core.ui.components.GlassButton as SharedGlassButton
import com.mrwang.ad.core.ui.components.GlassPanel as SharedGlassPanel

@Composable
fun GlassPanel(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable BoxScope.() -> Unit
) {
    SharedGlassPanel(
        backdrop = backdrop,
        modifier = modifier,
        cornerRadius = cornerRadius,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun GlassButton(
    text: String,
    backdrop: LayerBackdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    SharedGlassButton(
        text = text,
        backdrop = backdrop,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    )
}
