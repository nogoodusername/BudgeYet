package com.budgeyet.core.model

import kotlinx.serialization.Serializable

enum class DisplayMode { LIGHT, DARK, SYSTEM }

@Serializable
data class User(
    val id: Long,
    val email: String,
    val fullName: String,
    val nickname: String,
    val displayMode: DisplayMode = DisplayMode.SYSTEM,
    val pushNotificationsEnabled: Boolean = true
)
