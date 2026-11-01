package com.promode.agenticandroid.shopping

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

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
    internal fun addItem(itemName: String): Long = shoppingList.add(itemName).toLong()

    /**
     * Removes an item from the user's shopping list. Matching ignores case.
     *
     * @param itemName The item to remove, for example "oat milk".
     * @return True if the item was on the list and has been removed, false if it wasn't there.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    internal fun removeItem(itemName: String): Boolean = shoppingList.remove(itemName)

    /**
     * Returns every item currently on the user's shopping list, in the order they were added.
     *
     * @return The item names. Empty if the list is empty.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    internal fun getItems(): List<String> = shoppingList.items.value
}
