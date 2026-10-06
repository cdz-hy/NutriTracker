# Preserve line numbers for crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson generic type signatures & annotations
-keepattributes Signature
-keepattributes *Annotation*

# Keep AI & Export data transfer classes and database entities from R8 field obfuscation
-keep class com.example.nutritracker.feature.camera.NutritionResult { *; }
-keep class com.example.nutritracker.feature.camera.NutritionResult$* { *; }
-keep class com.example.nutritracker.data.ExportBundle { *; }
-keep class com.example.nutritracker.data.ExportBundle$* { *; }
-keep class com.example.nutritracker.data.entity.** { *; }

# Keep @SerializedName fields
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Enum values used by Gson / Room / DataStore
-keepclassmembers enum com.example.nutritracker.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
