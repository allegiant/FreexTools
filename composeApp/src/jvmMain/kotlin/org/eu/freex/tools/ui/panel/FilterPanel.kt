// 文件路径: composeApp/src/jvmMain/kotlin/org/eu/freex/tools/ui/panel/FilterPanel.kt
package org.eu.freex.tools.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.eu.freex.tools.model.*
import org.eu.freex.tools.ui.components.FilterGridButton
import org.eu.freex.tools.ui.components.SectionHeader

@Composable
fun FilterPanel(
    modifier: Modifier,
    currentFilter: ImageFilter,
    onFilterChange: (ImageFilter) -> Unit,
    colorRules: List<ColorRule>,
    defaultBias: String,
    onDefaultBiasChange: (String) -> Unit,
    onRuleUpdate: (Long, String) -> Unit,
    onRuleToggle: (Long, Boolean) -> Unit,
    onRuleRemove: (Long) -> Unit,
    onClearRules: () -> Unit,
    thresholdRange: ClosedFloatingPointRange<Float>,
    onThresholdChange: (ClosedFloatingPointRange<Float>) -> Unit,
    isRgbAvgEnabled: Boolean,
    onIsRgbAvgEnabledChange: (Boolean) -> Unit,
    onProcessAdd: (ImageFilter) -> Unit
) {
    Column(modifier = modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            FilterSection(ColorFilterType.TITLE, ColorFilterType.COLOR, ColorFilterType.entries, currentFilter, onFilterChange)
            FilterSection(BlackWhiteFilterType.TITLE, BlackWhiteFilterType.COLOR, BlackWhiteFilterType.entries, currentFilter, onFilterChange)
            FilterSection(CommonFilterType.TITLE, CommonFilterType.COLOR, CommonFilterType.entries, currentFilter, onFilterChange)

            Spacer(Modifier.height(8.dp))
            Divider(color = Color.Gray, thickness = 1.dp)

            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp).background(Color(0xFFB0BEC5)).padding(1.dp)) {
                Column(Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).padding(4.dp)) {
                    when (currentFilter) {
                        ColorFilterType.BINARIZATION -> BinarizationParams(
                            colorRules,
                            defaultBias,
                            onDefaultBiasChange,
                            onRuleUpdate,
                            onRuleToggle,
                            onRuleRemove,
                            onClearRules,
                            thresholdRange, onThresholdChange, isRgbAvgEnabled, onIsRgbAvgEnabledChange
                        )
                        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("当前功能: ${currentFilter.label}\n暂无参数配置", color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            Button(
                onClick = { onProcessAdd(currentFilter) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF8BC34A), contentColor = Color.Black),
                shape = androidx.compose.ui.graphics.RectangleShape
            ) { Text("添加") }

            Button(onClick = { }, modifier = Modifier.weight(1f).fillMaxHeight(), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFD54F), contentColor = Color.Black), shape = androidx.compose.ui.graphics.RectangleShape) { Text("前插入") }
            Button(onClick = { }, modifier = Modifier.weight(1f).fillMaxHeight(), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEF5350), contentColor = Color.White), shape = androidx.compose.ui.graphics.RectangleShape) { Text("修改") }
        }
    }
}

