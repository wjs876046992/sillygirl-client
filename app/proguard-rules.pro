# Keep Gson model classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.sillygirl.client.data.model.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
