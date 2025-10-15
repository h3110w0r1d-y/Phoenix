package com.h3110w0r1d.phoenix.di

import android.content.Context
import com.h3110w0r1d.phoenix.data.app.AppRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppRepositoryModule {
    @Provides
    @Singleton
    fun provideAppRepository(
        @ApplicationContext context: Context,
    ): AppRepository = AppRepository(context)
}