@Composable
fun <T : ImageFilter> FilterSection(title: String, color: Color, filters: List<T>, selected: ImageFilter, onSelect: (T) -> Unit) {
    SectionHeader(title, color)
    filters.chunked(2).forEach { rowItems ->
        Row(Modifier.fillMaxWidth()) {
            rowItems.forEach { item -> FilterGridButton(item.label, selected == item, { onSelect(item) }, Modifier.weight(1f)) }
            if (rowItems.size < 2) Spacer(Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BinarizationParams(
    colorRules: List<ColorRule>,
    defaultBias: String,
    onDefaultBiasChange: (String) -> Unit,
    onRuleUpdate: (Long, String) -> Unit,
    onRuleToggle: (Long, Boolean) -> Unit,
    onRuleRemove: (Long) -> Unit,
    onClearRules: () -> Unit,
    thresholdRange: ClosedFloatingPointRange<Float>,
    onThresholdChange: (ClosedFloatingPointRange<Float>) -> Unit,
    isRgbAvgEnabled: Boolean,
    onIsRgbAvgEnabledChange: (Boolean) -> Unit
) {
    val yellowColor = Color(0xFFFFD54F)

    Column {
        Box(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray).background(Color(0xFFE0E0E0))) {
            Column(Modifier.padding(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                        Checkbox(
                            checked = isRgbAvgEnabled,
                            onCheckedChange = onIsRgbAvgEnabledChange, // 绑定回调
                            modifier = Modifier.scale(0.8f).size(20.dp),
                            colors = CheckboxDefaults.colors(checkedColor = Color.Black)
                        )
                    }
                    Spacer(Modifier.width(4.dp)); Text("RGB平均阈值", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                    // 绑定输入框和滑块到 onThresholdChange
                    ThresholdInput(thresholdRange.start.toInt()) { new -> onThresholdChange(new.toFloat().coerceIn(0f, thresholdRange.endInclusive)..thresholdRange.endInclusive) }
                    RangeSlider(
                        value = thresholdRange,
                        onValueChange = onThresholdChange, // 绑定回调
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(thumbColor = yellowColor, activeTrackColor = yellowColor, inactiveTrackColor = Color.Gray)
                    )
                    ThresholdInput(thresholdRange.endInclusive.toInt()) { new -> onThresholdChange(thresholdRange.start..new.toFloat().coerceIn(thresholdRange.start, 255f)) }
                }
            }
        }
        Spacer(Modifier.height(8.dp)); OptionBox("智能 (点数均衡)"); OptionBox("自动 (OTSU算法)")
        Spacer(Modifier.height(12.dp)); Divider(); Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("新取色偏色: ", fontSize = 12.sp)
            BasicTextField(value = defaultBias, onValueChange = onDefaultBiasChange, modifier = Modifier.width(60.dp).background(Color.White).border(1.dp, Color.Gray).padding(2.dp), textStyle = TextStyle(fontSize = 12.sp))
            Spacer(Modifier.weight(1f)); Icon(Icons.Default.DeleteSweep, "Clear", Modifier.clickable { onClearRules() }.size(20.dp), tint = Color.Gray)
        }
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White).border(1.dp, Color.LightGray)) {
            LazyColumn(contentPadding = PaddingValues(2.dp)) {
                items(colorRules, key = { it.id }) { rule ->
                    // 【关键修复】使用命名参数确保 lambda 传递给正确的参数
                    ColorRuleItem(
                        rule = rule,
                        onBiasChange = { onRuleUpdate(rule.id, it) },
                        onToggle = { onRuleToggle(rule.id, it) }, // 修正：it (Boolean) 传给 onToggle
                        onRemove = { onRuleRemove(rule.id) }       // 修正：onRemove 是无参回调
                    )
                }
            }
        }
    }
}

@Composable
fun ThresholdInput(value: Int, onValueChange: (Int) -> Unit) {
    BasicTextField(value = value.toString(), onValueChange = { str -> if (str.isEmpty()) onValueChange(0) else str.toIntOrNull()?.let { onValueChange(it) } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(36.dp).height(20.dp).background(Color.White).border(1.dp, Color.Gray), textStyle = TextStyle(fontSize = 12.sp, textAlign = TextAlign.Center), singleLine = true)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OptionBox(text: String) {
    Box(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray).background(Color(0xFFE0E0E0)).padding(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) { Checkbox(checked = false, onCheckedChange = {}, modifier = Modifier.scale(0.8f).size(20.dp), colors = CheckboxDefaults.colors(checkedColor = Color.Black)) }
            Spacer(Modifier.width(4.dp)); Text(text, fontSize = 12.sp)
        }
    }
}