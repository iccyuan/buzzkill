package com.iccyuan.hush.service

import com.iccyuan.hush.data.model.MacroStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 进程内单例，桥接「无障碍服务里的录制悬浮层」与「宏编辑界面」。服务往这里追加录到的步骤，
 * 编辑界面订阅 [steps] / [isRecording] 取回结果（两者同进程，无需 IPC）。
 */
object MacroRecorder {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _steps = MutableStateFlow<List<MacroStep>>(emptyList())
    val steps: StateFlow<List<MacroStep>> = _steps.asStateFlow()

    /** 录制时的屏幕尺寸（像素），随结果一起带给宏，回放时据此缩放。 */
    @Volatile var screenWidth: Int = 0
    @Volatile var screenHeight: Int = 0

    /** 开始新一轮录制：清空上一轮结果。 */
    fun begin(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        _steps.value = emptyList()
        _isRecording.value = true
    }

    fun addStep(step: MacroStep) {
        _steps.value = _steps.value + step
    }

    /** 结束录制（保留已录步骤，供编辑界面读取）。 */
    fun finish() {
        _isRecording.value = false
    }
}
