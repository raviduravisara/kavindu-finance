package com.fintrack.kavindu.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fintrack.kavindu.core.model.CryptoDirection
import com.fintrack.kavindu.core.model.CryptoTransfer
import java.time.LocalDate

@Entity(tableName = "crypto_transfers", indices = [Index("date")])
data class CryptoTransferEntity(
    @PrimaryKey val id: String,
    val direction: CryptoDirection,
    val amountCents: Long,
    val date: LocalDate,
    val note: String?,
    val createdAt: Long,
)

fun CryptoTransferEntity.toModel() = CryptoTransfer(
    id = id,
    direction = direction,
    amountCents = amountCents,
    date = date,
    note = note,
    createdAt = createdAt,
)

fun CryptoTransfer.toEntity() = CryptoTransferEntity(
    id = id,
    direction = direction,
    amountCents = amountCents,
    date = date,
    note = note,
    createdAt = createdAt,
)
