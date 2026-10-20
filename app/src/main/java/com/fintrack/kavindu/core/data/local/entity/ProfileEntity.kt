package com.fintrack.kavindu.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.UserProfile

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val email: String,
    val cycleStartDay: Int,
    val baseCurrency: Currency,
    val lastChannel: String?,
    val lastAccount: String?,
    val createdAt: Long,
)

fun ProfileEntity.toModel() = UserProfile(
    id = id,
    email = email,
    cycleStartDay = cycleStartDay,
    baseCurrency = baseCurrency,
    lastChannel = lastChannel,
    lastAccount = lastAccount,
    createdAt = createdAt,
)

fun UserProfile.toEntity() = ProfileEntity(
    id = id,
    email = email,
    cycleStartDay = cycleStartDay,
    baseCurrency = baseCurrency,
    lastChannel = lastChannel,
    lastAccount = lastAccount,
    createdAt = createdAt,
)
