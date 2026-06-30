package com.iccyuan.hush.data.model

import kotlinx.serialization.Serializable

/** 宏中单个步骤的类型。 */
@Serializable
enum class MacroStepType { TAP, SWIPE, WAIT }

/**
 * 打卡宏的一个步骤。坐标为录制时屏幕的绝对像素（回放时按当前屏幕尺寸缩放）。
 * [delayMs] 是「执行本步前的等待」——录制时即与上一步的真实时间间隔，使回放保真。
 */
@Serializable
data class MacroStep(
    val type: MacroStepType,
    val x: Int = 0,
    val y: Int = 0,
    /** SWIPE 的终点。 */
    val x2: Int = 0,
    val y2: Int = 0,
    /** SWIPE 手势时长 / TAP 按压时长（毫秒）。 */
    val durationMs: Long = 0,
    /** 执行本步之前等待的毫秒数。 */
    val delayMs: Long = 0,
)
