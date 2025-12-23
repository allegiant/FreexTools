package org.eu.freex.tools.model

data class GridParams(
    val x: Int, val y: Int, val w: Int, val h: Int,
    val colGap: Int, val rowGap: Int,
    val colCount: Int, val rowCount: Int
)