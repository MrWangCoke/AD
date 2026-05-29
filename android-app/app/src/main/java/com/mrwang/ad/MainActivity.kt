package com.mrwang.ad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mrwang.ad.app.AppRoot
import com.mrwang.ad.ui.theme.ADTheme

// 应用主 Activity：
// 仅负责初始化 Compose 容器和全局主题，不承载业务逻辑。
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 让内容绘制到系统栏区域，结合沉浸式 UI 使用。
        enableEdgeToEdge()
        setContent {
            // 全局 Material3 主题入口。
            ADTheme {
                // App 的实际根组合函数（导航、背景、底栏都从这里展开）。
                AppRoot()
            }
        }
    }
}
