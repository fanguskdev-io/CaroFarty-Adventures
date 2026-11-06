package com.fanguskdev.carofarty

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.fanguskdev.carofarty.audio.AndroidAudioFocus
import com.fanguskdev.carofarty.game.Scenario
import com.fanguskdev.carofarty.storage.AndroidKeyValueStore
import com.fanguskdev.carofarty.storage.Choices

/** The app for Android (MOB-03): one activity, in landscape and immersive full screen, drawing behind the cutout too. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        hideSystemBars()
        val store = AndroidKeyValueStore(applicationContext)
        val focus = AndroidAudioFocus(applicationContext)
        // `adb shell am start -n com.fanguskdev.carofarty/.MainActivity --ez debug true` opens the debugging tools at once,
        // `--ez autopilot true` lets the pilot play and `--ez silencio true` turns the sound off, to measure without it.
        // The build to measure (AUD-29) does the first two by itself, with the sound on and the screen kept awake.
        val measure = BuildConfig.MEASURE
        val tools = BuildConfig.DEBUG || measure
        val debugAtStart = measure || (BuildConfig.DEBUG && intent.getBooleanExtra("debug", false))
        val autopilot = measure || (BuildConfig.DEBUG && intent.getBooleanExtra("autopilot", false))
        val sound = !(BuildConfig.DEBUG && intent.getBooleanExtra("silencio", false))
        if (measure) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // The same runs in two measures, this and CaroFarty G's (GOD-27): `--es escenario nieve --es semilla 2026` plays
        // the first run there and with that seed, the next ones with the seeds after it.
        val scenario = if (tools) intent.getStringExtra("escenario")?.let { named(it) } else null
        if (scenario != null) Choices(store).scenario = scenario
        val firstSeed = if (tools) intent.getStringExtra("semilla")?.toLongOrNull() else null
        // The app that measures leaves its lines in the log (`adb logcat -s carofarty:I`), as CaroFarty G's measure.
        val log = if (measure) MeasureLog(System::nanoTime, ::refreshRate) { Log.i(LOG_TAG, it) } else null
        setContent {
            // The button to leave the game of the start screen (UI-22) closes the app and takes it out of the recent ones.
            App(
                store,
                tools,
                debugAtStart,
                autopilot,
                sound,
                quit = { finishAndRemoveTask() },
                focus = focus,
                measure = log,
                firstSeed = firstSeed,
            )
        }
    }

    /** The scenario called [name], as the launch gives it: «bosque», «nieve» or «desierto». */
    private fun named(name: String): Scenario? = Scenario.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }

    /** The screen's refresh rate now, for the app that measures. */
    private fun refreshRate(): Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display?.refreshRate ?: 0f
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.refreshRate
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private companion object {
        const val LOG_TAG = "carofarty"
    }
}
