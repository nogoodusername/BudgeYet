package com.budgeyet

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

@OptIn(ExperimentalComposeApi::class)
fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = {
        // Default is OnFocusBehavior.FocusableAboveKeyboard, which pans the whole Compose scene
        // up when a field is focused. That only lifts the focused field — it never reveals what's
        // below it (the "Save Changes" button on Category Limits, etc.) — and it fights our own
        // keyboardAwarePadding()/scroll handling in App.kt + OnboardingRoute. DoNothing hands
        // keyboard avoidance entirely back to Compose layout, which those modifiers implement via
        // LocalKeyboardOverlapHeight (CMP 1.6.x has no WindowInsets.ime on iOS).
        onFocusBehavior = OnFocusBehavior.DoNothing
    }
) {
    App()
}
