Buat sesuai aturan google playstore dan google admob.

Buatkan aplikasi Android native (Kotlin + Jetpack Compose + Material 3) untuk
di-list di Google Play. Tidak ada backend sendiri; data diambil dari file JSON
yang di-host di GitHub.

=== RINGKASAN APP ===
App berisi daftar "props ID" untuk game Sakura School Simulator (fan-made,
tidak resmi). Pengguna melihat props per kategori, dan untuk melihat ID sebuah
props mereka harus menonton rewarded ad (per props, satu per satu). Setelah
terbuka, ID bisa disalin dan dibagikan.

Nama app: Props ID for Sakura
Package name: com.test.propsid4sakura
Bahasa UI: Indonesia dan Inggris (lihat bagian Settings). Semua teks di
strings.xml (values dan values-en), tidak ada teks hardcode.
minSdk 24, compileSdk 35, targetSdk 35 (standar terbaru Google Play).

=== SUMBER DATA (2 FILE JSON TERPISAH) ===
1. Config iklan (admob config):
   https://raw.githubusercontent.com/susantohenri/admob-remote-configs/refs/heads/main/propsid4sakura/ads_config.json
2. Daftar props (props list):
   https://raw.githubusercontent.com/susantohenri/admob-remote-configs/refs/heads/main/propsid4sakura/content.json

Simpan kedua URL sebagai dua konstanta terpisah di satu tempat (misalnya
object AppConfig) supaya mudah diganti. Tiap file di-fetch, di-cache, dan
di-fallback secara INDEPENDEN: kegagalan satu file tidak boleh memengaruhi
file lainnya. Field yang tidak ada diberi nilai default; field tak dikenal
diabaikan.

Gambar props berada di folder yang SAMA dengan JSON:
https://raw.githubusercontent.com/susantohenri/admob-remote-configs/refs/heads/main/propsid4sakura/<prop_id>.webp
Nama file gambar = nilai field prop_id + ".webp".
Contoh: prop_id "48161718836410" -> 48161718836410.webp
URL gambar dihitung dari prop_id (tidak ada field image_url di JSON, dan
TIDAK perlu fungsi slugify). Turunkan base URL gambar dari folder URL JSON.
Gunakan Coil dengan placeholder shimmer/loading dan gambar fallback jika file
tidak ditemukan (404), tanpa crash.

Struktur content.json (format flat array di root):
[
  {
    "category": "Waterpark & Kolam",
    "title": "Waterpark 1",
    "prop_id": "48161718836410"
  }
]
*Catatan Deserializer:* Buat parser resilient yang mendukung format flat array
langsung `[...]` maupun jika dibungkus objek `{"props_list": [...]}` agar aman
jika struktur remote berubah di masa depan.

Struktur ads_config.json (format flat remote saat ini):
{
  "appOpenAdUnitId": "ca-app-pub-3940256099942544/9257395921",
  "bannerAdUnitId": "ca-app-pub-3940256099942544/6300978111",
  "interstitialAdUnitId": "ca-app-pub-3940256099942544/1033173712",
  "rewardedAdUnitId": "ca-app-pub-3940256099942544/5224354917",
  "nativeAdUnitId": "ca-app-pub-3940256099942544/2247696110",
  "isAdsEnabled": true
}
*Catatan Parser & Nilai Default Frekuensi Iklan:*
- ID iklan diambil dinamis dari field JSON di atas; jangan hardcode ID iklan di kode.
- `isAdsEnabled`: master switch iklan. Jika false, SEMUA iklan dimatikan dan SEMUA props terbuka gratis tanpa syarat iklan.
- Dukung juga format nested (`admob_config` + `frequency`) jika kelak remote di-update.
- Jika field frekuensi / jeda tidak ada di JSON remote, gunakan nilai default:
  - interstitial_every_n_detail_opens = 3
  - interstitial_min_interval_seconds = 60
  - app_open_min_background_hours = 4
  - app_open_cooldown_after_any_ad_seconds = 60
  - native_every_n_list_items = 8

=== DEPENDENSI & MANIFEST ===
Di build.gradle.kts (module app), tambahkan:
  implementation("com.google.android.gms:play-services-ads:23.6.0")
  implementation("com.google.android.ump:user-messaging-platform:3.1.0")
