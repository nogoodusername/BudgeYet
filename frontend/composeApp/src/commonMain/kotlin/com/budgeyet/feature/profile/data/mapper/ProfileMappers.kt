package com.budgeyet.feature.profile.data.mapper

import com.budgeyet.core.model.PendingInvite
import com.budgeyet.feature.profile.data.remote.dto.InviteResponseDto

fun InviteResponseDto.toDomain(): PendingInvite = PendingInvite(id = id, email = email.orEmpty())
