package com.fintrack.kavindu.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fintrack.kavindu.core.model.Goal
import java.time.LocalDate

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetCents: Long,
    val deadline: LocalDate,
    val startingSavingsCents: Long,
    val startDate: LocalDate,
    val createdAt: Long,
)

fun GoalEntity.toModel() = Goal(
    id = id,
    name = name,
    targetCents = targetCents,
    deadline = deadline,
    startingSavingsCents = startingSavingsCents,
    startDate = startDate,
    createdAt = createdAt,
)

fun Goal.toEntity() = GoalEntity(
    id = id,
    name = name,
    targetCents = targetCents,
    deadline = deadline,
    startingSavingsCents = startingSavingsCents,
    startDate = startDate,
    createdAt = createdAt,
)
