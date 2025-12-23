package org.eu.freex.tools.model

data class ColorRule(
    // 使用时间戳作为唯一ID
    val id: Long = System.nanoTime(),
    val targetHex: String,
    val biasHex: String,
    // 新增：是否启用该规则（默认 true）
    val isEnabled: Boolean = true
)