package com.budgeyet.core.network.mapper

import com.budgeyet.core.model.DisplayMode
import com.budgeyet.core.model.User
import com.budgeyet.core.network.dto.DisplayModeDto
import com.budgeyet.core.network.dto.UserResponseDto

fun UserResponseDto.toDomain(): User = User(
    id = id,
    email = email,
    fullName = fullName,
    nickname = nickname,
    displayMode = displayMode.toDomain()
    // pushNotificationsEnabled has no backend field yet — keeps its default (true).
)

fun DisplayModeDto.toDomain(): DisplayMode = when (this) {
    DisplayModeDto.LIGHT -> DisplayMode.LIGHT
    DisplayModeDto.DARK -> DisplayMode.DARK
    DisplayModeDto.SYSTEM -> DisplayMode.SYSTEM
}

fun DisplayMode.toDto(): DisplayModeDto = when (this) {
    DisplayMode.LIGHT -> DisplayModeDto.LIGHT
    DisplayMode.DARK -> DisplayModeDto.DARK
    DisplayMode.SYSTEM -> DisplayModeDto.SYSTEM
}
