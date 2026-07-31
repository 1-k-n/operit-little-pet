package com.operit.pet

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import java.util.Calendar

/**
 * 感知系统。
 * 周期性读取时间/天气等状态，决定小精灵当前表情与行为。
 * 当前内置：时间(早/午/晚)、日期；后续可扩展位置/传感器。
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

        return "window.pet && window.pet.perceive({time:'$timeStr',date:'$dateStr',period:'$period'});"
    }

    companion object {
        private const val REFRESH_MS = 60_000L // 每分钟感知一次
    }
}