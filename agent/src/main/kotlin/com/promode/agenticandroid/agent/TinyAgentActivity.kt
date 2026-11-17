package com.promode.agenticandroid.agent

import android.Manifest
import android.app.Activity
import android.app.appfunctions.AppFunctionException
import android.app.appfunctions.AppFunctionManager
import android.app.appfunctions.ExecuteAppFunctionRequest
import android.app.appfunctions.ExecuteAppFunctionResponse
import android.app.appsearch.GenericDocument
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import android.util.TypedValue
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * A third-party "agent": an ordinary app that asks for EXECUTE_APP_FUNCTIONS and tries to call
 * the shopping app's addItem function, the same call the Google app or an adb shell can make.
 *
 * Launch it, or run it from adb:
 *   adb shell am start -n com.promode.agenticandroid.agent/.TinyAgentActivity --ez run true
 * and read the outcome with: adb logcat -s TinyAgent
 */
class TinyAgentActivity : Activity() {

    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply {
            text = "Tiny Agent"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setTextColor(0xFF1A1A2E.toInt())
        }
        val subtitle = TextView(this).apply {
            text = "An ordinary installed app that asks for EXECUTE_APP_FUNCTIONS."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(4), 0, dp(16))
        }
        val button = Button(this).apply {
            text = "Ask the shopping app to add \"coffee\""
            setOnClickListener { callAddItem() }
        }
        output = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(0, dp(16), 0, 0)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(subtitle)
            addView(button)
            addView(output)
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(0xFFFFFFFF.toInt())
            clipToPadding = false
            addView(content)
            // targetSdk 37 draws the app edge to edge, so keep the content clear of the
            // status bar, navigation bar and any display cutout.
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
                val margin = dp(24)
                view.setPadding(bars.left + margin, bars.top + margin, bars.right + margin, bars.bottom + margin)
                insets
            }
        })
        // Light background, so ask for dark status and navigation bar icons.
        window.insetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
        )
        if (intent.getBooleanExtra("run", false)) callAddItem()
    }

    private fun callAddItem() {
        val granted = checkSelfPermission(Manifest.permission.EXECUTE_APP_FUNCTIONS) ==
            PackageManager.PERMISSION_GRANTED
        show("EXECUTE_APP_FUNCTIONS granted: $granted")

        val request = ExecuteAppFunctionRequest.Builder(
            "com.promode.agenticandroid.shopping",
            "com.promode.agenticandroid.shopping.BaseShoppingFunctionService#addItem",
        ).setParameters(
            GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "")
                .setPropertyString("itemName", "coffee")
                .build()
        ).build()

        getSystemService(AppFunctionManager::class.java).executeAppFunction(
            request,
            mainExecutor,
            CancellationSignal(),
            object : OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> {
                override fun onResult(result: ExecuteAppFunctionResponse) {
                    show("Result: ${result.resultDocument}")
                }

                override fun onError(error: AppFunctionException) {
                    show("Error ${error.errorCode}: ${error.errorMessage}")
                }
            },
        )
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    private fun show(line: String) {
        Log.i(TAG, line)
        output.append(line + "\n")
    }

    private companion object {
        const val TAG = "TinyAgent"
    }
}
