package com.famex.feature.profile.data.mapper

import com.famex.core.model.PendingInvite
import com.famex.feature.profile.data.remote.dto.InviteResponseDto

fun InviteResponseDto.toDomain(): PendingInvite = PendingInvite(id = id, email = email.orEmpty())
