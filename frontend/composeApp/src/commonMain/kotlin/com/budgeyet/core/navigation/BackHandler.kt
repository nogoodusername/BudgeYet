package com.budgeyet.core.navigation

import androidx.compose.runtime.Composable

// Intercepts the platform back gesture/button so it drives our hand-rolled back stacks
// (OnboardingNavController, AppNavController) instead of falling through to the OS default,
// which on Android finishes the Activity outright regardless of in-app navigation state.
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
