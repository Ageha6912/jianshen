package com.jianshen.fitness.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 组间休息倒计时。进程内单例,训练页横幅驱动它。 */
object RestTimer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _remaining = MutableStateFlow<Int?>(null)
    private val _total = MutableStateFlow<Int?>(null)
    private val _finishedNaturally = MutableStateFlow(false)

    /** 非 null 表示计时中(剩余秒数),null 表示空闲。 */
    val remaining: StateFlow<Int?> = _remaining

    /** 本轮休息总时长(秒),null 表示空闲。供横幅进度条用。 */
    val total: StateFlow<Int?> = _total

    /** 倒计时自然走完(非手动跳过)时置 true,由 UI 消费后调用 clearFinished()。 */
    val finishedNaturally: StateFlow<Boolean> = _finishedNaturally

    fun clearFinished() {
        _finishedNaturally.value = false
    }

    fun start(seconds: Int) {
        job?.cancel()
        _remaining.value = seconds
        _total.value = seconds
        _finishedNaturally.value = false
        job = scope.launch {
            while (true) {
                delay(1000)
                val next = (_remaining.value ?: break) - 1
                if (next <= 0) {
                    _finishedNaturally.value = true
                    break
                }
                _remaining.value = next
            }
            _remaining.value = null
            _total.value = null
        }
    }

    fun skip() {
        job?.cancel()
        _remaining.value = null
        _total.value = null
    }

    fun add15() {
        _remaining.value = (_remaining.value ?: 0) + 15
        _total.value = (_total.value ?: 0) + 15
    }
}

private const val CHANNEL_REST = "rest_timer"
private const val NOTIFICATION_ID_REST = 1

fun restSecondsPref(context: Context): Int =
    context.getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        .getInt("rest_seconds", 90)

fun saveRestSecondsPref(context: Context, seconds: Int) {
    context.getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        .edit().putInt("rest_seconds", seconds).apply()
}

fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

fun postRestNotification(context: Context, seconds: Int) {
    if (!canPostNotifications(context)) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= 26) {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REST, "组间休息计时", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
    val notification = NotificationCompat.Builder(context, CHANNEL_REST)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("组间休息")
        .setContentText("休息 $seconds 秒,别急着下一组")
        .setColor(android.graphics.Color.rgb(201, 185, 143)) // 白金强调色,染色主题通知
        .setOngoing(true)
        .build()
    manager.notify(NOTIFICATION_ID_REST, notification)
}

fun cancelRestNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.cancel(NOTIFICATION_ID_REST)
}

/** 休息结束提示:闹钟音频流响三声 + 双段震动。 */
fun playRestAlarm(context: Context) {
    try {
        val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
        }, 550)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
        }, 1100)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, 1800)
    } catch (_: RuntimeException) {
        // 部分设备 ToneGenerator 资源紧张会抛异常,忽略即可,还有震动兜底
    }
    val vibrator = if (Build.VERSION.SDK_INT >= 31) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 180, 400), -1))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(longArrayOf(0, 400, 180, 400), -1)
    }
}
