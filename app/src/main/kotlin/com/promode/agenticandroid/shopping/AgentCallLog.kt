package com.promode.agenticandroid.shopping

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A small, persisted event log for the "Agent calls" tab.
 *
 * It's written from Application.onCreate and from each app function, so after an agent call
 * you can open the app and see what ran, on which thread, and in which process. It's kept in
 * SharedPreferences because the process an agent started may be gone by the time you look.
 */
class AgentCallLog private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("agent_call_log", Context.MODE_PRIVATE)
    private val _events = MutableStateFlow(load())

    /** Newest first. */
    val events: StateFlow<List<String>> = _events.asStateFlow()

    @Synchronized
    fun record(message: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val updated = (listOf("$time  $message") + _events.value).take(MAX_EVENTS)
        prefs.edit().putString(KEY_EVENTS, updated.joinToString(SEPARATOR)).commit()
        _events.value = updated
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_EVENTS).commit()
        _events.value = emptyList()
    }

    private fun load(): List<String> =
        prefs.getString(KEY_EVENTS, null)?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()

    companion object {
        private const val KEY_EVENTS = "events"
        private const val SEPARATOR = "\n"
        private const val MAX_EVENTS = 30

        @Volatile private var instance: AgentCallLog? = null

        fun get(context: Context): AgentCallLog =
            instance ?: synchronized(this) {
                instance ?: AgentCallLog(context.applicationContext).also { instance = it }
            }
    }
}
