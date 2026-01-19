package com.odoo.fieldapp.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.odoo.fieldapp.data.local.OdooDatabase
import com.odoo.fieldapp.data.local.dao.CustomerDao
import com.odoo.fieldapp.data.remote.DynamicBaseUrlInterceptor
import com.odoo.fieldapp.data.remote.api.OdooApiService
import com.odoo.fieldapp.data.repository.ApiKeyProvider
import com.odoo.fieldapp.data.repository.ApiKeyProviderImpl
import com.odoo.fieldapp.data.repository.CustomerRepositoryImpl
import com.odoo.fieldapp.domain.repository.CustomerRepository
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
     */
    @Provides
    @Singleton
    fun provideOdooDatabase(@ApplicationContext context: Context): OdooDatabase {
        return Room.databaseBuilder(
            context,
            OdooDatabase::class.java,
            OdooDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()  // For development - TODO: Add proper migrations for production
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
    fun provideDynamicBaseUrlInterceptor(apiKeyProvider: ApiKeyProviderImpl): DynamicBaseUrlInterceptor {
        return DynamicBaseUrlInterceptor(apiKeyProvider)
    }

    /**
     * Provide OkHttpClient with logging and dynamic URL interceptors
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
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
     * based on the database name from settings.
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
}
