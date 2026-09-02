package com.budgeyet.core.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Adds bottom padding equal to the height the on-screen keyboard currently overlaps the app,
 * so a scrollable screen can bring fields/buttons above the keyboard instead of leaving them
 * covered.
 *
 * Platform split (see the `actual`s): Android uses `Modifier.imePadding()`. **iOS can't** —
 * Compose Multiplatform 1.6.x does not wire `WindowInsets.ime` on iOS at all (that landed in
 * 1.7), so `imePadding()` is a no-op there. The iOS actual instead reads the keyboard height
 * from `LocalKeyboardOverlapHeight` and pads with it. This only produces the right result when
 * Compose's own scene-panning is turned off (`OnFocusBehavior.DoNothing` in MainViewController)
 * — otherwise the scene pans *and* we pad, double-compensating.
 */
expect fun Modifier.keyboardAwarePadding(): Modifier

/**
 * Dismisses the on-screen keyboard when the user taps any non-interactive area of the container.
 *
 * iOS has no hardware Back button, so a keyboard opened by a text field on a page like Category
 * Limits would otherwise stay up indefinitely, covering the Save button and the lower rows with
 * no way to get rid of it.
 *
 * Runs its own gesture loop instead of `detectTapGestures`: it waits for the *whole* gesture to
 * finish and only dismisses if the terminating "up" was never consumed — i.e. the tap landed on
 * empty space, not on a text field, button, list row, or a scroll. `detectTapGestures` here was
 * unreliable because its default `awaitFirstDown(requireUnconsumed = true)` bailed whenever the
 * `LazyColumn` child claimed the initial down.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.dismissKeyboardOnTap(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val up = waitForUpOrCancellation()
            if (up != null && !up.isConsumed) {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            }
        }
    }
}
