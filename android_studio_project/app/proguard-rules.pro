# Keep JavaScript bridge interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep JSoup and data models intact under R8 optimization
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
-keep class com.kuroread.app.v2.model.** { *; }
