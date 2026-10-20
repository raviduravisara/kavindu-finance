package com.fintrack.kavindu.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fintrack.kavindu.core.model.Currency
import com.fintrack.kavindu.core.model.Milestone
import com.fintrack.kavindu.core.model.MilestoneStatus
import java.time.LocalDate

@Entity(
    tableName = "milestones",
    indices = [Index("projectId"), Index("status", "dueDate")],
)
data class MilestoneEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val amountCents: Long,
    val currency: Currency,
    val dueDate: LocalDate,
    val status: MilestoneStatus,
    val incomeId: String?,
    val createdAt: Long,
)

fun MilestoneEntity.toModel() = Milestone(
    id = id,
    projectId = projectId,
    title = title,
    amountCents = amountCents,
    currency = currency,
    dueDate = dueDate,
    status = status,
    incomeId = incomeId,
    createdAt = createdAt,
)

fun Milestone.toEntity() = MilestoneEntity(
    id = id,
    projectId = projectId,
    title = title,
    amountCents = amountCents,
    currency = currency,
    dueDate = dueDate,
    status = status,
    incomeId = incomeId,
    createdAt = createdAt,
)
