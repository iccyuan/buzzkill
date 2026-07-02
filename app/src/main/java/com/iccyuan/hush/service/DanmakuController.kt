package com.iccyuan.hush.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.TextView
import com.iccyuan.hush.util.Logger

/**
 * 通过悬浮窗在屏幕顶部显示滚动的"弹幕"文字。每次调用都会添加一条弹幕，使其从右边缘动画移动
 * 到左侧屏幕外，然后将自身移除。弹幕会被放置在若干轮换的行上，以避免相互重叠。
 * 需要"在其他应用上层显示"的权限。
 */
object DanmakuController {

    private val main = Handler(Looper.getMainLooper())
    private var rowIndex = 0
    private const val ROWS = 4

    fun canShow(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun show(context: Context, text: String, durationMs: Long) {
        if (text.isBlank()) return
        if (!canShow(context)) {
            // 未授予悬浮窗权限——规则虽匹配，但弹幕无法显示。在编辑器里会提示授权。
            Logger.w("danmaku skipped: overlay permission not granted")
            return
        }
        val app = context.applicationContext
        main.post { showOnMain(app, text, durationMs) }
    }

    private fun showOnMain(app: Context, text: String, durationMs: Long) {
        val wm = app.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val metrics = app.resources.displayMetrics
        val screenWidth = metrics.widthPixels

        val tv = TextView(app).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setSingleLine()
            // 更紧凑、更深的描边阴影，确保白字在任意壁纸上都有清晰边缘。
            setShadowLayer(6f, 0f, 1f, Color.argb(220, 0, 0, 0))
            // 半透明深色圆角胶囊背景：在浅色壁纸上也能保证对比度，同时更美观。
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = metrics.density * 16
                setColor(Color.argb(140, 0, 0, 0))
            }
            val padH = (metrics.density * 14).toInt()
            val padV = (metrics.density * 6).toInt()
            setPadding(padH, padV, padH, padV)
            // 关键：在 addView 之前就定位到右边缘外并隐藏，避免视图先在左侧 (x=0) 画出一帧
            // 再跳到右侧造成「闪烁」，也确保确实是从最右侧滑入。
            translationX = screenWidth.toFloat()
            alpha = 0f
        }

        // 承载弹幕的全屏宽容器：让 TextView 能在整屏范围内平移。若窗口只有内容宽度，
        // 平移到窗口外的部分会被裁剪，导致「从最右侧滑入」失效（只在接近左侧才可见）。
        // 容器透明、不可聚焦、不可触摸，不拦截用户操作。
        val container = android.widget.FrameLayout(app).apply {
            addView(
                tv,
                android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val lp = WindowManager.LayoutParams(
            // 窗口占满屏宽（承载容器），胶囊 TextView 在其内平移；容器透明且不可触摸。
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val topInset = (metrics.density * 48).toInt()
            val rowHeight = (metrics.density * 40).toInt()
            y = topInset + (rowIndex % ROWS) * rowHeight
        }
        rowIndex = (rowIndex + 1) % ROWS

        runCatching { wm.addView(container, lp) }.onFailure {
            // 某些 OEM（如 ColorOS）即使已授予「显示在其他应用上层」，仍会拦截
            // 后台进程绘制悬浮窗，需额外开启「后台弹出界面」权限。记录原因便于排查。
            Logger.w("danmaku addView failed: ${it.message}")
            return
        }

        // 布局完成（容器与文字宽度已知）后：从容器（全屏宽）最右侧滑入，淡入，
        // 再匀速向左平移直至完全移出左边缘。
        tv.post {
            tv.translationX = container.width.toFloat().coerceAtLeast(screenWidth.toFloat())
            tv.alpha = 1f
            tv.animate()
                .translationX(-tv.width.toFloat())
                .setDuration(durationMs.coerceIn(2000, 20000))
                .setInterpolator(LinearInterpolator())
                .withEndAction { runCatching { wm.removeView(container) } }
                .start()
        }
    }

    fun overlaySettingsIntent(context: Context) =
        android.content.Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}"),
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
}