(UMP adalah artefak terpisah dari play-services-ads, jadi wajib ditambahkan.)
Gunakan versi stabil terkini.

Di AndroidManifest.xml, dalam tag <application>, pasang:
  <meta-data
      android:name="com.google.android.gms.ads.APPLICATION_ID"
      android:value="ca-app-pub-3940256099942544~3347511713" />
(Ini Sample App ID AdMob milik Google untuk testing. Beri komentar di
manifest bahwa harus diganti dengan App ID asli sebelum rilis.)
Izin: INTERNET, ACCESS_NETWORK_STATE, dan com.google.android.gms.permission.AD_ID.

=== LOGIKA CONFIG (PENTING) ===
1. Bundle salinan default ads_config.json dan content.json di assets / res/raw sebagai fallback.
2. Saat app dibuka: tampilkan data cache/fallback langsung (tidak boleh
   blank layar menunggu jaringan), lalu fetch kedua JSON terbaru di
   background dan perbarui cache jika berhasil.
3. Jika fetch gagal / offline / JSON rusak: tetap pakai cache terakhir yang
   valid, lalu fallback bawaan. Jangan crash.
4. Hormati isAdsEnabled / is_enabled di setiap format iklan. Jika iklan dinonaktifkan,
   SEMUA props otomatis terbuka gratis (tanpa iklan) dan tombol kunci tidak ditampilkan.
5. Tech Stack:
   - DI: Koin (koin-android, koin-androidx-compose)
   - Serialization: kotlinx.serialization + OkHttp
   - Database: Room Database untuk entitas Props, Favorites, dan Unlocked status
   - Preferences: Jetpack DataStore Preferences untuk preferensi (Bahasa, Tema, Ad Cooldowns & Counters)
   - Arsitektur: MVVM (ViewModel + StateFlow + Repository)

=== FITUR & LAYAR ===
1. Splash (SplashScreen API) -> Home.
2. Home / List:
   - Chip/tab kategori (dari nilai unik field category, plus "Semua"/"All").
   - Search bar (filter by title).
   - Grid atau list props: gambar (Coil, placeholder + error fallback, cache
     disk), title, ikon gembok jika belum terbuka, ikon favorit.
   - Native ad diselipkan tiap N item (frequency.native_every_n_list_items)
     jika native enabled & isAdsEnabled true.
   - Collapsed banner di bagian bawah layar list jika banner enabled
     (gunakan collapsible banner adaptive).
   - Akses ke Settings dan Favorit dari top bar/navigasi.
