-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

-assumenosideeffects class java.util.Objects {
    public static ** requireNonNull(...);
}

-assumenosideeffects class kotlin.coroutines.jvm.internal.DebugMetadataKt {
    private static ** getDebugMetadataAnnotation(...) return null;
}

-keep class su.android.** { *; }

-keepclassmembers class androidx.appcompat.app.AppCompatDelegateImpl {
    boolean mActivityHandlesConfigFlagsChecked;
    int mActivityHandlesConfigFlags;
}

-assumenosideeffects class timber.log.Timber$Tree {
    public void v(...);
    public void d(...);
    public void v(java.lang.String, java.lang.Object[]);
    public void d(java.lang.String, java.lang.Object[]);
    public void v(java.lang.Throwable, java.lang.String, java.lang.Object[]);
    public void d(java.lang.Throwable, java.lang.String, java.lang.Object[]);
    public void v(java.lang.Throwable);
    public void d(java.lang.Throwable);
}

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-dontwarn org.junit.**
-dontwarn org.apache.**
-dontwarn org.bouncycastle.**
-dontwarn androidx.room.paging.**

-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.JsonQualifier <fields>;
}

-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
