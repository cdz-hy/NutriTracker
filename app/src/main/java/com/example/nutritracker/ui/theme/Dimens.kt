package com.example.nutritracker.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 全局设计令牌 - 统一间距、圆角、尺寸
 * 所有页面应使用这些常量替代硬编码值
 */
object Dimens {
    // ── 间距 ──────────────────────────────────────────────────────
    /** 页面水平内边距 */
    val ContentPadding = 16.dp
    /** 卡片之间的间距 */
    val CardSpacing = 12.dp
    /** 区块之间的间距（如 计算/AI/关于 三个 section 之间） */
    val SectionSpacing = 20.dp
    /** 卡片内部内边距 */
    val CardInnerPadding = 20.dp
    /** 卡片内部紧凑内边距（列表项等） */
    val CardInnerPaddingCompact = 16.dp

    // ── 圆角 ──────────────────────────────────────────────────────
    /** 标准卡片圆角 */
    val CardCorner = 16.dp
    /** 小卡片/Chip 圆角 */
    val SmallCorner = 12.dp
    /** 输入框/搜索栏圆角 */
    val InputCorner = 12.dp

    // ── 按钮 ──────────────────────────────────────────────────────
    /** 全宽按钮高度 */
    val ButtonHeight = 56.dp
    /** 紧凑按钮高度 */
    val ButtonHeightSmall = 40.dp

    // ── 图标 ──────────────────────────────────────────────────────
    /** 列表项前导图标 */
    val IconSizeMedium = 24.dp
    /** 操作图标（删除/编辑等） */
    val IconSizeSmall = 20.dp

    // ── 海拔 ──────────────────────────────────────────────────────
    /** 标准卡片海拔 */
    val CardElevation = 1.dp
    /** 强调卡片海拔 */
    val CardElevationHigh = 2.dp
    /** 微弱卡片海拔 */
    val CardElevationLow = 0.5.dp
}
