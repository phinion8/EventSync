package com.app.eventsync.di

import android.content.Context
import android.util.Log
import com.app.eventsync.data.network.ApiInterface
import com.app.eventsync.data.repositories.FirebaseDatabaseRepository
import com.app.eventsync.data.repositories.AuthenticationRepository
import com.app.eventsync.utils.PreferenceManager
import com.moczul.ok2curl.logger.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideAuthenticationRepository() = AuthenticationRepository()

    @Singleton
    @Provides
    fun provideFirebaseDatabaseService(@ApplicationContext context: Context) =
        FirebaseDatabaseRepository(context)

    @Singleton
    @Provides
    fun providePreferenceManager(@ApplicationContext context: Context) = PreferenceManager(context)

    @Singleton
    @Provides
    fun provideApiService(): ApiInterface {
        return Retrofit.Builder()
            .run {
                baseUrl("https://fcm.googleapis.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(
                        OkHttpClient.Builder()
                        .addInterceptor(curlInterceptor())
                        .build())
                    .build()
            }.create(ApiInterface::class.java)
    }

}

private fun curlInterceptor(): Interceptor {
    return com.moczul.ok2curl.CurlInterceptor(object : Logger {
        override fun log(message: String) {
            Log.v("Ok2Curl", message)
        }
    })
}