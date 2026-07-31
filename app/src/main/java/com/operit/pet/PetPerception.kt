package com.operit.pet

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.format.DateFormat
import java.util.Calendar

/**
 * 感知系统。
 * 周期性读取时间/天气/电量/前台App 等状态，决定小精灵当前表情与行为。
 * 当前内置：时间(早/午/晚)、日期、电量、充电状态、前台应用。
 */
class PetPerception(private val context: Context) {
    fun interface Listener {
        /** 感知结果转成前端 JS，由回调抛出 */
        fun onPerceptionResult(js: String)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var listener: Listener? = null
    private var running = false

    fun start(l: Listener) {
        listener = l
        running = true
        tick()
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
        listener = null
    }

    private fun tick() {
        if (!running) return
        val js = buildPerceptionJs()
        listener?.onPerceptionResult(js)
        handler.postDelayed({ tick() }, REFRESH_MS)
    }

    private fun buildPerceptionJs(): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeStr = "%02d:%02d".format(hour, minute)
        val dateStr = DateFormat.format("M月d日 EEEE", cal).toString()
        val period = when (hour) {
            in 5..8 -> "early"    // 早起
            in 9..11 -> "morning" // 上午
            in 12..13 -> "noon"   // 午间
            in 14..17 -> "afternoon"
            in 18..22 -> "evening"
            else -> "night"
        }

        val batt = readBattery()
        val app = getFrontApp()

        return "window.pet && window.pet.perceive({time:'$timeStr',date:'$dateStr',period:'$period',battery:${batt.first},charging:${batt.second},app:'$app'});"
    }

    /** 读取电量(%) 与 是否充电 */
    private fun readBattery(): Pair<Int, Boolean> {
        val intent: Intent? = context.registerReceiver(
            null,
            android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        var percent = -1
        if (level >= 0 && scale > 0) {
            percent = (level * 100 / scale).toInt().coerceIn(0, 100)
        }
        if (percent < 0) percent = 50 // 兜底
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        return Pair(percent, charging)
    }

    /** 读取前台App包名（通过 UsageStatsManager），失败返回空串 */
    private fun getFrontApp(): String {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - 1000, now)
            var pkg = ""
            val e = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    pkg = e.packageName ?: ""
                }
            }
            pkg
        } catch (_: Exception) {
            // 无 UsageStats 权限或异常时静默失败
            ""
        }
    }

    companion object {
        private const val REFRESH_MS = 15_000L // 每15秒感知一次（电量/前台应用要更及时）
    }
}
