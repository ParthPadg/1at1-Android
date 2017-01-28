package org.oneat1.android;

import android.app.Application;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;

import org.oneat1.android.firebase.RemoteConfigHelper;
import org.oneat1.android.firebase.RemoteConfigHelper.RemoteConfigValues;
import org.oneat1.android.util.API;
import org.oneat1.android.util.CrashlyticsCrashAppender;
import org.oneat1.android.util.OA1Config;
import org.oneat1.android.util.OA1Font;
import org.oneat1.android.util.OA1Util.ThreadUtil;
import org.oneat1.android.util.Prefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import io.fabric.sdk.android.Fabric;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Created by parthpadgaonkar on 1/3/17.
 */

public class OA1App extends Application {
    private final static Logger LOG = LoggerFactory.getLogger(OA1App.class);

    private static OA1App sInstance;

    public static OA1App getApp() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        long start = System.currentTimeMillis();
        sInstance = this;
        Log.d("OA1App", "Hello World, I am OA1App-Android!");
        ThreadUtil.getInstance();
        configureLogging();
        OA1Config config = OA1Config.getInstance(this);
        initFabric(config);
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                LOG.error("Uncaught exception: ", throwable);
                Crashlytics.logException(throwable);
            }
        });
        LOG.debug("Application.onCreate took {}ms", System.currentTimeMillis() - start);
    }

    public void init() {
        long start = System.currentTimeMillis();
        Prefs.init(this);
        initFirebase();
        OA1Font.init();
        API.init(this);
        LOG.debug("Application.init() took {}ms", System.currentTimeMillis() - start);
    }

    private void configureLogging() {
        Log.i("OA1App", "Configuring Logback...");
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();

        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        PatternLayoutEncoder logcatEncoder = new PatternLayoutEncoder();
        logcatEncoder.setContext(loggerContext);
        logcatEncoder.setPattern("%msg%n");
        logcatEncoder.start();
        if (BuildConfig.DEBUG) {
            LogcatAppender logcatAppender = new LogcatAppender();
            logcatAppender.setContext(loggerContext);
            logcatAppender.setEncoder(logcatEncoder);
            logcatAppender.start();
            rootLogger.addAppender(logcatAppender);
        } else {
            CrashlyticsCrashAppender firebaseAppender = new CrashlyticsCrashAppender(logcatEncoder);
            firebaseAppender.setContext(loggerContext);
            firebaseAppender.start();
            rootLogger.addAppender(firebaseAppender);
        }

        LOG.warn("Finished initializing logging library.");
        LOG.warn("This is the first log message after the app starts up! App Version: {}", BuildConfig.VERSION_NAME);
    }

    private void initFirebase() {
        RemoteConfigHelper.get()
              .fetch(true, true)
              .subscribe(new BiConsumer<RemoteConfigValues, Throwable>() {
                  @Override
                  public void accept(RemoteConfigValues values, Throwable throwable) throws Exception {
                      //we don't care about the values
                      //TODO handle error


                  }
              });
    }

    private void initFabric(OA1Config config) {
        LOG.debug("Initializing Fabric/Twitter");
        TwitterAuthConfig authConfig = new TwitterAuthConfig(config.getTwitterKey(), config.getTwitterSecret());
        if (BuildConfig.DEBUG) {
            Fabric.with(this, new Twitter(authConfig)); //don't enable Crashlytics for Debug builds!
        } else {
            Fabric.with(this, new Twitter(authConfig), new Crashlytics());
        }
    }
}
