package com.operit.pet

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * Supabase 后端通信客户端。
 * 读取用户写入的状态表，并把后端推送的表情/气泡/反应拉回来。
 * 使用 REST + 轮询（也可换成 Realtime Channel，这里先给最稳的单表方案）。
 */
class SupabaseClient(
    private val baseUrl: String,
    private val anonKey: String
) {
    private val client = OkHttpClient()

    companion object {
        private val TAG = "SupabaseClient"
        // 请替换为真实 Supabase 项目 URL 和 anon key
        const val SUPABASE_URL = "https://YOUR-PROJECT.supabase.co"
        const val SUPABASE_ANON_KEY = "YOUR-ANON-KEY"
        private const val TABLE = "pet_state"
    }

    /**
     * 读取最新推送（供轮询用）。
     * @return 推送 JSON，形如 {"emotion":"happy","bubble":"...","ts":...}
     */
    suspend fun fetchLatest(): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/rest/v1/$TABLE" +
                    "?select=payload,created_at&order=created_at.desc&limit=1"
            val req = Request.Builder()
                .url(url)
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e(TAG, "fetch failed: ${resp.code} ${resp.body?.string()}")
                    return@withContext null
                }
                val body = resp.body?.string() ?: return@withContext null
                val arr = org.json.JSONArray(body)
                if (arr.length() == 0) return@withContext null
                val obj = arr.getJSONObject(0)
                val payload = obj.optString("payload", "")
                return@withContext if (payload.isNotBlank()) JSONObject(payload) else null
            }
        } catch (e: IOException) {
            Log.e(TAG, "network error", e)
            null
        }
    }

    /** 用户从悬浮窗状态条目里推入的本地测试数据也行——这里做一个写方法供以后扩展 */
    suspend fun pushState(key: String, value: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = "$baseUrl/rest/v1/$TABLE"
                val body = JSONObject()
                    .put("payload", JSONObject().put(key, value).toString())
                val req = okhttp3.Request.Builder()
                    .url(url)
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer $anonKey")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .post(okhttp3.RequestBody.create("application/json".toMediaType(), body.toString()))
                    .build()
                client.newCall(req).execute().use { }
            } catch (e: IOException) {
                Log.e(TAG, "push failed", e)
            }
        }
    }
}