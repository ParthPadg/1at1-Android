package org.oneat1.android.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.oneat1.android.BuildConfig;
import org.oneat1.android.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by parthpadgaonkar on 1/16/17.
 */

public class RemoteConfigHelper {
    private final static Logger LOG = LoggerFactory.getLogger(RemoteConfigHelper.class);

    private static final long FIREBASE_FETCH_THRESHOLD = TimeUnit.MINUTES.toSeconds(15); //playing with fire, because server throttles at ~5+ requests per hour.
    private static final String KEY_YOUTUBE_VIDEO_ID = "LiveVideoUrl";
    private static final String KEY_YOUTUBE_PLAYLIST = "VideoPlaylistID";

    private static RemoteConfigHelper sInstance;
    private final FirebaseRemoteConfig remoteConfigInstance;

    public interface CompletionListener {
        void onComplete(boolean wasSuccessful, @Nullable String youtubeID);
    }

    public synchronized static RemoteConfigHelper get() {
        if (sInstance == null) {
            sInstance = new RemoteConfigHelper();
        }
        return sInstance;
    }

    private RemoteConfigHelper() {
        remoteConfigInstance = FirebaseRemoteConfig.getInstance();
        remoteConfigInstance.setDefaults(R.xml.firebase_defaults);
    }

    public void fetch(boolean bustCache, final CompletionListener listener) {
        final long lastFetch = remoteConfigInstance.getInfo().getFetchTimeMillis();
        remoteConfigInstance
              .fetch(bustCache ? 0 : FIREBASE_FETCH_THRESHOLD)
              .addOnCompleteListener(new OnCompleteListener<Void>() {
                  @Override
                  public void onComplete(@NonNull Task<Void> task) {
                      remoteConfigInstance.activateFetched();
                      final boolean success;
                      String videoID = null;
                      String playlistID = null;
                      if (success = task.isSuccessful()) {
                          LOG.debug("successful fetch!");

                          //actually obtain the new IDs here
                          videoID = remoteConfigInstance.getString(KEY_YOUTUBE_VIDEO_ID);
                          playlistID = remoteConfigInstance.getString(KEY_YOUTUBE_PLAYLIST);

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
                          Exception exception = task.getException();
                          LOG.error("Error while fetching remote config: ", exception);
                          if (!BuildConfig.DEBUG) {
                              CustomEvent event = new CustomEvent("Firebase Error")
                                                        .putCustomAttribute("Message", exception == null ? "null" : exception.getLocalizedMessage());
                              Answers.getInstance().logCustom(event);
                          }
                      }
                      listener.onComplete(success, videoID);
                  }
              });
    }

    public String getYoutubeID() {
        return remoteConfigInstance.getString(KEY_YOUTUBE_VIDEO_ID);
    }

    public String getPlaylistID() {
        return remoteConfigInstance.getString(KEY_YOUTUBE_PLAYLIST);
    }
}
