package com.famex.core.navigation

import androidx.compose.runtime.mutableStateListOf

// Hand-rolled sealed-Screen + back-stack navigation — avoids pulling in
// androidx.navigation.compose against this project's pinned Kotlin/Compose Multiplatform versions.
class AppNavController(startScreen: Screen = Screen.Dashboard) {
    private val backStack = mutableStateListOf(startScreen)

    val current: Screen get() = backStack.last()
    val canGoBack: Boolean get() = backStack.size > 1

    /** Push a new screen onto the stack (detail views, add-transaction). */
    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    /** Reset the stack to a single top-level tab — used by bottom navigation. */
    fun switchTab(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }

    fun back(): Boolean {
        if (!canGoBack) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }
}
