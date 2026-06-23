package com.androidfloatingbubble

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.util.TypedValue
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.abs

class FloatingBubbleService : Service() {

  companion object {
    const val ACTION_START = "com.androidfloatingbubble.ACTION_START"
    const val ACTION_STOP = "com.androidfloatingbubble.ACTION_STOP"
    const val EXTRA_BADGE_COUNT = "badge_count"
    // Sentinel value: "keep current count" — used when show() is called without
    // an explicit number (toggle/bootstrap) so the existing badge is not reset.
    // MUST match KEEP_BADGE_COUNT on the JS side.
    const val KEEP_BADGE_COUNT = -1
    const val PREFS_NAME = "floating_bubble_prefs"
    const val PREF_KEY_BUBBLE_ICON = "bubble_icon_name"
    const val PREF_KEY_NOTIFICATION_ICON = "notification_icon_name"

    private const val TAG = "FloatingBubble"
    private const val CHANNEL_ID = "floating_bubble_channel_v2"
    private const val NOTIFICATION_ID = 9001
    private const val BUBBLE_SIZE_DP = 56f
    private const val BADGE_SIZE_DP = 20f
    private const val BADGE_TEXT_SP = 11f
    private const val BADGE_MAX = 99
    private const val BADGE_COLOR = 0xFFE53935.toInt()
    private const val EDGE_MARGIN_PX = 16
    private const val INITIAL_Y = 200
    private const val TAP_THRESHOLD_PX = 10
    private const val SNAP_DURATION_MS = 250L
    private const val PREF_KEY_X = "bubble_x"
    private const val PREF_KEY_Y = "bubble_y"
  }

  private var windowManager: WindowManager? = null
  private var bubbleView: View? = null
  private var badgeView: TextView? = null
  private var badgeCount = 0
  private var snapAnimator: ValueAnimator? = null
  private var activeActivityCount = 0

  private val prefs: SharedPreferences by lazy {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
    override fun onActivityStarted(activity: Activity) {
      activeActivityCount++
      Log.d(TAG, "onActivityStarted ${activity.localClassName} count=$activeActivityCount")
      if (activeActivityCount == 1) hideBubbleView()
    }

    override fun onActivityStopped(activity: Activity) {
      activeActivityCount = (activeActivityCount - 1).coerceAtLeast(0)
      Log.d(TAG, "onActivityStopped ${activity.localClassName} count=$activeActivityCount")
      if (activeActivityCount == 0) showBubbleView()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "onCreate")
    windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    createNotificationChannel()
    application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d(TAG, "onStartCommand action=${intent?.action} activeCount=$activeActivityCount")
    if (intent?.action == ACTION_STOP) {
      stopSelf()
      return START_NOT_STICKY
    }

    val incoming = intent?.getIntExtra(EXTRA_BADGE_COUNT, KEEP_BADGE_COUNT) ?: KEEP_BADGE_COUNT
    if (incoming != KEEP_BADGE_COUNT) {
      badgeCount = incoming.coerceAtLeast(0)
      applyBadge()
    }

    // Satisfies the startForegroundService() contract on EVERY start.
    // onCreate() only runs the first time; while the service is alive, repeated
    // startForegroundService() calls only run onStartCommand(). Without calling
    // startForeground() here, the system throws RemoteServiceException
    // ("did not then call Service.startForeground()") ~10s later.
    promoteToForeground()
    return START_NOT_STICKY
  }

