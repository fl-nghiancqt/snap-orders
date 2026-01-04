package com.example.snaporder.core.di

import com.example.snaporder.core.data.CartRepository
import com.example.snaporder.core.data.FakeCartRepository
import com.example.snaporder.core.data.FakeMenuRepository
import com.example.snaporder.core.data.FakeOrderHistoryRepository
import com.example.snaporder.core.data.FirestoreMenuDataSource
import com.example.snaporder.core.data.MenuDataSource
import com.example.snaporder.core.data.OrderHistoryRepository
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
     * Provides MenuDataSource for menu operations.
     * Uses FirestoreMenuDataSource to load menus from Firestore.
     */
    @Provides
    @Singleton
    fun provideMenuDataSource(menuRepository: MenuRepository): MenuDataSource {
        return FirestoreMenuDataSource(menuRepository)
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
    
    /**
     * Provides CartRepository for UI development.
     * Currently returns FakeCartRepository.
     * 
     * TODO: Replace with FirestoreCartRepository when ready:
     * return FirestoreCartRepository(firestoreProvider)
     */
    @Provides
    @Singleton
    fun provideCartRepository(): CartRepository {
        return FakeCartRepository()
    }
    
    /**
     * Provides OrderHistoryRepository for UI development.
     * Currently returns FakeOrderHistoryRepository.
     * 
     * TODO: Replace with FirestoreOrderHistoryRepository when ready:
     * return FirestoreOrderHistoryRepository(firestoreProvider)
     */
    @Provides
    @Singleton
    fun provideOrderHistoryRepository(): OrderHistoryRepository {
        return FakeOrderHistoryRepository()
    }
}

