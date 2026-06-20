# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
