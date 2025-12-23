package org.eu.freex.tools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(32.dp)
            .background(if (isSelected) Color(0xFF673AB7) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) Color.White else Color.Black, fontSize = 12.sp)
    }
}

@Composable
fun LabelRow(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        color = Color.DarkGray,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

// 将 RowScope 移除，改用 modifier 控制权重，使其更通用
@Composable
fun NumberInput(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(30.dp)
            .background(Color.White, RoundedCornerShape(2.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(2.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, end = 2.dp), color = Color.Gray)
        BasicTextField(
            value = value.toString(),
            onValueChange = { str ->
                if (str.isEmpty()) onValueChange(0)
                else str.toIntOrNull()?.let { onValueChange(it) }
            },
            textStyle = TextStyle(fontSize = 12.sp, textAlign = TextAlign.Start),
            singleLine = true,
            modifier = Modifier.weight(1f).padding(vertical = 4.dp)
        )
    }
}