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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A third-party "agent": an ordinary app that asks for EXECUTE_APP_FUNCTIONS and tries to call
 * the shopping app's addItem function, the same call the Gemini app or an adb shell can make.
 *
 * Launch it, or run it from adb:
 *   adb shell am start -n com.promode.agenticandroid.agent/.TinyAgentActivity --ez run true
 * and read the outcome with: adb logcat -s TinyAgent
 */
class TinyAgentActivity : Activity() {

    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        output = TextView(this).apply { textSize = 15f }
        val button = Button(this).apply {
            text = "Ask the shopping app to add \"coffee\""
            setOnClickListener { callAddItem() }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 160, 48, 48)
            addView(button)
            addView(output)
        })
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

    private fun show(line: String) {
        Log.i(TAG, line)
        output.append(line + "\n")
    }

    private companion object {
        const val TAG = "TinyAgent"
    }
}
