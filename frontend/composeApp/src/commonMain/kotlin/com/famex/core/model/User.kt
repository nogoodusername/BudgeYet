package com.famex.core.model

enum class DisplayMode { LIGHT, DARK, SYSTEM }

data class User(
    val id: Long,
    val email: String,
    val fullName: String,
    val nickname: String,
    val displayMode: DisplayMode = DisplayMode.SYSTEM,
    val pushNotificationsEnabled: Boolean = true
)
