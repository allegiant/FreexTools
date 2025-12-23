package org.eu.freex.tools.utils

import org.eu.freex.tools.model.ColorRule
import kotlin.math.abs

object ColorUtils {
    // 单色匹配核心逻辑
    fun isMatch(color: Int, targetStr: String, biasStr: String): Boolean {
        val target = try { targetStr.toInt(16) } catch (e: Exception) { 0 }
        val bias = try { biasStr.toInt(16) } catch (e: Exception) { 0 }

        val tR = (target shr 16) and 0xFF; val tG = (target shr 8) and 0xFF; val tB = target and 0xFF
        val bR = (bias shr 16) and 0xFF; val bG = (bias shr 8) and 0xFF; val bB = bias and 0xFF
        val cR = (color shr 16) and 0xFF; val cG = (color shr 8) and 0xFF; val cB = color and 0xFF

        return abs(cR - tR) <= bR && abs(cG - tG) <= bG && abs(cB - tB) <= bB
    }

    // 匹配任意一条规则
    fun isMatchAny(color: Int, rules: List<ColorRule>): Boolean {
        if (rules.isEmpty()) return false
        return rules.any { rule -> isMatch(color, rule.targetHex, rule.biasHex) }
    }
}