package org.eu.freex.tools


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // 必需
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eu.freex.tools.model.ColorRule
import org.eu.freex.tools.utils.ColorUtils
import java.awt.image.BufferedImage

@Composable
fun RightPanel(
    modifier: Modifier,
    rawImage: BufferedImage?,
    hoverPixelPos: IntOffset?,
    hoverColor: Color,
    mainScale: Float,
    onScaleChange: (Float) -> Unit,
    colorRules: List<ColorRule>,
    defaultBias: String,
    onDefaultBiasChange: (String) -> Unit,
    onRuleUpdate: (Long, String) -> Unit,
    onRuleToggle: (Long, Boolean) -> Unit, // 新增回调
    onRuleRemove: (Long) -> Unit,
    onClearRules: () -> Unit,
    showBinary: Boolean,
    onTogglePreview: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFFEEEEEE))
            .padding(16.dp)
    ) {
        // 1. 全图二值化预览
        Text("全图二值化预览", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .background(Color.DarkGray)
                .border(2.dp, Color.DarkGray)
        ) {
            if (rawImage != null) {
                BinaryPreviewScope(rawImage = rawImage, colorRules = colorRules)
            } else {
                Text("暂无图片", color = Color.Gray, modifier = Modifier.align(Alignment.Center), fontSize = 12.sp)
            }
        }

        // 边界检查：防止裁剪后旧坐标导致越界
        val isHoverValid = rawImage != null && hoverPixelPos != null &&
                hoverPixelPos.x >= 0 && hoverPixelPos.x < rawImage.width &&
                hoverPixelPos.y >= 0 && hoverPixelPos.y < rawImage.height

        if (isHoverValid) {
            val rgb = rawImage!!.getRGB(hoverPixelPos!!.x, hoverPixelPos.y)
            val isMatch = ColorUtils.isMatchAny(rgb, colorRules)
            Text(
                text = if (isMatch) "鼠标处: 有效 (匹配)" else "鼠标处: 无效 (背景)",
                color = if (isMatch) Color(0xFF00C853) else Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
            )
        } else {
            Spacer(Modifier.height(20.dp))
        }

        Divider(Modifier.padding(vertical = 12.dp))

        // 2. 鼠标信息
        Row(verticalAlignment = Alignment.CenterVertically) {
            val safeColor = if (isHoverValid) hoverColor else Color.Transparent
            Box(Modifier.size(16.dp).background(safeColor).border(1.dp, Color.Gray))
            Spacer(Modifier.width(8.dp))

            if (isHoverValid) {
                val r = (safeColor.red * 255).toInt()
                val g = (safeColor.green * 255).toInt()
                val b = (safeColor.blue * 255).toInt()
                Text(
                    String.format("#%02X%02X%02X", r, g, b),
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text("#------", style = MaterialTheme.typography.caption, color = Color.Gray)
            }

            Spacer(Modifier.weight(1f))
            if (hoverPixelPos != null) {
                Text(
                    "(${hoverPixelPos.x}, ${hoverPixelPos.y})",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 3. 默认偏色
        Text("新取色默认偏色", style = MaterialTheme.typography.caption, color = Color.Gray)
        OutlinedTextField(
            value = defaultBias,
            onValueChange = { if (it.length <= 6) onDefaultBiasChange(it.uppercase()) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            singleLine = true,
            textStyle = TextStyle(fontSize = 12.sp),
            colors = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = Color.White)
        )

        Spacer(Modifier.height(12.dp))

        // 4. 规则列表 (Header)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "取色规则 (${colorRules.size}/10)", // 显示数量限制
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClearRules, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.Gray)
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
        ) {
            LazyColumn(contentPadding = PaddingValues(4.dp)) {
                items(colorRules, key = { it.id }) { rule ->
                    ColorRuleRow(
                        rule = rule,
                        onBiasChange = { newBias -> onRuleUpdate(rule.id, newBias) },
                        onRemove = { onRuleRemove(rule.id) },
                        onToggle = { isEnabled -> onRuleToggle(rule.id, isEnabled) }
                    )
                }
            }
        }
    }
}

@Composable
fun BinaryPreviewScope(
    rawImage: BufferedImage,
    colorRules: List<ColorRule>
) {
    var binaryBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // 监听规则变化，后台计算全图
    LaunchedEffect(rawImage, colorRules.size, colorRules.toList()) {
        withContext(Dispatchers.Default) {
            // 过滤未启用的规则
            val activeRules = colorRules.filter { it.isEnabled }

            if (activeRules.isEmpty()) {
                binaryBitmap = null
                return@withContext
            }
            val w = rawImage.width
            val h = rawImage.height
            val resultImg = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)

            for (y in 0 until h) {
                for (x in 0 until w) {
                    val rgb = rawImage.getRGB(x, y)
                    val isMatch = ColorUtils.isMatchAny(rgb, activeRules)
                    // 匹配显示白色，不匹配黑色
                    resultImg.setRGB(x, y, if (isMatch) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
                }
            }
            binaryBitmap = resultImg.toComposeImageBitmap()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        if (colorRules.none { it.isEnabled }) {
            Text("无启用规则", color = Color.Gray, fontSize = 12.sp)
        } else if (binaryBitmap != null) {
            Image(
                bitmap = binaryBitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None
            )
        } else {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ColorRuleRow(
    rule: ColorRule,
    onBiasChange: (String) -> Unit,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    val color = try {
        Color(0xFF000000 or rule.targetHex.toInt(16).toLong())
    } catch (e: Exception) {
        Color.Black
    }

    // 极度紧凑的 Row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp) // 强制限制高度
            .padding(vertical = 2.dp)
            .background(if (rule.isEnabled) Color(0xFFF9F9F9) else Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp), // 减小内部 padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Checkbox
        // 使用 CompositionLocal 移除默认的最小触摸区域限制，使 Checkbox 变小
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Checkbox(
                checked = rule.isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.8f).size(24.dp), // 缩小显示
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF5722))
            )
        }

        Spacer(Modifier.width(4.dp))

        // 2. 色块
        Box(Modifier.size(16.dp).background(color).border(1.dp, Color.Gray))

        Spacer(Modifier.width(6.dp))

        // 3. 目标色值
        Text(
            rule.targetHex,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (rule.isEnabled) Color.Black else Color.Gray,
            modifier = Modifier.width(55.dp)
        )

        Spacer(Modifier.width(4.dp))

        // 4. 容差输入框 (使用 BasicTextField 自定义样式以解决显示不全问题)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp) // 显式控制高度
                .background(Color.White, RoundedCornerShape(2.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(2.dp))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = rule.biasHex,
                onValueChange = { if (it.length <= 6) onBiasChange(it.uppercase()) },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = if (rule.isEnabled) Color.Black else Color.Gray
                ),
                cursorBrush = SolidColor(Color.Black),
                enabled = rule.isEnabled
            )
        }

        Spacer(Modifier.width(4.dp))

        // 5. 删除按钮
        Icon(
            Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(16.dp).clickable { onRemove() },
            tint = Color.Gray
        )
    }
}