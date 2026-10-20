package com.fintrack.kavindu.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fintrack.kavindu.core.data.local.dao.BalanceCheckDao
import com.fintrack.kavindu.core.data.local.dao.CategoryDao
import com.fintrack.kavindu.core.data.local.dao.CryptoTransferDao
import com.fintrack.kavindu.core.data.local.dao.ExpenseDao
import com.fintrack.kavindu.core.data.local.dao.FreelanceProjectDao
import com.fintrack.kavindu.core.data.local.dao.GoalDao
import com.fintrack.kavindu.core.data.local.dao.IncomeDao
import com.fintrack.kavindu.core.data.local.dao.MilestoneDao
import com.fintrack.kavindu.core.data.local.dao.ProfileDao
import com.fintrack.kavindu.core.data.local.dao.RecurringItemDao
import com.fintrack.kavindu.core.data.local.entity.BalanceCheckEntity
import com.fintrack.kavindu.core.data.local.entity.CategoryEntity
import com.fintrack.kavindu.core.data.local.entity.CryptoTransferEntity
import com.fintrack.kavindu.core.data.local.entity.ExpenseEntity
import com.fintrack.kavindu.core.data.local.entity.FreelanceProjectEntity
import com.fintrack.kavindu.core.data.local.entity.GoalEntity
import com.fintrack.kavindu.core.data.local.entity.IncomeEntity
import com.fintrack.kavindu.core.data.local.entity.MilestoneEntity
import com.fintrack.kavindu.core.data.local.entity.ProfileEntity
import com.fintrack.kavindu.core.data.local.entity.RecurringItemEntity

// The local copy of the signed-in user's Firestore data. Screens read only from here.
@Database(
    entities = [
        ProfileEntity::class,
        IncomeEntity::class,
        ExpenseEntity::class,
        CategoryEntity::class,
        RecurringItemEntity::class,
        GoalEntity::class,
        FreelanceProjectEntity::class,
        MilestoneEntity::class,
        CryptoTransferEntity::class,
        BalanceCheckEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun incomeDao(): IncomeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringItemDao(): RecurringItemDao
    abstract fun goalDao(): GoalDao
    abstract fun freelanceProjectDao(): FreelanceProjectDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun cryptoTransferDao(): CryptoTransferDao
    abstract fun balanceCheckDao(): BalanceCheckDao

    companion object {
        const val NAME = "kavindu_finance.db"
    }
}
