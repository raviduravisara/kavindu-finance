package com.fintrack.kavindu.core.di

import com.fintrack.kavindu.core.data.repository.AuthRepository
import com.fintrack.kavindu.core.data.repository.AuthRepositoryImpl
import com.fintrack.kavindu.core.data.repository.BalanceCheckRepository
import com.fintrack.kavindu.core.data.repository.BalanceCheckRepositoryImpl
import com.fintrack.kavindu.core.data.repository.CategoryRepository
import com.fintrack.kavindu.core.data.repository.CategoryRepositoryImpl
import com.fintrack.kavindu.core.data.repository.CryptoRepository
import com.fintrack.kavindu.core.data.repository.CryptoRepositoryImpl
import com.fintrack.kavindu.core.data.repository.ExpenseRepository
import com.fintrack.kavindu.core.data.repository.ExpenseRepositoryImpl
import com.fintrack.kavindu.core.data.repository.FreelanceRepository
import com.fintrack.kavindu.core.data.repository.FreelanceRepositoryImpl
import com.fintrack.kavindu.core.data.repository.GoalRepository
import com.fintrack.kavindu.core.data.repository.GoalRepositoryImpl
import com.fintrack.kavindu.core.data.repository.IncomeRepository
import com.fintrack.kavindu.core.data.repository.IncomeRepositoryImpl
import com.fintrack.kavindu.core.data.repository.ProfileRepository
import com.fintrack.kavindu.core.data.repository.ProfileRepositoryImpl
import com.fintrack.kavindu.core.data.repository.RecurringRepository
import com.fintrack.kavindu.core.data.repository.RecurringRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Tells Hilt which class to use for each repository interface. Swapping a fake for a real
// repository is a one-line change here.
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindIncomeRepository(impl: IncomeRepositoryImpl): IncomeRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindRecurringRepository(impl: RecurringRepositoryImpl): RecurringRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds
    @Singleton
    abstract fun bindFreelanceRepository(impl: FreelanceRepositoryImpl): FreelanceRepository

    @Binds
    @Singleton
    abstract fun bindCryptoRepository(impl: CryptoRepositoryImpl): CryptoRepository

    @Binds
    @Singleton
    abstract fun bindBalanceCheckRepository(impl: BalanceCheckRepositoryImpl): BalanceCheckRepository
}
