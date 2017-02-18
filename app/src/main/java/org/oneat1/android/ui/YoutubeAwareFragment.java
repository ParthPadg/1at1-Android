package org.oneat1.android.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.os.Bundle;
import android.view.View;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerFragment;

import org.oneat1.android.BuildConfig;
import org.oneat1.android.R;
import org.oneat1.android.model.PlaylistItemResponse.PlaylistItem;
import org.oneat1.android.util.OA1Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by parthpadgaonkar on 2/14/17.
 */
public abstract class YoutubeAwareFragment extends Fragment implements OnInitializedListener {
    private final static Logger LOG = LoggerFactory.getLogger(YoutubeAwareFragment.class);

    protected YouTubePlayer youtubePlayer;
    protected PublishSubject<PlaylistItem> playlistClickSubject = PublishSubject.create();
    private Disposable subscription;

    protected abstract String getCurrentVideoID();

    protected abstract void setCurrentVideoID(String newVideoID);

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        YouTubePlayerFragment f = (YouTubePlayerFragment) getChildFragmentManager().findFragmentById(R.id.watch_fragment);
        if (f != null) { //if this is null, we're *so* boned...
            f.initialize(OA1Config.getInstance(getActivity()).getYoutubeAPIKey(), this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        subscription = playlistClickSubject
                             .subscribe(new Consumer<PlaylistItem>() {
                                 @Override
                                 public void accept(PlaylistItem playlistItem) throws Exception {
                                     String videoID = playlistItem.getVideoID();
                                     setCurrentVideoID(videoID);
                                     if (youtubePlayer != null) {
                                         youtubePlayer.loadVideo(videoID);
                                     }
                                 }
                             }, new Consumer<Throwable>() {
                                 @Override
                                 public void accept(Throwable throwable) throws Exception {

                                 }
                             });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (subscription != null) {
            subscription.dispose();
        }
    }

    @Override
    public void onDestroyView() {
        if (youtubePlayer != null) {
            youtubePlayer.release();
        }
        playlistClickSubject = null;
        youtubePlayer = null;
        super.onDestroyView();
    }

    //Youtube callback
    @Override
    public void onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
        this.youtubePlayer = player;
        LOG.debug("YOUTUBE player successful init; wasRestored {}", wasRestored);
        player.setShowFullscreenButton(false);
        if (!wasRestored) {
            String videoID = getCurrentVideoID();
            player.cueVideo(videoID);
            LOG.debug("queueing video ID {}", videoID);
        } else {
            player.play();
        }
        if (!BuildConfig.DEBUG) {
            CustomEvent event = new CustomEvent("Youtube SDK Init")
                                      .putCustomAttribute("success", "yes");
            Answers.getInstance().logCustom(event);
        }
    }

    //Youtube callback
    @Override
    public void onInitializationFailure(Provider provider, YouTubeInitializationResult errorReason) {
        LOG.error("Error initializing Youtube client: {}", errorReason);
        if (getActivity() == null) return;
        if (!BuildConfig.DEBUG) {
            CustomEvent event = new CustomEvent("Youtube SDK Init")
                                      .putCustomAttribute("success", "no")
                                      .putCustomAttribute("reason", errorReason.name());
            Answers.getInstance().logCustom(event);
        }
        if (errorReason.isUserRecoverableError()) {
            errorReason.getErrorDialog(getActivity(), 19181).show();
        } else {
            new AlertDialog.Builder(getActivity())
                  .setTitle("Error Loading YouTube")
                  .setMessage("It seems like there was an error loading the YouTube player; please ensure that the YouTube app is installed.")
                  .show();
        }
    }

    protected void handleYoutubeIdUpdate() {
        if (youtubePlayer != null) {
            String videoID = getCurrentVideoID();
            if (youtubePlayer.isPlaying()) {
                youtubePlayer.loadVideo(videoID);
            } else {
                youtubePlayer.cueVideo(videoID);
            }
        } // the player isn't loaded - we'll just have to wait :(
    }

    protected void onPlaylistVideoClicked(String newMainVideoID) {
    }

}
