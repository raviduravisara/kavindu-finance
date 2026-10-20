package com.fintrack.kavindu.core.di

import android.content.Context
import androidx.room.Room
import com.fintrack.kavindu.core.data.local.AppDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // Room is only a copy of Firestore: after a table change it starts empty and refills from Firestore.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideProfileDao(database: AppDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideIncomeDao(database: AppDatabase): IncomeDao = database.incomeDao()

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideRecurringItemDao(database: AppDatabase): RecurringItemDao = database.recurringItemDao()

    @Provides
    fun provideGoalDao(database: AppDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideFreelanceProjectDao(database: AppDatabase): FreelanceProjectDao = database.freelanceProjectDao()

    @Provides
    fun provideMilestoneDao(database: AppDatabase): MilestoneDao = database.milestoneDao()

    @Provides
    fun provideCryptoTransferDao(database: AppDatabase): CryptoTransferDao = database.cryptoTransferDao()

    @Provides
    fun provideBalanceCheckDao(database: AppDatabase): BalanceCheckDao = database.balanceCheckDao()
}
