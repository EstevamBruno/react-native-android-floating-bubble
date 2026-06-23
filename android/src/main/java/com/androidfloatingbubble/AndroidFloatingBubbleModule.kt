package com.androidfloatingbubble

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext

class AndroidFloatingBubbleModule(reactContext: ReactApplicationContext) :
  NativeAndroidFloatingBubbleSpec(reactContext) {

  private val context: Context
    get() = reactApplicationContext

  override fun isSupported(): Boolean = true

  override fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

  override fun requestOverlayPermission() {
    if (Settings.canDrawOverlays(context)) return
    try {
      val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
      ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    } catch (_: Exception) {
    }
  }

  override fun setIcons(bubbleIcon: String, notificationIcon: String) {
    context.getSharedPreferences(FloatingBubbleService.PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(FloatingBubbleService.PREF_KEY_BUBBLE_ICON, bubbleIcon)
      .putString(FloatingBubbleService.PREF_KEY_NOTIFICATION_ICON, notificationIcon)
      .apply()
  }

  override fun show(badgeCount: Double) {
    val granted = Settings.canDrawOverlays(context)
    Log.d(TAG, "show(badgeCount=$badgeCount) overlayGranted=$granted")
    if (!granted) {
      Log.w(TAG, "show() aborted: overlay permission not granted")
      return
    }
    try {
      val intent = Intent(context, FloatingBubbleService::class.java).apply {
        action = FloatingBubbleService.ACTION_START
        putExtra(FloatingBubbleService.EXTRA_BADGE_COUNT, badgeCount.toInt())
      }
      val comp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
      Log.d(TAG, "show() started service -> $comp")
    } catch (e: RuntimeException) {
      Log.e(TAG, "show() failed to start service", e)
    }
  }

  override fun hide() {
    try {
      val intent = Intent(context, FloatingBubbleService::class.java).apply {
        action = FloatingBubbleService.ACTION_STOP
      }
      context.startService(intent)
    } catch (_: Exception) {
    }
  }

  companion object {
    const val NAME = NativeAndroidFloatingBubbleSpec.NAME
    private const val TAG = "FloatingBubble"
  }
}
