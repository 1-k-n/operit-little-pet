package com.operit.pet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 悬浮窗前台服务。
 * 保活 + 拉起悬浮窗 + 接入感知系统与 Supabase 推送。
 */
class FloatPetService : Service() {

    companion object {
        private const val NOTIF_ID = 1001
        private const val CHANNEL_ID = "operit_pet"
        private const val POLL_MS = 5_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var renderer: PetRenderer? = null
    private var perception: PetPerception? = null
    private var supabase: SupabaseClient? = null
    private var lastTs = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())

        renderer = PetRenderer(this)
        renderer?.show(
            onDoubleTap = {
                // 双击：呼出单点菜单
                renderer?.sendToPet("window.pet && window.pet.dblPet();")
            },
            onSingleTap = {
                // 单击：换表情 + 亲昵交互
                renderer?.sendToPet("window.pet && window.pet.tapPet();")
            }
        )

        // 感知系统
        perception = PetPerception(this)
        perception?.start { js ->
            renderer?.sendToPet(js)
        }

        // Supabase 推送轮询
        supabase = SupabaseClient(
            SupabaseClient.SUPABASE_URL,
            SupabaseClient.SUPABASE_ANON_KEY
        )
        startPolling()
    }

    private fun startPolling() {
        scope.launch {
            while (true) {
                try {
                    val payload = supabase?.fetchLatest()
                    val ts = payload?.optString("ts", "") ?: ""
                    if (payload != null && ts != lastTs) {
                        lastTs = ts
                        val emotion = payload.optString("emotion", "")
                        val bubble = payload.optString("bubble", "")
                        if (emotion.isNotEmpty()) {
                            renderer?.sendToPet("window.pet && window.pet.setEmotion('$emotion');")
                        }
                        if (bubble.isNotEmpty()) {
                            renderer?.sendToPet("window.pet && window.pet.showBubble('$bubble');")
                        }
                    }
                } catch (_: Exception) {
                }
                kotlinx.coroutines.delay(POLL_MS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        perception?.stop()
        renderer?.dismiss()
        renderer = null
        scope.cancel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.float_service_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.float_service_title))
            .setContentText(getString(R.string.float_service_text))
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setOngoing(true)
            .build()
}