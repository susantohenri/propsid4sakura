# Props ID for Sakura — Android App

Aplikasi Android native untuk melihat **Props ID** game Sakura School Simulator.
Dibangun dengan Kotlin + Jetpack Compose + Material 3 + AdMob + Google UMP.

---

## 🚀 Cara Build

### Prasyarat
- Android Studio Ladybug (2024.2.1) atau lebih baru
- JDK 17+ (sudah bundled di Android Studio)
- Android SDK Platform 35

### Langkah
1. Clone / buka folder ini di Android Studio
2. Tunggu Gradle sync selesai
3. Jalankan di emulator atau perangkat fisik

---

## 🔑 Cara Mengganti Test ID ke ID Produksi (WAJIB sebelum rilis)

### 1. AdMob Application ID
Buka [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml) dan ganti:
```xml
android:value="ca-app-pub-3940256099942544~3347511713"
```
dengan **App ID** akun AdMob Anda (format: `ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX`).

### 2. Ad Unit IDs
Upload file [`ads_config.json`](https://github.com/susantohenri/admob-remote-configs/blob/main/propsid4sakura/ads_config.json) di GitHub Anda dengan Ad Unit ID produksi:
```json
{
  "appOpenAdUnitId": "ca-app-pub-XXXX/XXXX",
  "bannerAdUnitId": "ca-app-pub-XXXX/XXXX",
  "interstitialAdUnitId": "ca-app-pub-XXXX/XXXX",
  "rewardedAdUnitId": "ca-app-pub-XXXX/XXXX",
  "nativeAdUnitId": "ca-app-pub-XXXX/XXXX",
  "isAdsEnabled": true
}
```
Aplikasi akan fetch dan menggunakan ID terbaru dari remote secara otomatis.

### 3. Default Fallback
Update juga [`app/src/main/assets/ads_config.json`](app/src/main/assets/ads_config.json) dengan ID produksi, sebagai fallback offline.

---

## 📦 Cara Menambahkan Props Baru

1. Upload gambar ke GitHub dengan nama `<prop_id>.webp` di folder:
   `admob-remote-configs/propsid4sakura/`

2. Tambahkan baris baru di `content.json`:
   ```json
   { "category": "Nama Kategori", "title": "Nama Props", "prop_id": "XXXXXXXXXXXXXX" }
   ```

3. Commit & push. Aplikasi akan mengambil data terbaru saat dibuka online.

---

## 🏗️ Cara Build Release AAB (untuk Google Play)

### 1. Buat Keystore
```bash
keytool -genkey -v -keystore propsid4sakura.jks -keyalg RSA -keysize 2048 -validity 10000 -alias propsid4sakura
```

### 2. Build AAB
Di Android Studio: **Build → Generate Signed Bundle/APK → Android App Bundle**

Atau via command line:
```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

### 3. Upload ke Google Play Console
Upload AAB di Google Play Console → Production/Internal testing.

---

## 🏛️ Arsitektur

```
app/
├── ads/                    # AdManager + ConsentManager (UMP)
├── data/
│   ├── local/              # Room DB (PropEntity, PropDao) + DataStore (AppPreferences)
│   ├── model/              # PropItem, AdsConfig (domain models)
│   ├── remote/             # RemoteDataSource (OkHttp + resilient JSON parser)
│   └── repository/         # PropsRepository, AdsConfigRepository
├── di/                     # Koin AppModule
├── ui/
│   ├── components/         # PropCard, CategoryChips, SearchBar, BannerAdView, NativeAdView, Shimmer
│   ├── detail/             # DetailScreen + DetailViewModel
│   ├── favorites/          # FavoritesScreen + FavoritesViewModel
│   ├── home/               # HomeScreen + HomeViewModel
│   ├── navigation/         # AppNavigation, Screen
│   ├── settings/           # SettingsScreen + SettingsViewModel
│   └── theme/              # Color, Type, Theme (Material 3)
├── AppConfig.kt            # URL constants + image URL builder
├── MainActivity.kt         # SplashScreen, UMP Consent, Theme/Lang init
└── PropsApplication.kt     # Koin init
```

---

## ⚠️ Catatan Penting

- **Jangan klik iklan asli saat testing** — gunakan test device ID.
- **Disclaimer**: Aplikasi ini tidak resmi dan tidak berafiliasi dengan Garusoft atau Sakura School Simulator.
- Ganti package name `com.test.propsid4sakura` → nama package produksi Anda sebelum publish.
