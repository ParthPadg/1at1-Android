package org.oneat1.android.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.oneat1.android.BuildConfig;
import org.oneat1.android.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by parthpadgaonkar on 1/16/17.
 */

public class RemoteConfigHelper {
    private final static Logger LOG = LoggerFactory.getLogger(RemoteConfigHelper.class);

    private static final long FIREBASE_FETCH_THRESHOLD = TimeUnit.MINUTES.toSeconds(15); //living on the edge, because server throttles at ~5+ requests per hour.
    private static final String KEY_YOUTUBE_VIDEO_ID = "LiveVideoUrl";
    private static final String KEY_YOUTUBE_PLAYLIST = "VideoPlaylistID";

    private static RemoteConfigHelper sInstance;
    private final FirebaseRemoteConfig remoteConfigInstance;

    public synchronized static RemoteConfigHelper get() {
        if (sInstance == null) {
            sInstance = new RemoteConfigHelper();
        }
        return sInstance;
    }

    private RemoteConfigHelper() {
        remoteConfigInstance = FirebaseRemoteConfig.getInstance();
        remoteConfigInstance.setDefaults(R.xml.firebase_defaults);
        remoteConfigInstance.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                                                     .setDeveloperModeEnabled(BuildConfig.DEBUG)
                                                     .build());
    }

    /**
     * @param bustCache        whether or not to respect Firebase's caching rules
     * @param fetchFromNetwork if false, overrides <code>bustCache</code>
     */
    public Single<RemoteConfigValues> fetch(final boolean bustCache, boolean fetchFromNetwork) {
        if (!fetchFromNetwork) {
            final RemoteConfigValues values = new RemoteConfigValues(remoteConfigInstance.getString(KEY_YOUTUBE_VIDEO_ID),
                                                                          remoteConfigInstance.getString(KEY_YOUTUBE_PLAYLIST));
            return Single.just(values);
        } else {
            return Single.create(new SingleOnSubscribe<RemoteConfigValues>() {
                @Override
                public void subscribe(final SingleEmitter<RemoteConfigValues> emitter) throws Exception {
                    final long lastFetch = remoteConfigInstance.getInfo().getFetchTimeMillis();
                    remoteConfigInstance
                          .fetch(bustCache ? 0 : FIREBASE_FETCH_THRESHOLD)
                          .addOnSuccessListener(new OnSuccessListener<Void>() {
                              @Override
                              public void onSuccess(Void aVoid) {
                                  remoteConfigInstance.activateFetched();

                                  RemoteConfigValues values = new RemoteConfigValues(remoteConfigInstance.getString(KEY_YOUTUBE_VIDEO_ID),
                                                                                          remoteConfigInstance.getString(KEY_YOUTUBE_PLAYLIST));

                                  emitter.onSuccess(values);
                                  setAnalytics(lastFetch, null);
                              }
                          })
                          .addOnFailureListener(new OnFailureListener() {
                              @Override
                              public void onFailure(@NonNull Exception e) {
                                  LOG.error("Error while fetching remote config: ", e);
                                  setAnalytics(lastFetch, e);
                                  emitter.onError(e);
                              }
                          });
                }
            }).subscribeOn(Schedulers.io());
        }

    }

    void setAnalytics(long lastFetch, @Nullable Exception e) {
        if (e == null) {
            CustomEvent event = new CustomEvent("Firebase Fetch Status");
            switch (remoteConfigInstance.getInfo().getLastFetchStatus()) {
                case FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS:
                    event.putCustomAttribute("status", "success");
                    break;
                case FirebaseRemoteConfig.LAST_FETCH_STATUS_FAILURE:
                    event.putCustomAttribute("status", "failure");
                    break;
                case FirebaseRemoteConfig.LAST_FETCH_STATUS_NO_FETCH_YET:
                    event.putCustomAttribute("status", "incomplete");
                    break;
                case FirebaseRemoteConfig.LAST_FETCH_STATUS_THROTTLED:
                    event.putCustomAttribute("status", "throttled");
                    LOG.warn("fetch was from cache");
                    break;
            }
            if (!BuildConfig.DEBUG) {
                event.putCustomAttribute("last fetch (s)", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastFetch));
                Answers.getInstance().logCustom(event);
            }
        } else {
            LOG.error("Error while fetching remote config: ", e);
            if (!BuildConfig.DEBUG) {
                CustomEvent event = new CustomEvent("Firebase Error")
                                          .putCustomAttribute("Message", e.getLocalizedMessage());
                Answers.getInstance().logCustom(event);
            }
        }
    }

    public static class RemoteConfigValues {
        String videoID;
        String playlistID;

        RemoteConfigValues(String videoID, String playlistID) {
            this.videoID = videoID;
            this.playlistID = playlistID;
        }

        public String getVideoID() {
            return videoID;
        }

        public String getPlaylistID() {
            return playlistID;
        }
    }
}
