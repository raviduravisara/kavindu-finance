package com.fintrack.kavindu.core.di

import com.fintrack.kavindu.core.data.repository.AuthRepository
import com.fintrack.kavindu.core.data.repository.AuthRepositoryImpl
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
}
