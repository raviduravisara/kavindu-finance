package com.fintrack.kavindu.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fintrack.kavindu.core.model.FreelanceProject

@Entity(tableName = "freelance_projects")
data class FreelanceProjectEntity(
    @PrimaryKey val id: String,
    val clientName: String,
    val projectName: String,
    val createdAt: Long,
)

fun FreelanceProjectEntity.toModel() = FreelanceProject(
    id = id,
    clientName = clientName,
    projectName = projectName,
    createdAt = createdAt,
)

fun FreelanceProject.toEntity() = FreelanceProjectEntity(
    id = id,
    clientName = clientName,
    projectName = projectName,
    createdAt = createdAt,
)
