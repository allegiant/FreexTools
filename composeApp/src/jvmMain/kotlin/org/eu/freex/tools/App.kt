// 文件路径: composeApp/src/jvmMain/kotlin/org/eu/freex/tools/App.kt
package org.eu.freex.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.eu.freex.tools.dialogs.CharMappingDialog
import org.eu.freex.tools.dialogs.ScreenCropperDialog
import org.eu.freex.tools.viewmodel.ImageProcessingViewModel
import java.awt.Component
import java.awt.Container
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.io.File

@Composable
fun App(window: androidx.compose.ui.awt.ComposeWindow?) {
    var currentModule by remember { mutableStateOf(AppModule.IMAGE_PROCESSING) }

    // 【核心变化】创建 ViewModel
    val viewModel = remember { ImageProcessingViewModel() }

    // 拖拽支持 (调用 ViewModel 方法)
    if (window != null) {
        DisposableEffect(window) {
            val dropTarget = object : DropTarget() {
                override fun dragEnter(dtde: DropTargetDragEvent) { dtde.acceptDrag(DnDConstants.ACTION_COPY) }
                override fun dragOver(dtde: DropTargetDragEvent) { dtde.acceptDrag(DnDConstants.ACTION_COPY) }
                override fun drop(evt: DropTargetDropEvent) {
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_COPY)
                        val list = evt.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                        list.firstOrNull()?.let {
                            val file = it as File
                            if (file.name.endsWith(".png") || file.name.endsWith(".jpg") || file.name.endsWith(".bmp")) {
                                viewModel.loadFile(file) // 调用 VM
                            }
                        }
                        evt.dropComplete(true)
                    } catch (e: Exception) { e.printStackTrace(); evt.dropComplete(false) }
                }
            }
            fun attachToAll(component: Component) { component.dropTarget = dropTarget; if (component is Container) for (child in component.components) attachToAll(child) }
            attachToAll(window); onDispose { window.dropTarget = null }
        }
    }

    // 弹窗逻辑 (观察 ViewModel 状态)
    if (viewModel.showScreenCropper && viewModel.fullScreenCapture != null) {
        ScreenCropperDialog(
            fullScreenImage = viewModel.fullScreenCapture!!,
            onDismiss = { viewModel.showScreenCropper = false },
            onCropConfirm = { viewModel.confirmScreenCrop(it) }
        )
    }

    if (viewModel.showMappingDialog && viewModel.mappingBitmap != null) {
        CharMappingDialog(
            bitmap = viewModel.mappingBitmap!!,
            onDismiss = { viewModel.showMappingDialog = false },
            onConfirm = { viewModel.confirmMapping(it) }
        )
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(currentModule = currentModule, onModuleChange = { currentModule = it })
        Box(Modifier.weight(1f)) {
            when (currentModule) {
                AppModule.IMAGE_PROCESSING -> {
                    // 【核心变化】只传一个 viewModel 参数
                    ImageProcessingWorkbench(viewModel = viewModel)
                }
                AppModule.FONT_MANAGER -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("字库管理模块 - 开发中...", color = Color.Gray)
                    }
                }
            }
        }
    }
}