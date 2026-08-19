# Compose Multiplatform / Kotlin serialization / Ktor keep rules
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# slf4j: no runtime binding included in release builds, keep the API surface
-dontwarn org.slf4j.impl.**
-keep class org.slf4j.impl.** { *; }

# kotlinx.serialization: keep generated serializers
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.budgeyet.**$$serializer { *; }
-keepclassmembers class com.budgeyet.** {
    *** Companion;
}
-keepclasseswithmembers class com.budgeyet.** {
    kotlinx.serialization.KSerializer serializer(...);
}
