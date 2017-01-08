package org.oneat1.android.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;

import org.oneat1.android.BuildConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by parthpadgaonkar on 1/3/17.
 */

public class OA1Config {
    private final static Logger LOG = LoggerFactory.getLogger(OA1Config.class);
    private static final String CONFIG_FILENAME = "config.properties";
    private static final String PROPERTY_YOUTUBE = "YOUTUBE_API_KEY";
    private static final String PROPERTY_TWITTER_KEY= "TWITTER_KEY";
    private static final String PROPERTY_TWITTER_SECRET = "TWITTER_SECRET";

    private static OA1Config sInstance;

    private String youtubeKey;
    private String twitterKey;
    private String twitterSecret;

    public static OA1Config getInstance(@NonNull Context context) {
        if (sInstance == null) {
            AssetManager assetManager = context.getResources().getAssets();
            try {
                InputStream propsInputStream = assetManager.open(CONFIG_FILENAME);
                Properties props = new Properties();
                props.load(propsInputStream);
                LOG.debug("Loaded properties!");
                OA1Config instance = new OA1Config();

                instance.youtubeKey = props.getProperty(PROPERTY_YOUTUBE);
                instance.twitterKey = props.getProperty(PROPERTY_TWITTER_KEY);
                instance.twitterSecret = props.getProperty(PROPERTY_TWITTER_SECRET);
                //add new secrets here

                sInstance = instance;
            } catch (IOException | IllegalArgumentException e) {
                LOG.error("Warning - couldn't load config.properties. All properties will be null!");
                LOG.error("Error loading config.properties - is it missing?", e);
                if (BuildConfig.DEBUG) {
                    throw new RuntimeException(e); //crash, so the developer knows to fix this
                }
            }

        }
        return sInstance;
    }

    public String getYoutubeAPIKey() {
        return youtubeKey;
    }

    public String getTwitterKey() {
        return twitterKey;
    }

    public String getTwitterSecret() {
        return twitterSecret;
    }
}
