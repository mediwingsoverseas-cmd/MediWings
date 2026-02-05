# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===================================
# Firebase ProGuard Rules
# ===================================
# Firebase SDK handles most obfuscation automatically via consumer ProGuard rules
# These rules ensure proper operation in production builds

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep model classes used with Firebase Database/Firestore
# Add your model classes here if you use custom data classes
-keepclassmembers class com.tripplanner.mediwings.** {
    *;
}

# Keep FCM Service
-keep class com.tripplanner.mediwings.MyFirebaseMessagingService { *; }

# Glide ProGuard Rules
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep AndroidX annotations
-keep class androidx.annotation.** { *; }
-dontwarn androidx.annotation.**