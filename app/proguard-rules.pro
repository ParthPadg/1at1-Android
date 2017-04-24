-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-dontpreverify
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-repackageclasses ''
-verbose
-allowaccessmodification

-libraryjars libs
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable #needed for crashlytics

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class * extends android.content.Context {
    public void *(android.view.View);
    public void *(android.view.MenuItem);
}
-keepclassmembers enum * { *;}
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
-keep public class * {
    public protected *;
}

#misc
-dontwarn javax.**
-dontwarn java.beans.**
# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**


#Logback
-keep class org.slf4j.Logger.** {*;}
-keep class org.slf4j.LoggerFactory.** {*;}
-keep class ch.qos.logback.core.AppenderBase.** {*;}
-keep class ch.qos.logback.classic.encoder.PatternLayoutEncoder.** {*;}
-keep class ch.qos.logback.classic.spi.ILoggingEvent.** {*;}
-keep class ch.qos.logback.classic.Logger.** {*;}
-keep class ch.qos.logback.classic.LoggerContext.** {*;}
-keep class ch.qos.logback.classic.LogcatAppender.** {*;}

#Glide
-keep class com.bumptech.glide.integration.okhttp3.OkHttpGlideModule