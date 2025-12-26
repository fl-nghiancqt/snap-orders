package com.example.snaporder.core.di

import com.example.snaporder.core.data.FakeMenuRepository
import com.example.snaporder.core.data.MenuDataSource
import com.example.snaporder.core.firestore.FirestoreProvider
import com.example.snaporder.core.firestore.MenuRepository
import com.example.snaporder.core.firestore.OrderRepository
import com.example.snaporder.core.firestore.UserRepository
import com.example.snaporder.core.utils.OrderBusinessLogic
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing application-wide dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideFirestoreProvider(): FirestoreProvider {
        return FirestoreProvider()
    }
    
    @Provides
    @Singleton
    fun provideUserRepository(firestoreProvider: FirestoreProvider): UserRepository {
        return UserRepository(firestoreProvider)
    }
    
    @Provides
    @Singleton
    fun provideMenuRepository(firestoreProvider: FirestoreProvider): MenuRepository {
        return MenuRepository(firestoreProvider)
    }
    
    /**
     * Provides MenuDataSource for UI development.
     * Currently returns FakeMenuRepository.
     * 
     * TODO: Replace with FirestoreMenuDataSource when ready:
     * return FirestoreMenuDataSource(firestoreProvider)
     */
    @Provides
    @Singleton
    fun provideMenuDataSource(): MenuDataSource {
        return FakeMenuRepository()
    }
    
    @Provides
    @Singleton
    fun provideOrderRepository(firestoreProvider: FirestoreProvider): OrderRepository {
        return OrderRepository(firestoreProvider)
    }
    
    @Provides
    @Singleton
    fun provideOrderBusinessLogic(orderRepository: OrderRepository): OrderBusinessLogic {
        return OrderBusinessLogic(orderRepository)
    }
}

