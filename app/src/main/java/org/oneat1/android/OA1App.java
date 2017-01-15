package org.oneat1.android;

import android.app.Application;
import android.util.Log;

import com.google.gson.Gson;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;

import org.oneat1.android.util.OA1Config;
import org.oneat1.android.util.OA1Font;
import org.oneat1.android.util.Prefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import io.fabric.sdk.android.Fabric;

/**
 * Created by parthpadgaonkar on 1/3/17.
 */

public class OA1App extends Application {
    private final static Logger LOG = LoggerFactory.getLogger(OA1App.class);
    private static final Gson GSON = new Gson();

    private static OA1App sInstance;

    public static OA1App getApp(){
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        Log.d("OA1App", "Hello World, I am OA1App-Android!");

        configureLogging();
    }

    public void init(){
        Prefs.init(this);
        OA1Config config = OA1Config.getInstance(this);
        initFabric(config);

        startFCM();
        OA1Font.init();
    }

    public Gson getGson(){
        return GSON;
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
        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(loggerContext);
        logcatAppender.setEncoder(logcatEncoder);
        logcatAppender.start();
        rootLogger.addAppender(logcatAppender);

        LOG.warn("Finished initializing logging library.");
        LOG.warn("This is the first log message after the app starts up! App Version: {}", BuildConfig.VERSION_NAME);
    }

    private void startFCM() {
        LOG.debug("Initializing Firebase Cloud Messaging");
        //TODO check Play services
        //todo start FCM

    }

    private void initFabric(OA1Config config) {
        LOG.debug("Initializing Fabric/Twitter");
        TwitterAuthConfig authConfig = new TwitterAuthConfig(config.getTwitterKey(), config.getTwitterSecret());
        Fabric.with(this, new Twitter(authConfig));
    }

    /*
       private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                LOG.debug("This device supports Google Play Services, but they are not installed. Notifications will not work");
            } else {
                LOG.debug("This device does not support Google Play Services. Notifications will not work");
            }
            return false;
        }
        return true;
    }
     */




}
