package com.promode.agenticandroid.shopping

import android.app.Application
import android.os.Process

/**
 * Runs in every process start, including the ones an agent causes.
 *
 * When an agent calls an app function and this app isn't running, the system binds
 * ShoppingFunctionService with BIND_AUTO_CREATE. That starts the process, and this onCreate
 * runs before the function does, even though the user never opened the app.
 */
class ShoppingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (DemoSettings.isHeavyInitEnabled(this)) {
            // Demo only: stands in for the SDK and DI setup many apps do here.
            Thread.sleep(DemoSettings.HEAVY_INIT_MS)
        }
        AgentCallLog.get(this).record("Application.onCreate in process ${Process.myPid()}")
    }
}

/** Demo switches, set from the "Agent calls" tab. */
object DemoSettings {
    const val HEAVY_INIT_MS = 800L
    private const val PREFS = "demo_settings"
    private const val KEY_HEAVY_INIT = "heavy_init"

    fun isHeavyInitEnabled(context: android.content.Context): Boolean =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getBoolean(KEY_HEAVY_INIT, false)

    fun setHeavyInitEnabled(context: android.content.Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HEAVY_INIT, enabled).commit()
    }
}
