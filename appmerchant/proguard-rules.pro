# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\userdata\adgangwa\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keepattributes SourceFile,LineNumberTable
-keep class android.support.v4.** { *; }
-keep class in.myecash.merchantbase.LoginActivity { *; }
# Backendless
-dontwarn com.backendless.**
-dontwarn weborb.**
-keep class weborb.** {*;}
-keep class com.backendless.** {*;}
# Backendless
# MPAndroidChart library
-dontwarn io.realm.**
-dontwarn com.github.mikephil.**
-keep public class com.github.mikephil.** {
     public protected *;
}
-keep class com.github.mikephil.charting.** { *; }
-keep class io.realm.** { *; }
# MPAndroidChart library
# crashlytics
-keepattributes *Annotation*
-keep public class * extends java.lang.Exception
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**
# crashlytics