  private fun promoteToForeground() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(
        NOTIFICATION_ID,
        buildSilentNotification(),
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
      )
    } else {
      startForeground(NOTIFICATION_ID, buildSilentNotification())
    }

    // Remove the notification immediately — the overlay window keeps the
    // process alive. startForeground() is only called to satisfy Android.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      stopForeground(STOP_FOREGROUND_REMOVE)
    } else {
      @Suppress("DEPRECATION")
      stopForeground(true)
    }
  }

  override fun onDestroy() {
    application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
    hideBubbleView()
    super.onDestroy()
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    stopSelf()
    super.onTaskRemoved(rootIntent)
  }

  private fun showBubbleView() {
    Log.d(TAG, "showBubbleView() existing=${bubbleView != null} canOverlay=${android.provider.Settings.canDrawOverlays(this)}")
    if (bubbleView != null) return

    val sizePx = dpToPx(BUBBLE_SIZE_DP)

    @Suppress("DEPRECATION")
    val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      WindowManager.LayoutParams.TYPE_PHONE
    }

    val params = WindowManager.LayoutParams(
      sizePx,
      sizePx,
      overlayType,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
      PixelFormat.TRANSLUCENT,
    ).apply {
      gravity = Gravity.TOP or Gravity.START
      x = prefs.getInt(PREF_KEY_X, EDGE_MARGIN_PX)
      y = prefs.getInt(PREF_KEY_Y, INITIAL_Y)
    }

    val container = FrameLayout(this).apply {
      setOnTouchListener(buildDragTouchListener(params))
    }

    val icon = ImageView(this).apply {
      setImageResource(resolveBubbleIcon())
    }
    container.addView(
      icon,
      FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
      ),
    )

    val badge = buildBadgeView()
    val badgeSizePx = dpToPx(BADGE_SIZE_DP)
    container.addView(
      badge,
      FrameLayout.LayoutParams(badgeSizePx, badgeSizePx).apply {
        gravity = Gravity.TOP or Gravity.END
      },
    )
    badgeView = badge

    try {
      windowManager?.addView(container, params)
      bubbleView = container
      applyBadge()
      Log.d(TAG, "showBubbleView() addView OK at x=${params.x} y=${params.y} size=$sizePx")
    } catch (e: Exception) {
      Log.e(TAG, "showBubbleView() addView FAILED", e)
      bubbleView = null
      badgeView = null
    }
  }

  private fun buildBadgeView(): TextView {
    val background = GradientDrawable().apply {
      shape = GradientDrawable.OVAL
      setColor(BADGE_COLOR)
      setStroke(dpToPx(1.5f), Color.WHITE)
    }
    return TextView(this).apply {
      setTextColor(Color.WHITE)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, BADGE_TEXT_SP)
      typeface = Typeface.DEFAULT_BOLD
      gravity = Gravity.CENTER
      includeFontPadding = false
      this.background = background
    }
  }

  private fun applyBadge() {
    val badge = badgeView ?: return
    if (badgeCount > 0) {
      badge.text = if (badgeCount > BADGE_MAX) "$BADGE_MAX+" else badgeCount.toString()
      badge.visibility = View.VISIBLE
    } else {
      badge.visibility = View.GONE
    }
  }

  private fun dpToPx(dp: Float): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics
  ).toInt()

  private fun buildDragTouchListener(
    params: WindowManager.LayoutParams,
  ): View.OnTouchListener {
    var initialTouchX = 0f
    var initialTouchY = 0f
    var initialParamsX = 0
    var initialParamsY = 0

    return View.OnTouchListener { view, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          initialTouchX = event.rawX
          initialTouchY = event.rawY
          initialParamsX = params.x
          initialParamsY = params.y
          true
        }

        MotionEvent.ACTION_MOVE -> {
          params.x = initialParamsX + (event.rawX - initialTouchX).toInt()
          params.y = initialParamsY + (event.rawY - initialTouchY).toInt()
          safeUpdateLayout(view, params)
          true
        }

        MotionEvent.ACTION_UP -> {
          val movedX = abs(event.rawX - initialTouchX)
          val movedY = abs(event.rawY - initialTouchY)
          if (movedX < TAP_THRESHOLD_PX && movedY < TAP_THRESHOLD_PX) {
            openApp()
          } else {
            snapToNearestEdge(view, params)
          }
          true
        }

        else -> false
      }
    }
  }

  private fun safeUpdateLayout(view: View, params: WindowManager.LayoutParams) {
    try {
      windowManager?.updateViewLayout(view, params)
    } catch (_: Exception) {}
  }

  private fun hideBubbleView() {
    snapAnimator?.cancel()
    snapAnimator = null
    val view = bubbleView ?: return
    try {
      windowManager?.removeView(view)
    } catch (_: Exception) {
    }
    bubbleView = null
    badgeView = null
  }

  private fun snapToNearestEdge(view: View, params: WindowManager.LayoutParams) {
    val screenWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      windowManager?.currentWindowMetrics?.bounds?.width() ?: return
    } else {
      val size = Point()
      @Suppress("DEPRECATION")
      windowManager?.defaultDisplay?.getSize(size)
      size.x.takeIf { it > 0 } ?: return
    }

    val bubbleCenterX = params.x + view.width / 2
    val targetX = if (bubbleCenterX <= screenWidth / 2) {
      EDGE_MARGIN_PX
    } else {
      screenWidth - view.width - EDGE_MARGIN_PX
    }

    prefs.edit()
      .putInt(PREF_KEY_X, targetX)
      .putInt(PREF_KEY_Y, params.y)
      .apply()

    snapAnimator?.cancel()
    snapAnimator = ValueAnimator.ofInt(params.x, targetX).apply {
      duration = SNAP_DURATION_MS
      interpolator = DecelerateInterpolator()
      addUpdateListener { anim ->
        params.x = anim.animatedValue as Int
        safeUpdateLayout(view, params)
      }
      start()
    }
  }

  private fun openApp() {
    // The library does not know the host app's launcher Activity, so resolve it
    // via the package launch intent instead of referencing a concrete class.
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
      launchIntent.addFlags(
        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK,
      )
      try {
        startActivity(launchIntent)
      } catch (_: Exception) {
      }
    }
  }

  /**
   * Resolves a drawable id from a name persisted by the JS `setIcons` call,
   * looked up against the host app's resources. Falls back to the library's
   * bundled default when the name is empty or cannot be resolved.
   */
  private fun resolveBubbleIcon(): Int = resolveIcon(
    prefs.getString(PREF_KEY_BUBBLE_ICON, null),
    R.drawable.fb_bubble_default,
  )

  private fun resolveNotificationIcon(): Int = resolveIcon(
    prefs.getString(PREF_KEY_NOTIFICATION_ICON, null),
    R.drawable.fb_notification_default,
  )

  private fun resolveIcon(name: String?, fallback: Int): Int {
    if (!name.isNullOrBlank()) {
      var id = resources.getIdentifier(name, "drawable", packageName)
      if (id == 0) id = resources.getIdentifier(name, "mipmap", packageName)
      if (id != 0) return id
      Log.w(TAG, "Icon '$name' not found in host app resources; using default.")
    }
    return fallback
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
      CHANNEL_ID,
      "Floating Bubble",
      NotificationManager.IMPORTANCE_NONE,
    ).apply {
      setShowBadge(false)
      setSound(null, null)
      enableLights(false)
      enableVibration(false)
    }
    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
  }

  @Suppress("DEPRECATION")
  private fun buildSilentNotification(): Notification =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(resolveNotificationIcon())
        .build()
    } else {
      Notification.Builder(this)
        .setSmallIcon(resolveNotificationIcon())
        .setPriority(Notification.PRIORITY_MIN)
        .build()
    }
}
