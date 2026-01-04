package com.example.snaporder.core.di

import android.app.Application
import com.example.snaporder.core.session.UserSessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Entry point for accessing UserSessionManager from composables.
 * 
 * USAGE:
 * ```kotlin
 * val userSessionManager = EntryPointAccessors.fromApplication<UserSessionEntryPoint>()
 *     .userSessionManager()
 * ```
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserSessionEntryPoint {
    fun userSessionManager(): UserSessionManager
}

