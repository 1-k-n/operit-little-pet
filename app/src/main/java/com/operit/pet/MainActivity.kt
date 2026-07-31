package com.operit.pet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 小精灵入口页。
 * 负责：申请悬浮窗权限、通知权限、启动/关闭前台服务。
 */
class MainActivity : AppCompatActivity() {

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (canDrawOverlays()) {
                startPet()
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && canDrawOverlays()) startPet() else requestOverlay()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            checkAndStart()
        }
        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            stopService(Intent(this, FloatPetService::class.java))
            Toast.makeText(this, "小精灵睡下了", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestOverlay()
        }
    }

    private fun requestOverlay() {
        if (!canDrawOverlays()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startPet()
        }
    }

    private fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    private fun startPet() {
        ContextCompat.startForegroundService(this, Intent(this, FloatPetService::class.java))
        Toast.makeText(this, "小精灵上线啦", Toast.LENGTH_SHORT).show()
    }

    // deprecated 但仅用于读取屏幕尺寸，无副作用
    @Suppress("DEPRECATION")
    fun screenSize(): Point {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getRealSize(point)
        return point
    }
}