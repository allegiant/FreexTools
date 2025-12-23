package org.eu.freex.tools.dialogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min
import org.eu.freex.tools.utils.ImageUtils

@Composable
fun ScreenCropperDialog(
    fullScreenImage: BufferedImage,
    onCropConfirm: (BufferedImage) -> Unit,
    onDismiss: () -> Unit
) {
    val displayBitmap = remember(fullScreenImage) { fullScreenImage.toComposeImageBitmap() }
    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var currentPoint by remember { mutableStateOf<Offset?>(null) }

    val selectionRect = remember(startPoint, currentPoint) {
        if (startPoint != null && currentPoint != null) {
            Rect(
                left = min(startPoint!!.x, currentPoint!!.x),
                top = min(startPoint!!.y, currentPoint!!.y),
                right = max(startPoint!!.x, currentPoint!!.x),
                bottom = max(startPoint!!.y, currentPoint!!.y)
            )
        } else null
    }

    // 使用原生 Window 实现全屏覆盖
    Window(
        onCloseRequest = onDismiss,
        undecorated = true,
        transparent = true,
        resizable = false,
        alwaysOnTop = true,
        state = rememberWindowState(placement = WindowPlacement.Maximized)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startPoint = it; currentPoint = it },
                        onDrag = { change, _ -> change.consume(); currentPoint = change.position }
                    )
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawImage(displayBitmap)
                drawRect(Color.Black.copy(alpha = 0.6f))

                selectionRect?.let { rect ->
                    drawImage(
                        image = displayBitmap,
                        srcOffset = IntOffset(rect.left.toInt(), rect.top.toInt()),
                        srcSize = IntSize(rect.width.toInt(), rect.height.toInt()),
                        dstOffset = IntOffset(rect.left.toInt(), rect.top.toInt()),
                        dstSize = IntSize(rect.width.toInt(), rect.height.toInt())
                    )
                    drawRect(Color.Red, rect.topLeft, rect.size, style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
                }
            }

            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(30.dp)
                    .background(Color.White, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(10.dp)
            ) {
                Button(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.width(16.dp))
                Button(
                    enabled = selectionRect != null && selectionRect.width > 5,
                    onClick = {
                        selectionRect?.let {
                            onCropConfirm(ImageUtils.cropImage(fullScreenImage, it))
                        }
                    }
                ) { Text("确认裁剪") }
            }
        }
    }
}