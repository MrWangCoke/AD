package com.mrwang.ad.feature.profile

// 文件说明：该文件已补充详细注释，重点解释数据流、状态和交互边界。

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.mrwang.ad.core.ui.components.GlassButton as SharedGlassButton
import com.mrwang.ad.core.ui.components.GlassPanel as SharedGlassPanel

// Profile 模块 UI 封装层：
// 通过类型别名复用核心玻璃组件，保持调用方语义清晰。
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

// Profile 模块按钮封装，底层复用通用 GlassButton。
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
