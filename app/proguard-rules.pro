# Add project specific ProGuard rules here.

# -------- kotlinx.serialization --------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.test.propsid4sakura.**$$serializer { *; }
-keepclassmembers class com.test.propsid4sakura.** {
    *** Companion;
}
-keepclasseswithmembers class com.test.propsid4sakura.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class * { *; }

# -------- Room --------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}
-dontwarn androidx.room.paging.**

# -------- Koin --------
-keepnames class * { @org.koin.core.annotation.KoinInternalApi *; }

# -------- OkHttp --------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# -------- Google Mobile Ads (AdMob) --------
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# -------- User Messaging Platform (UMP) --------
-keep class com.google.android.ump.** { *; }

# -------- Coil --------
-dontwarn coil.**

# -------- Kotlin --------
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# -------- Jetpack Compose --------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# -------- BuildConfig --------
-keep class com.test.propsid4sakura.BuildConfig { *; }
