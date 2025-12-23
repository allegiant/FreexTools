package org.eu.freex.tools.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.eu.freex.tools.model.ColorRule

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ColorRuleItem(
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(vertical = 2.dp)
            .background(if (rule.isEnabled) Color(0xFFF9F9F9) else Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Checkbox(
                checked = rule.isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.8f).size(24.dp),
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF5722))
            )
        }
        Spacer(Modifier.width(4.dp))
        Box(Modifier.size(16.dp).background(color).border(1.dp, Color.Gray))
        Spacer(Modifier.width(6.dp))
        Text(
            rule.targetHex,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (rule.isEnabled) Color.Black else Color.Gray,
            modifier = Modifier.width(55.dp)
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
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
        Icon(
            Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(16.dp).clickable { onRemove() },
            tint = Color.Gray
        )
    }
}