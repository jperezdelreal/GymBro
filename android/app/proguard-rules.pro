# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# ============================
# GSON
# ============================
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ============================
# Retrofit & OkHttp
# ============================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ============================
# Hilt (Dependency Injection)
# ============================
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.components.SingletonComponent class * { *; }
-keepclasseswithmembernames class * {
    @dagger.* <fields>;
}
-keepclasseswithmembernames class * {
    @dagger.* <methods>;
}

# ============================
# Room Database
# ============================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** DATABASE;
}
-keep class * extends androidx.room.migration.Migration { *; }
-dontwarn androidx.room.paging.**

# ============================
# Lottie Animations
# ============================
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.model.** { *; }
-keep class com.airbnb.lottie.animation.** { *; }
-keep class com.airbnb.lottie.value.** { *; }

# ============================
# Firebase
# ============================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }

# Firebase Vertex AI
-keep class com.google.firebase.vertexai.** { *; }

# ============================
# Kotlin Coroutines
# ============================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================
# AndroidX & Jetpack Compose
# ============================
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.work.** { *; }
-keepclassmembers class androidx.work.Worker {
    public <init>(...);
}

# DataStore
-keep class androidx.datastore.*.** { *; }

# Glance (Widgets)
-keep class androidx.glance.** { *; }

# Health Connect
-keep class androidx.health.connect.client.** { *; }

# ============================
# Coil Image Loading
# ============================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================
# Kotlin & Android
# ============================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keepclassmembers class * {
    @kotlin.Metadata <fields>;
}

# Parcelize
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ============================
# GymBro App Models
# ============================
-keep class com.gymbro.core.data.model.** { *; }
-keep class com.gymbro.core.domain.model.** { *; }
-keepclassmembers class com.gymbro.core.data.model.** { *; }
-keepclassmembers class com.gymbro.core.domain.model.** { *; }

# Keep all @Serializable classes
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
