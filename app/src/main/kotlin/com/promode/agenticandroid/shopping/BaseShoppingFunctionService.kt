package com.promode.agenticandroid.shopping

import android.app.ActivityManager
import android.os.Process
import android.os.SystemClock
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The functions this app exposes to agents.
 *
 * The AppFunctions KSP compiler reads this class and generates two things:
 * - `ShoppingFunctionService`, a concrete subclass that routes each request to the right method.
 *   It's declared in AndroidManifest.xml.
 * - `assets/shopping_functions.xml`, the description of every function below. The system's
 *   AppSearch indexer reads that file out of the APK at install time, and that index is what
 *   agents search.
 *
 * With `isDescribedByKDoc = true`, each function's KDoc becomes the `<description>` an agent
 * receives, so it's written for a reader who sees nothing else.
 */
@AppFunctionServiceEntryPoint(
    serviceName = "ShoppingFunctionService",
    appFunctionXmlFileName = "shopping_functions",
)
abstract class BaseShoppingFunctionService : AppFunctionService() {

    private val shoppingList by lazy { ShoppingList.get(applicationContext) }

    /**
     * Adds an item to the user's shopping list.
     *
     * @param itemName The item to add, as the user said it, for example "oat milk".
     * @return The number of items on the list after adding.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    internal suspend fun addItem(itemName: String): Long {
        recordCall("addItem")
        // App functions are entered on the main thread. Move the write off it.
        return withContext(Dispatchers.IO) {
            AgentCallLog.get(applicationContext)
                .record("addItem writing on thread \"${Thread.currentThread().name}\"")
            shoppingList.add(itemName).toLong()
        }
    }

    /**
     * Removes an item from the user's shopping list. Matching ignores case.
     *
     * @param itemName The item to remove, for example "oat milk".
     * @return True if the item was on the list and has been removed, false if it wasn't there.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    internal fun removeItem(itemName: String): Boolean {
        recordCall("removeItem")
        return shoppingList.remove(itemName)
    }

    /**
     * Returns every item currently on the user's shopping list, in the order they were added.
     *
     * @return The item names. Empty if the list is empty.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    internal fun getItems(): List<String> {
        recordCall("getItems")
        return shoppingList.items.value
    }

    /**
     * Logs which thread and process ran the function, and stamps the first call of this process
     * into its ApplicationStartInfo so the "Agent calls" tab can show fork → bindApplication →
     * Application.onCreate → first app function on one timeline.
     */
    private fun recordCall(function: String) {
        AgentCallLog.get(applicationContext).record(
            "$function ran on thread \"${Thread.currentThread().name}\" in process ${Process.myPid()}"
        )
        if (!firstCallStamped) {
            firstCallStamped = true
            getSystemService(ActivityManager::class.java)
                .addStartInfoTimestamp(START_TIMESTAMP_FIRST_APP_FUNCTION, SystemClock.uptimeNanos())
        }
    }

    companion object {
        /** First developer-defined key in ApplicationStartInfo's timestamp range (21..30). */
        const val START_TIMESTAMP_FIRST_APP_FUNCTION = 21

        /** Per process: only the first function call after a process start is stamped. */
        @Volatile private var firstCallStamped = false
    }
}
