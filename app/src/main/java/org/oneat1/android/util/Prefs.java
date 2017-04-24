package org.oneat1.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.support.annotation.NonNull;

import org.oneat1.android.BuildConfig;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by parthpadgaonkar on 1/4/17.
 */
public class Prefs {
    private static final String PREF_FILE_KEY = BuildConfig.APPLICATION_ID;
    private static SharedPreferences prefs;

    private static final String NOTIFICATION_PREFERENCE = "pref.notify";
    private static final String COMPLETED_VIDEO_ID_PREFIX = "pref.completed.videoID";
    private static final String PLAYLIST_VIDEO_ID_PREFIX = "pref.playlist.numVideos";

    static final PublishSubject<String> prefChangedSignal = PublishSubject.create();
    private static OnSharedPreferenceChangeListener prefChangedListener;

    public static void init(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_FILE_KEY, Context.MODE_PRIVATE);
        prefChangedListener = new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                prefChangedSignal.onNext(key);
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefChangedListener);
    }

    /**
     * Returns the user's notification preference, if it exists.
     *
     * @return a boolean if the user has set a notification preference,
     * or <code>null</code> if no preference has been set.
     */
    public static Boolean getNotificationPreference() {
        if (prefs.contains(NOTIFICATION_PREFERENCE)) {
            return prefs.getBoolean(NOTIFICATION_PREFERENCE, false);
        }
        return null;
    }

    public static void setNotificationPreference(boolean allowNotifications) {
        prefs.edit()
              .putBoolean(NOTIFICATION_PREFERENCE, allowNotifications)
              .apply();
    }

    public static boolean didCompleteVideoAction(@NonNull String videoID) {
        Set<String> set = prefs.getStringSet(COMPLETED_VIDEO_ID_PREFIX, Collections.<String>emptySet());
        return set.contains(videoID);
    }

    public static void completedVideoAction(@NonNull String videoID, boolean completed) {
        Set<String> set = prefs.getStringSet(COMPLETED_VIDEO_ID_PREFIX, Collections.<String>emptySet());
        set = new HashSet<>(set); //getStringSet's Set oughtn't be modified
        if (completed) {
            set.add(videoID);
        } else {
            set.remove(videoID);
        }
        prefs.edit().putStringSet(COMPLETED_VIDEO_ID_PREFIX, set).apply();
    }

    public static int getNumVideosCompleted() {
        return prefs.getStringSet(COMPLETED_VIDEO_ID_PREFIX, Collections.<String>emptySet()).size();
    }

    public static void setNumPlaylistVideos(int num) {
        prefs.edit().putInt(PLAYLIST_VIDEO_ID_PREFIX, num).apply();
    }

    public static int getNumVideosInPlaylist() {
        return prefs.getInt(PLAYLIST_VIDEO_ID_PREFIX, 0);
    }

    public static Observable<Object> getVideoCompletedSignal() {
        return prefChangedSignal
                     .filter(new Predicate<String>() {
                         @Override
                         public boolean test(String prefKeyChanged) throws Exception {
                             switch (prefKeyChanged) {
                                 case COMPLETED_VIDEO_ID_PREFIX:
                                 case PLAYLIST_VIDEO_ID_PREFIX:
                                     return true;
                                 default:
                                     return false;
                             }
                         }
                     })
                     .cast(Object.class);
    }
}
