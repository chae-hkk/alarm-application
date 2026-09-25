package com.movealarm.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.movealarm.app.alarm.AlarmKind
import com.movealarm.app.alarm.AlarmNotifier
import com.movealarm.app.alarm.AlarmScheduler
import com.movealarm.app.ui.AlarmScreen
import com.movealarm.app.ui.MoveTheme
import java.io.File

/** 알람 알림을 누르거나(또는 화면이 꺼져 있을 때 자동으로) 뜨는 사진 화면 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render()
    }

    private fun render() {
        val photo = intent.getStringExtra(EXTRA_PHOTO)?.let(::File)?.takeIf { it.exists() }
        val kind = runCatching { AlarmKind.valueOf(intent.getStringExtra(EXTRA_KIND) ?: "") }.getOrDefault(AlarmKind.HOURLY)
        setContent {
            MoveTheme {
                AlarmScreen(
                    photo = photo,
                    kind = kind,
                    onConfirm = {
                        AlarmNotifier.cancel(this)
                        finish()
                    },
                    onSnooze = {
                        AlarmNotifier.cancel(this)
                        AlarmScheduler.scheduleSnooze(this)
                        Toast.makeText(this, "5분 뒤에 다시 알려드릴게요", Toast.LENGTH_LONG).show()
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_PHOTO = "photo"
        private const val EXTRA_KIND = "kind"

        fun intent(context: Context, photoPath: String?, kind: AlarmKind): Intent =
            Intent(context, AlarmActivity::class.java)
                .putExtra(EXTRA_PHOTO, photoPath)
                .putExtra(EXTRA_KIND, kind.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
