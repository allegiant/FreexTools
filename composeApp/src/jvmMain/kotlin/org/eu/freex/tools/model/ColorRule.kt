package org.eu.freex.tools.model

data class ColorRule(
    // 使用时间戳作为唯一ID，方便在列表中进行删除/修改操作
    val id: Long = System.nanoTime(),
    val targetHex: String,
    val biasHex: String
)