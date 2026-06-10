# ProGuard rules for RoadSignAI
# Keep ML Kit models
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep OSMDroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