3. Detail props:
   - Gambar besar, title, kategori.
   - Jika terkunci: ID di-blur/disamarkan dan ada tombol "Tonton iklan untuk
     membuka ID" (rewarded). Pengguna HARUS menekan tombol sendiri; iklan
     tidak boleh muncul otomatis.
   - Buka ID hanya di callback onUserEarnedReward. Jika iklan gagal dimuat
     atau tidak tersedia, tampilkan pesan ramah + tombol coba lagi (jangan
     buka gratis diam-diam, kecuali rewarded dimatikan via config).
   - Jika terbuka: tampilkan prop_id, tombol "Salin ID" (clipboard + snackbar
     konfirmasi) dan tombol "Bagikan".
   - Bagikan: Intent.ACTION_SEND lewat share sheet bawaan. Teks (bilingual
     via strings.xml): nama props, kategori, ID, dan link Play Store app
     ini (https://play.google.com/store/apps/details?id=com.test.propsid4sakura).
     Sertakan gambar props sebagai lampiran jika sudah ter-cache
     (FileProvider, nama file cache = prop_id); jika belum, kirim teks saja.
   - Tombol favorit.
4. Favorit: layar daftar props favorit (disimpan lokal di Room Database).
5. Settings (layar tersendiri, dibuka dari Home):
   Section "Preferences" / "Preferensi":
     - Bahasa: pilihan Bahasa Indonesia / English (toggle dua pilihan,
       misalnya segmented button). Gunakan
       AppCompatDelegate.setApplicationLocales / per-app language API supaya
       perubahan langsung berlaku dan tersimpan. Default: ikut bahasa sistem,
       lalu pilihan pengguna disimpan.
     - Tema: pilihan Light mode / Dark mode / System default (toggle pilihan). Default:
       ikut sistem, lalu pilihan pengguna disimpan di DataStore dan
       diterapkan ke seluruh app.
   Section "Legal & Info":
     - About: tampilkan nama app, versi aplikasi (versionName dari
       BuildConfig/PackageInfo, contoh "Version 1.0.0"), dan disclaimer.
     - Privacy Policy: tombol yang membuka URL eksternal saat diklik
       (Intent.ACTION_VIEW / Custom Tabs):
       https://tokiocv.blogspot.com/2026/07/privacy-policy.html
6. Disclaimer (tampil di About dan singkat di layar pertama kali): "Aplikasi
   ini tidak resmi dan tidak berafiliasi dengan Garusoft atau Sakura School
   Simulator. Semua merek dagang adalah milik pemiliknya masing-masing."
   Sediakan versi Inggris.
7. Google UMP (User Messaging Platform) SDK, standar GDPR / Privacy Consent
   iklan:
   - Pada setiap app start, panggil requestConsentInfoUpdate lalu
     loadAndShowConsentFormIfRequired.
   - Inisialisasi MobileAds (MobileAds.initialize) dan load iklan HANYA
     setelah consent selesai dan consentInformation.canRequestAds() bernilai
     true. Jangan load iklan sebelum itu.
   - Di Settings, tampilkan opsi "Privacy Settings" / "Pengaturan Privasi"
     jika consentInformation.privacyOptionsRequirementStatus ==
     REQUIRED, dan buka form dengan showPrivacyOptionsForm supaya pengguna
     bisa mengubah pilihan consent.
   - Untuk testing, sediakan ConsentDebugSettings (test device hash
     dikonfigurasi lewat BuildConfig, aktif hanya di debug).

=== PENYIMPANAN LOKAL ===
- Room Database:
  - Tabel `props`: id, category, title, prop_id, is_favorite (Boolean), is_unlocked (Boolean), cached_at.
  - Sekali terbuka, tetap terbuka secara persisten tanpa iklan lagi.
- DataStore Preferences:
  - Preferensi bahasa, tema aplikasi.
  - Timestamp iklan terakhir (semua jenis, untuk cooldown).
  - Counter detail-open (untuk frekuensi interstitial).

=== ATURAN IKLAN (WAJIB SESUAI KEBIJAKAN ADMOB/GOOGLE PLAY) ===
- Rewarded: hanya lewat aksi sukarela pengguna. Preload satu iklan setelah
  consent selesai dan muat ulang setelah ditutup.
- Interstitial: tampil setiap frequency.interstitial_every_n_detail_opens
  kali pengguna membuka detail props, dengan jeda minimal
  frequency.interstitial_min_interval_seconds sejak iklan interstitial
  terakhir. Jangan tampil saat app baru dibuka, saat menekan tombol back
  untuk keluar, atau tepat setelah rewarded ditonton.
- App open: hanya saat app kembali dari background setelah minimal
  frequency.app_open_min_background_hours jam, tidak saat cold start
  pertama, dan tidak dalam
  frequency.app_open_cooldown_after_any_ad_seconds detik setelah iklan
  jenis apa pun ditutup. Tidak boleh tampil saat rewarded/interstitial
  sedang aktif.
- Buat AdManager terpusat yang mencatat waktu iklan terakhir (semua jenis)
  supaya tidak ada dua iklan muncul berdekatan.
- Tidak ada penempatan iklan yang menipu atau memancing klik tidak sengaja
  (jangan taruh tombol dekat iklan).
- Gunakan test ID selama development. Jangan pernah klik iklan asli saat
  testing.

=== KUALITAS & RILIS ===
- Loading state, empty state, dan error state di semua layar.
- Light/Dark mode mengikuti pilihan pengguna di Settings.
- ProGuard/R8 aktif untuk release, aturan yang diperlukan untuk
  serialization, Room, Koin, dan AdMob.
- README singkat: cara mengganti test ID dan Sample App ID ke ID asli,
  cara update props di GitHub (tambah gambar <prop_id>.webp + baris JSON),
  dan cara build release.

=== OUTPUT YANG DIHARAPKAN ===
Project Android Studio lengkap yang bisa langsung di-build, struktur package
rapi, tanpa TODO yang mengganjal fungsi inti. Jelaskan singkat keputusan
teknis penting di akhir.