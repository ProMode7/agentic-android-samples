package com.promode.agenticandroid.shopping

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app's only data: a shopping list kept in SharedPreferences.
 *
 * The UI and the AppFunctions service both use this one instance, so when an agent adds an
 * item through an app function, the list on screen updates too.
 */
class ShoppingList private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("shopping_list", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(load())

    /** The current items, in the order they were added. */
    val items: StateFlow<List<String>> = _items.asStateFlow()

    /** Adds [itemName] and returns how many items are on the list afterwards. */
    @Synchronized
    fun add(itemName: String): Int {
        val trimmed = itemName.trim()
        if (trimmed.isNotEmpty()) save(_items.value + trimmed)
        return _items.value.size
    }

    /** Removes the first item matching [itemName], ignoring case. Returns true if one was removed. */
    @Synchronized
    fun remove(itemName: String): Boolean {
        val index = _items.value.indexOfFirst { it.equals(itemName.trim(), ignoreCase = true) }
        if (index < 0) return false
        save(_items.value.toMutableList().apply { removeAt(index) })
        return true
    }

    private fun load(): List<String> =
        prefs.getString(KEY_ITEMS, null)?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()

    private fun save(items: List<String>) {
        prefs.edit().putString(KEY_ITEMS, items.joinToString(SEPARATOR)).apply()
        _items.value = items
    }

    companion object {
        private const val KEY_ITEMS = "items"
        private const val SEPARATOR = "\n"

        @Volatile private var instance: ShoppingList? = null

        fun get(context: Context): ShoppingList =
            instance ?: synchronized(this) {
                instance ?: ShoppingList(context.applicationContext).also { instance = it }
            }
    }
}
