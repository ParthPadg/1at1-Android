package org.oneat1.android.util;

import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.ErrorReason;

/**
 * We don't care about all the methods
 * Created by parthpadgaonkar on 1/16/17.
 */
public class PlayerStateChangeListenerAdapter implements YouTubePlayer.PlayerStateChangeListener {
    @Override public void onLoading() { }

    @Override public void onLoaded(String s) { }

    @Override public void onAdStarted() { }

    @Override public void onVideoStarted() { }

    @Override public void onVideoEnded() { }

    @Override public void onError(ErrorReason errorReason) { }
}
