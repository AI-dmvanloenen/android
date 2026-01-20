package com.odoo.fieldapp.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.odoo.fieldapp.BuildConfig
import com.odoo.fieldapp.data.connectivity.ConnectivityManagerNetworkMonitor
import com.odoo.fieldapp.data.local.OdooDatabase
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.domain.connectivity.NetworkMonitor
import com.odoo.fieldapp.data.local.dao.DeliveryDao
import com.odoo.fieldapp.data.local.dao.DeliveryLineDao
import com.odoo.fieldapp.data.local.dao.PaymentDao
import com.odoo.fieldapp.data.local.dao.ProductDao
import com.odoo.fieldapp.data.local.dao.SaleDao
import com.odoo.fieldapp.data.local.dao.SaleLineDao
import com.odoo.fieldapp.data.local.dao.SyncQueueDao
import com.odoo.fieldapp.data.remote.DynamicBaseUrlInterceptor
import com.odoo.fieldapp.data.remote.api.OdooApiService
import com.odoo.fieldapp.data.repository.ApiKeyProvider
import com.odoo.fieldapp.data.repository.ApiKeyProviderImpl
import com.odoo.fieldapp.data.repository.CustomerRepositoryImpl
import com.odoo.fieldapp.data.repository.DashboardRepositoryImpl
import com.odoo.fieldapp.data.repository.DeliveryRepositoryImpl
import com.odoo.fieldapp.data.repository.PaymentRepositoryImpl
import com.odoo.fieldapp.data.repository.ProductRepositoryImpl
import com.odoo.fieldapp.data.repository.SaleRepositoryImpl
import com.odoo.fieldapp.domain.repository.CustomerRepository
import com.odoo.fieldapp.domain.repository.DashboardRepository
import com.odoo.fieldapp.domain.repository.DeliveryRepository
import com.odoo.fieldapp.domain.repository.PaymentRepository
import com.odoo.fieldapp.domain.repository.ProductRepository
import com.odoo.fieldapp.domain.repository.SaleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module that provides dependencies for the entire app
 * 
 * @InstallIn(SingletonComponent::class) means these dependencies live as long as the app
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Provide Room Database
     *
     * Uses proper migrations to preserve data across schema changes.
     * Never use fallbackToDestructiveMigration() in production as it wipes all data.
     */
    @Provides
    @Singleton
    fun provideOdooDatabase(@ApplicationContext context: Context): OdooDatabase {
        return Room.databaseBuilder(
            context,
            OdooDatabase::class.java,
            OdooDatabase.DATABASE_NAME
        )
            .addMigrations(*OdooDatabase.ALL_MIGRATIONS)
            .build()
    }
    
    /**
     * Provide Customer DAO
     */
    @Provides
    @Singleton
    fun provideCustomerDao(database: OdooDatabase): CustomerDao {
        return database.customerDao()
    }
    
    /**
     * Provide Gson for JSON parsing
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    /**
     * Provide ApiKeyProvider (must be provided before OkHttpClient)
     */
    @Provides
    @Singleton
    fun provideApiKeyProviderImpl(@ApplicationContext context: Context): ApiKeyProviderImpl {
        return ApiKeyProviderImpl(context)
    }

    /**
     * Provide ApiKeyProvider interface binding
     */
    @Provides
    @Singleton
    fun provideApiKeyProvider(impl: ApiKeyProviderImpl): ApiKeyProvider {
        return impl
    }

    /**
     * Provide Dynamic Base URL Interceptor
     */
    @Provides
    @Singleton
    fun provideDynamicBaseUrlInterceptor(apiKeyProvider: ApiKeyProvider): DynamicBaseUrlInterceptor {
        return DynamicBaseUrlInterceptor(apiKeyProvider)
    }

    /**
     * Provide OkHttpClient with logging and dynamic URL interceptors
     *
     * Logging level differs by build type:
     * - Debug: BODY (full request/response logging for development)
     * - Release: BASIC (minimal logging for performance and security)
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Provide Retrofit instance
     *
     * Base URL is dynamically overridden by DynamicBaseUrlInterceptor
     * based on the server URL from settings.
     * The placeholder URL here is overwritten at request time.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://placeholder.odoo.com/")  // Overridden by interceptor
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Provide Odoo API Service
     */
    @Provides
    @Singleton
    fun provideOdooApiService(retrofit: Retrofit): OdooApiService {
        return retrofit.create(OdooApiService::class.java)
    }

    /**
     * Provide CustomerRepository
     */
    @Provides
    @Singleton
    fun provideCustomerRepository(
        customerDao: CustomerDao,
        apiService: OdooApiService,
        apiKeyProvider: ApiKeyProvider
    ): CustomerRepository {
        return CustomerRepositoryImpl(customerDao, apiService, apiKeyProvider)
    }

    /**
     * Provide Sale DAO
     */
    @Provides
    @Singleton
    fun provideSaleDao(database: OdooDatabase): SaleDao {
        return database.saleDao()
    }

    /**
     * Provide SaleLine DAO
     */
    @Provides
    @Singleton
    fun provideSaleLineDao(database: OdooDatabase): SaleLineDao {
        return database.saleLineDao()
    }

    /**
     * Provide SaleRepository
     */
    @Provides
    @Singleton
    fun provideSaleRepository(
        saleDao: SaleDao,
        saleLineDao: SaleLineDao,
        customerDao: CustomerDao,
        apiService: OdooApiService,
        apiKeyProvider: ApiKeyProvider
    ): SaleRepository {
        return SaleRepositoryImpl(saleDao, saleLineDao, customerDao, apiService, apiKeyProvider)
    }

    /**
     * Provide Delivery DAO
     */
    @Provides
    @Singleton
    fun provideDeliveryDao(database: OdooDatabase): DeliveryDao {
        return database.deliveryDao()
    }

    /**
     * Provide DeliveryLine DAO
     */
    @Provides
    @Singleton
    fun provideDeliveryLineDao(database: OdooDatabase): DeliveryLineDao {
        return database.deliveryLineDao()
    }

    /**
     * Provide DeliveryRepository
     */
    @Provides
    @Singleton
    fun provideDeliveryRepository(
        deliveryDao: DeliveryDao,
        deliveryLineDao: DeliveryLineDao,
        customerDao: CustomerDao,
        saleDao: SaleDao,
        apiService: OdooApiService,
        apiKeyProvider: ApiKeyProvider
    ): DeliveryRepository {
        return DeliveryRepositoryImpl(
            deliveryDao,
            deliveryLineDao,
            customerDao,
            saleDao,
            apiService,
            apiKeyProvider
        )
    }

    /**
     * Provide Payment DAO
     */
    @Provides
    @Singleton
    fun providePaymentDao(database: OdooDatabase): PaymentDao {
        return database.paymentDao()
    }

    /**
     * Provide PaymentRepository
     */
    @Provides
    @Singleton
    fun providePaymentRepository(
        paymentDao: PaymentDao,
        customerDao: CustomerDao,
        apiService: OdooApiService,
        apiKeyProvider: ApiKeyProvider
    ): PaymentRepository {
        return PaymentRepositoryImpl(
            paymentDao,
            customerDao,
            apiService,
            apiKeyProvider
        )
    }

    /**
     * Provide DashboardRepository
     */
    @Provides
    @Singleton
    fun provideDashboardRepository(
        deliveryDao: DeliveryDao,
        paymentDao: PaymentDao,
        customerDao: CustomerDao,
        saleDao: SaleDao,
        customerRepository: CustomerRepository,
        saleRepository: SaleRepository,
        deliveryRepository: DeliveryRepository,
        paymentRepository: PaymentRepository,
        productRepository: ProductRepository
    ): DashboardRepository {
        return DashboardRepositoryImpl(
            deliveryDao,
            paymentDao,
            customerDao,
            saleDao,
            customerRepository,
            saleRepository,
            deliveryRepository,
            paymentRepository,
            productRepository
        )
    }

    /**
     * Provide Product DAO
     */
    @Provides
    @Singleton
    fun provideProductDao(database: OdooDatabase): ProductDao {
        return database.productDao()
    }

    /**
     * Provide ProductRepository
     */
    @Provides
    @Singleton
    fun provideProductRepository(
        productDao: ProductDao,
        apiService: OdooApiService,
        apiKeyProvider: ApiKeyProvider
    ): ProductRepository {
        return ProductRepositoryImpl(productDao, apiService, apiKeyProvider)
    }

    /**
     * Provide NetworkMonitor
     * Monitors device connectivity state for offline-first functionality
     */
    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return ConnectivityManagerNetworkMonitor(context)
    }

    /**
     * Provide SyncQueue DAO
     * Used for managing offline operations queue
     */
    @Provides
    @Singleton
    fun provideSyncQueueDao(database: OdooDatabase): SyncQueueDao {
        return database.syncQueueDao()
    }
}
