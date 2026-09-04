package com.wyrmwhelp.idlehoard.di

import com.wyrmwhelp.idlehoard.BuildConfig
import com.wyrmwhelp.idlehoard.data.remote.SupabaseAuthRepository
import com.wyrmwhelp.idlehoard.data.remote.SupabaseCloudSaveRepository
import com.wyrmwhelp.idlehoard.domain.repository.AuthRepository
import com.wyrmwhelp.idlehoard.domain.repository.CloudSaveRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth)
        install(Postgrest)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SupabaseRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: SupabaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCloudSaveRepository(impl: SupabaseCloudSaveRepository): CloudSaveRepository
}
