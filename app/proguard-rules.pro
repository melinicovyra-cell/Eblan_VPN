-keep class com.eblanvpn.app.** { *; }
-keep class libv2ray.** { *; }
-keep class go.** { *; }

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.eblanvpn.app.**$$serializer { *; }
-keepclassmembers class com.eblanvpn.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.eblanvpn.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
