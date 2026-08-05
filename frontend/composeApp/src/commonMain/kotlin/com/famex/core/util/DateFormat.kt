package com.famex.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

private val monthAbbreviations = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

private val monthFullNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

fun todayLocalDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun LocalDate.toDisplayText(): String = "${monthAbbreviations[monthNumber - 1]} $dayOfMonth, $year"

// "August 2026" — used to default the onboarding Budget Goal screen's name/period fields to
// the current month, matching the Stitch mockup's placeholder values.
fun LocalDate.toMonthYearText(): String = "${monthFullNames[monthNumber - 1]} $year"

fun currentMonthYearLabel(): String = todayLocalDate().toMonthYearText()

// "Jan 2026" — used for HouseholdMember.joinedAtText, matching the abbreviated style the fake
// repo's seed data already used (see FakeAuthRepository.joinableHousehold).
fun LocalDate.toShortMonthYearText(): String = "${monthAbbreviations[monthNumber - 1]} $year"

// Backend datetime fields (e.g. HouseholdMemberResponse.joined_at) serialize as naive ISO-8601
// local date-times (no offset/zone — see SQLAlchemy's plain DateTime columns), so this parses
// via LocalDateTime rather than Instant, which requires an offset. Falls back to today on a
// malformed string rather than crashing a screen over a display-only label.
fun parseIsoDateTimeToLocalDate(isoDateTime: String): LocalDate =
    try {
        LocalDateTime.parse(isoDateTime).date
    } catch (e: Exception) {
        todayLocalDate()
    }

// Material3's DatePickerState works in UTC epoch millis at start-of-day — these bridge that
// representation to/from the plain LocalDate the rest of the app uses.
fun LocalDate.toUtcEpochMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

fun epochMillisToLocalDate(epochMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.UTC).date
