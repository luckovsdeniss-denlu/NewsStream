# R8 rules for the release build. Libraries bring their own rules; these cover what the app itself
# reaches by reflection.

# The YouTube player page calls these methods from JavaScript (see VideoPlayerScreen).
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
