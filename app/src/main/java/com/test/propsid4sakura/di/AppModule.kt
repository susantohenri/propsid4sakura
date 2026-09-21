package com.test.propsid4sakura.di

import android.content.Context
import androidx.room.Room
import com.test.propsid4sakura.AppConfig
import com.test.propsid4sakura.ads.AdManager
import com.test.propsid4sakura.ads.ConsentManager
import com.test.propsid4sakura.data.local.AppDatabase
import com.test.propsid4sakura.data.local.AppPreferences
import com.test.propsid4sakura.data.remote.RemoteDataSource
import com.test.propsid4sakura.data.repository.AdsConfigRepository
import com.test.propsid4sakura.data.repository.PropsRepository
import com.test.propsid4sakura.ui.detail.DetailViewModel
import com.test.propsid4sakura.ui.favorites.FavoritesViewModel
import com.test.propsid4sakura.ui.home.HomeViewModel
import com.test.propsid4sakura.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {

    // ---- Application-scope coroutine scope ----
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // ---- Network ----
    single {
        OkHttpClient.Builder()
            .connectTimeout(AppConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    // ---- Database ----
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    // ---- DataStore Preferences ----
    single { AppPreferences(androidContext()) }

    // ---- Remote Data Source ----
    single { RemoteDataSource(androidContext(), get(), get()) }

    // ---- Repositories ----
    single { PropsRepository(get(), get()) }
    single { AdsConfigRepository(get()) }

    // ---- Ads & Consent ----
    single { ConsentManager(androidContext()) }
    single { AdManager(androidContext(), get(), get(), get()) }

    // ---- ViewModels ----
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { (propId: String) -> DetailViewModel(propId, get(), get(), get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}
