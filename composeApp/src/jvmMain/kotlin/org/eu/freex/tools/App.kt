package org.eu.freex.tools

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eu.freex.tools.dialogs.ScreenCropperDialog
import org.eu.freex.tools.model.ColorRule
import org.eu.freex.tools.model.WorkImage
import org.eu.freex.tools.utils.ImageUtils
import java.awt.Component
import java.awt.Container
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

@Composable
fun App(window: androidx.compose.ui.awt.ComposeWindow?) {
    val imageList = remember { mutableStateListOf<WorkImage>() }
    var currentIndex by remember { mutableStateOf(-1) }
    val currentImage = if (currentIndex in imageList.indices) imageList[currentIndex] else null

    var mainScale by remember { mutableStateOf(1f) }
    var mainOffset by remember { mutableStateOf(Offset.Zero) }
    var hoverPixelPos by remember { mutableStateOf<IntOffset?>(null) }
    var hoverColor by remember { mutableStateOf(Color.Transparent) }
    var isCropMode by remember { mutableStateOf(false) }

    val colorRules = remember { mutableStateListOf<ColorRule>() }
    var defaultBias by remember { mutableStateOf("101010") }
    var showBinaryPreview by remember { mutableStateOf(false) }
    var showScreenCropper by remember { mutableStateOf(false) }
    var fullScreenCapture by remember { mutableStateOf<BufferedImage?>(null) }

    val scope = rememberCoroutineScope()

    fun addImage(bufferedImage: BufferedImage, name: String) {
        imageList.add(WorkImage(bufferedImage.toComposeImageBitmap(), bufferedImage, name))
        currentIndex = imageList.lastIndex
        mainScale = 1f; mainOffset = Offset.Zero
        isCropMode = false
    }

    fun loadFile(file: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val img = ImageIO.read(file)
                if (img != null) withContext(Dispatchers.Main) { addImage(img, file.name) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 拖拽挂载 logic
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
                                loadFile(file)
                            }
                        }
                        evt.dropComplete(true)
                    } catch (e: Exception) {
                        e.printStackTrace(); evt.dropComplete(false)
                    }
                }
            }
            // 递归挂载
            fun attachToAll(component: Component) {
                component.dropTarget = dropTarget
                if (component is Container) {
                    for (child in component.components) attachToAll(child)
                }
            }
            attachToAll(window)
            onDispose { window.dropTarget = null }
        }
    }

    if (showScreenCropper && fullScreenCapture != null) {
        ScreenCropperDialog(
            fullScreenImage = fullScreenCapture!!,
            onDismiss = { showScreenCropper = false },
            onCropConfirm = { cropped -> showScreenCropper = false; addImage(cropped, "截图 ${imageList.size + 1}") }
        )
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            onLoadFile = { ImageUtils.pickFile()?.let { loadFile(it) } },
            onScreenCapture = {
                scope.launch(Dispatchers.IO) {
                    delay(300)
                    val capture = ImageUtils.captureFullScreen()
                    withContext(Dispatchers.Main) { fullScreenCapture = capture; showScreenCropper = true }
                }
            },
            isCropMode = isCropMode,
            onToggleCropMode = { isCropMode = !isCropMode }
        )

        Row(Modifier.weight(1f).fillMaxWidth()) {
            Workspace(
                modifier = Modifier.weight(3f),
                workImage = currentImage,
                isCropMode = isCropMode,
                scale = mainScale,
                offset = mainOffset,
                onTransformChange = { s, o -> mainScale = s; mainOffset = o },
                onHoverChange = { pos, col -> hoverPixelPos = pos; hoverColor = col },
                onColorPick = { hex ->
                    if (colorRules.none { it.targetHex == hex }) {
                        colorRules.add(ColorRule(targetHex = hex, biasHex = defaultBias))
                    }
                },
                onCropConfirm = { cropRect ->
                    currentImage?.let {
                        val newImg = ImageUtils.cropImage(it.bufferedImage, cropRect)
                        addImage(newImg, "裁剪 ${imageList.size + 1}")
                    }
                },
                colorRules = colorRules
            )

            RightPanel(
                modifier = Modifier.width(340.dp),
                rawImage = currentImage?.bufferedImage,
                hoverPixelPos = hoverPixelPos,
                hoverColor = hoverColor,
                mainScale = mainScale,
                onScaleChange = { mainScale = it },
                colorRules = colorRules,
                defaultBias = defaultBias,
                onDefaultBiasChange = { defaultBias = it },
                onRuleUpdate = { id, newBias ->
                    val index = colorRules.indexOfFirst { it.id == id }
                    if (index != -1) colorRules[index] = colorRules[index].copy(biasHex = newBias)
                },
                onRuleRemove = { id -> colorRules.removeIf { it.id == id } },
                onClearRules = { colorRules.clear() },
                showBinary = showBinaryPreview,
                onTogglePreview = { showBinaryPreview = !showBinaryPreview }
            )
        }

        BottomBar(
            images = imageList,
            selectedIndex = currentIndex,
            onSelect = { currentIndex = it },
            onDelete = { index ->
                if (index in imageList.indices) {
                    imageList.removeAt(index)
                    if (currentIndex >= imageList.size) currentIndex = imageList.lastIndex
                }
            }
        )
    }
}