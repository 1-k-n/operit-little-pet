package com.operit.pet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import kotlin.math.max

/**
 * 悬浮窗渲染核心。
 * 在一个 150x150 的透明 FrameLayout 里，用 WebView 加载 assets/pet/index.html
 * 渲染像素风 Q 版小精灵，并内置全局手势拖拽逻辑。
 */
class PetRenderer(private val context: Context) {

    companion object {
        const val SIZE_DP = 120
    }

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private lateinit var overlayView: View
    var webView: WebView? = null
        private set

    private val layoutParams by lazy {
        val sizePx = dp(SIZE_DP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                sizePx, sizePx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }
        } else {
            WindowManager.LayoutParams(
                sizePx, sizePx,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
        }
    }

    private var touchStartX = 0
    private var touchStartY = 0
    private var startX = 0
    private var startY = 0
    private var isDragging = false
    private var lastTapTime = 0L
    fun show(onDoubleTap: () -> Unit, onSingleTap: () -> Unit = {}) {
        val container = FrameLayout(context)
        container.setBackgroundColor(Color.TRANSPARENT)

        webView = WebView(context).also { wv ->
            configureWebView(wv)
            container.addView(
                wv,
                FrameLayout.LayoutParams(dp(SIZE_DP), dp(SIZE_DP))
            )
        }

        // 手势：外层容器拦截触摸 → 拖拽 / 单击 / 双击
        container.setOnTouchListener { _, event ->
            handleTouch(event, onDoubleTap, onSingleTap)
            true
        }

        overlayView = container
        wm.addView(overlayView, layoutParams)

        // 加载本地 SVG/HTML 渲染小精灵
        webView?.loadUrl("file:///android_asset/pet/index.html")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(wv: WebView) {
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.loadWithOverviewMode = true
        wv.settings.useWideViewPort = true
        wv.setBackgroundColor(Color.TRANSPARENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            wv.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }

    /** 从 Android 侧调 JS，让前端切表情/气泡 */
    fun sendToPet(js: String) {
        webView?.post {
            webView?.evaluateJavascript(js, null)
        }
    }

    private fun handleTouch(event: MotionEvent, onDoubleTap: () -> Unit, onSingleTap: () -> Unit): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.rawX.toInt()
                touchStartY = event.rawY.toInt()
                startX = layoutParams.x
                startY = layoutParams.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX.toInt() - touchStartX
                val dy = event.rawY.toInt() - touchStartY
                if (max(kotlin.math.abs(dx), kotlin.math.abs(dy)) > threshold()) {
                    isDragging = true
                }
                if (isDragging) {
                    layoutParams.x = startX + dx
                    layoutParams.y = startY + dy
                    wm.updateViewLayout(overlayView, layoutParams)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < 300) {
                        // 双击
                        lastTapTime = 0
                        onDoubleTap()
                    } else {
                        // 单击：先记录，等待300ms后若没有第二次则触发单击
                        if (lastTapTime != 0L && now - lastTapTime >= 300) {
                            // 前一次的单击已超时，直接触发
                            lastTapTime = 0
                            onSingleTap()
                        } else {
                            lastTapTime = now
                            // 延迟触发单击，若期间出现双击则取消
                            webView?.postDelayed({
                                if (lastTapTime != 0L && System.currentTimeMillis() - lastTapTime >= 300) {
                                    lastTapTime = 0
                                    onSingleTap()
                                }
                            }, 320)
                        }
                    }
                }
                isDragging = false
            }
        }
        return true
    }

    private fun threshold(): Int = dp(6)

    fun dismiss() {
        try {
            wm.removeView(overlayView)
        } catch (_: Exception) {
        }
        webView?.destroy()
        webView = null
    }

    private fun dp(v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()
}