package com.roadsignai.di

import com.roadsignai.data.repository.SignRepositoryImpl
import com.roadsignai.domain.repository.SignRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSignRepository(impl: SignRepositoryImpl): SignRepository
}
