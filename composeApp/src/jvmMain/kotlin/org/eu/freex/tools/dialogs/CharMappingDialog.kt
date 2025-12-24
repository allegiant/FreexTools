// composeApp/src/jvmMain/kotlin/org/eu/freex/tools/dialogs/CharMappingDialog.kt
package org.eu.freex.tools.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
// 【关键】导入 Desktop 专用的 Window 组件
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState

@Composable
fun CharMappingDialog(
    bitmap: ImageBitmap,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // 使用 DialogWindow 替代已弃用的 Dialog
    DialogWindow(
        onCloseRequest = onDismiss,
        state = rememberDialogState(width = 300.dp, height = 300.dp),
        title = "字符映射",
        resizable = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("字符映射", style = androidx.compose.material.MaterialTheme.typography.h6)
            Spacer(Modifier.height(16.dp))

            // 显示图片
            Box(Modifier.size(100.dp).background(Color.LightGray).padding(4.dp)) {
                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(16.dp))

            // 输入框
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("输入对应字符") },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester)
            )

            Spacer(Modifier.height(16.dp))

            Row {
                Button(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.width(16.dp))
                Button(onClick = { if (text.isNotEmpty()) onConfirm(text) }) { Text("保存") }
            }
        }

        // 自动聚焦
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}