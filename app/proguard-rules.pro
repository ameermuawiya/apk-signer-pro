-dontwarn javax.lang.model.element.Modifier
-dontwarn org.xmlpull.v1.**
-dontwarn org.kxml2.io.**
-dontwarn android.content.res.**
-dontwarn org.**

# BouncyCastle Cryptographic Providers & JCA/JCE SPIs
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keystore classes & custom SPIs
-keep class com.ameermuawiya.apksigner.data.keystore.** { *; }

# APK Signature Engine and Verifier
-keep class com.android.apksig.** { *; }

# Apache Commons Compress
-keep class org.apache.commons.compress.** { *; }

# Models and Database
-keep class com.ameermuawiya.apksigner.data.model.** { *; }
-keep class com.ameermuawiya.apksigner.data.db.** { *; }
-keep class com.ameermuawiya.apksigner.data.preferences.** { *; }

# Android Architecture Components & Compose
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Remove unused logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
