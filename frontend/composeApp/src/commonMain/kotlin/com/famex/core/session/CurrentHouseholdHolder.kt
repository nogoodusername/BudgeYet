package com.famex.core.session

import com.famex.core.network.AppException

// App-wide "which household is the signed-in user currently in" — set by App.kt whenever the
// session changes (restored on cold start, set on onboarding complete, cleared on sign out) and
// read by Real*Repository implementations that need a household_id to scope their requests.
//
// This exists because domain repository interfaces (CategoryRepository, DashboardRepository,
// TransactionRepository, ...) deliberately don't take a household id parameter — they're shared
// across many call sites that were written before real networking existed, and v1 caps a user to
// a single household, so there's nothing for a per-call parameter to disambiguate. Every screen
// that reaches one of these repositories only renders inside MainAppShell, i.e. after onboarding
// has guaranteed a household exists — see App.kt.
//
// userId exists for the same session-context reason: presentation code that needs to know the
// current member's role (Profile/Household Members role-gating) derives it from the loaded
// household's members list by matching this id, so it must be kept in step with householdId.
class CurrentHouseholdHolder {
    var householdId: Long? = null
    var userId: Long? = null

    fun require(): Long = householdId
        ?: throw AppException.AuthenticationException("No active household. Please sign in again.")
}
