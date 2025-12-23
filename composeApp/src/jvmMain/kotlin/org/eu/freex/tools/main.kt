package org.eu.freex.tools

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "大漠字库工具 Pro (KMP)",
        state = rememberWindowState(width = 1280.dp, height = 900.dp)
    ) {
        App(window = window)
    }
}