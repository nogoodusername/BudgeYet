package com.budgeyet.core.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

// Keyboard-dismiss helpers shared across screens.
//
// Two things every screen needs for good keyboard UX:
//  1. A way to dismiss the keyboard when the user taps outside a field (common
//     tap-outside-to-hide), and
//  2. An IME "Done" action on fields so the keyboard itself exposes a dismiss
//     control — numeric keypads (money/limit/amount fields) have no built-in
//     dismiss button on iOS, which is why the category-limit fields get "stuck".

/** Clears text-field focus and hides the keyboard when the user taps anywhere on the
 *  modified node that isn't consumed by a child. Observed in the FINAL pointer pass, so taps
 *  that a child consumed (a text field receiving focus, a button, a scroll container's drag)
 *  are skipped — tapping a field still focuses it; tapping the background or a non-interactive
 *  part of a scrollable list dismisses the keyboard. Apply to a screen's root or its scrollable
 *  content. */
fun Modifier.dismissKeyboardOnTap(focusManager: FocusManager, keyboardController: SoftwareKeyboardController?): Modifier =
    this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Final)
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

/** [dismissKeyboardOnTap] wired up from the composition's own focus manager + keyboard
 *  controller — the idiomatic call-site for a screen root. */
@Composable
fun Modifier.dismissKeyboardOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return this.dismissKeyboardOnTap(focusManager, keyboardController)
}

/** [KeyboardOptions] + [KeyboardActions] that give a field an explicit IME "Done" button wired
 *  to clear focus and hide the keyboard. [onDone] runs first when the user taps Done. */
@Composable
fun dismissKeyboardOptions(
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {}
): Pair<KeyboardOptions, KeyboardActions> {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(imeAction, onDone, focusManager, keyboardController) {
        KeyboardOptions(imeAction = imeAction) to KeyboardActions(
            onDone = {
                onDone()
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        )
    }
}
