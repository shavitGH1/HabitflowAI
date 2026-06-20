package com.habitflowai.di

import com.habitflowai.data.repository.AuthRepositoryImpl
import com.habitflowai.data.repository.GoalsRepositoryImpl
import com.habitflowai.data.repository.PersonaRepositoryImpl
import com.habitflowai.domain.repository.AuthRepository
import com.habitflowai.domain.repository.GoalsRepository
import com.habitflowai.domain.repository.PersonaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPersonaRepository(
        impl: PersonaRepositoryImpl
    ): PersonaRepository

    @Binds
    @Singleton
    abstract fun bindGoalsRepository(
        impl: GoalsRepositoryImpl
    ): GoalsRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository
}
