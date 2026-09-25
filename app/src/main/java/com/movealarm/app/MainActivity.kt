package com.movealarm.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.movealarm.app.alarm.AlarmNotifier
import com.movealarm.app.alarm.AlarmScheduler
import com.movealarm.app.data.AlarmSettings
import com.movealarm.app.data.SettingsStore
import com.movealarm.app.photo.PhotoStore
import com.movealarm.app.ui.HomeClockScreen
import com.movealarm.app.ui.MoveColors
import com.movealarm.app.ui.MoveTheme
import com.movealarm.app.ui.PermissionState
import com.movealarm.app.ui.PhotosScreen
import com.movealarm.app.ui.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Screen { HOME, SETTINGS, PHOTOS }

class MainActivity : ComponentActivity() {

    private val resumeCount = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        AlarmNotifier.ensureChannel(this)
        setContent {
            MoveTheme {
                Box(Modifier.fillMaxSize().background(MoveColors.Background)) {
                    MoveApp(resumeCount.intValue)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 앱을 열 때마다 알람을 다시 등록해 둔다 (혹시 시스템이 지웠어도 복구되도록).
        AlarmScheduler.rescheduleHourly(this)
        resumeCount.intValue++
    }
}

@Composable
private fun MoveApp(resumeCount: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val photoStore = remember { PhotoStore(context) }

    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var settings by remember { mutableStateOf(settingsStore.load()) }
    var photos by remember { mutableStateOf(photoStore.list()) }
    var nextSequential by remember { mutableIntStateOf(settingsStore.sequentialNextIndex) }
    var importing by remember { mutableStateOf(false) }
    var permissions by remember { mutableStateOf(readPermissions(context)) }

    LaunchedEffect(resumeCount) {
        settings = settingsStore.load()
        photos = photoStore.list()
        nextSequential = settingsStore.sequentialNextIndex
        permissions = readPermissions(context)
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissions = readPermissions(context)
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !permissions.notificationsAllowed && !settingsStore.askedNotificationPermission
        ) {
            settingsStore.askedNotificationPermission = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            importing = true
            val failed = withContext(Dispatchers.IO) { uris.count { !photoStore.import(it) } }
            photos = photoStore.list()
            importing = false
            if (failed > 0) Toast.makeText(context, "사진 ${failed}장을 저장하지 못했어요", Toast.LENGTH_LONG).show()
        }
    }

    fun update(newSettings: AlarmSettings) {
        settings = newSettings
        settingsStore.save(newSettings)
        AlarmScheduler.rescheduleHourly(context)
    }

    val fixNotifications = { openNotificationSettings(context) }
    val fixExactAlarm = { openExactAlarmSettings(context) }

    BackHandler(enabled = screen != Screen.HOME) {
        screen = if (screen == Screen.PHOTOS) Screen.SETTINGS else Screen.HOME
    }

    when (screen) {
        Screen.HOME -> HomeClockScreen(
            settings = settings,
            permissions = permissions,
            onOpenSettings = { screen = Screen.SETTINGS },
            onFixNotifications = fixNotifications,
            onFixExactAlarm = fixExactAlarm,
        )
        Screen.SETTINGS -> SettingsScreen(
            settings = settings,
            photos = photos,
            permissions = permissions,
            onBack = { screen = Screen.HOME },
            onChange = ::update,
            onOpenPhotos = { screen = Screen.PHOTOS },
            onTestAlarm = {
                AlarmScheduler.scheduleTest(context)
                val msg = if (permissions.notificationsAllowed) "1분 뒤에 테스트 알람이 울려요.\n화면을 끄고 기다려 보세요."
                else "알림이 꺼져 있어서 테스트 알람이 보이지 않아요. 아래 안내를 눌러 알림을 켜주세요."
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            },
            onFixNotifications = fixNotifications,
            onFixExactAlarm = fixExactAlarm,
        )
        Screen.PHOTOS -> PhotosScreen(
            photos = photos,
            order = settings.photoOrder,
            nextSequentialIndex = nextSequential,
            importing = importing,
            onBack = { screen = Screen.SETTINGS },
            onAdd = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onDelete = {
                photoStore.delete(it)
                photos = photoStore.list()
            },
            onOrderChange = { update(settings.copy(photoOrder = it)) },
        )
    }
}

private const val MAX_PICK = 10

private fun readPermissions(context: Context) = PermissionState(
    notificationsAllowed = AlarmNotifier.canPostNotifications(context),
    exactAlarmAllowed = AlarmScheduler.canScheduleExact(context),
)

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    runCatching { context.startActivity(intent) }.onFailure { openAppDetails(context) }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        runCatching { context.startActivity(intent) }.onFailure { openAppDetails(context) }
    } else {
        openAppDetails(context)
    }
}

private fun openAppDetails(context: Context) {
    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
}
