package org.oneat1.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.ShareEvent;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.gson.Gson;

import org.oneat1.android.BuildConfig;
import org.oneat1.android.OA1App;
import org.oneat1.android.R;
import org.oneat1.android.firebase.RemoteConfigHelper;
import org.oneat1.android.firebase.RemoteConfigHelper.CompletionListener;
import org.oneat1.android.model.youtube.YTResponseBody;
import org.oneat1.android.model.youtube.YTResponseBody.YTItem;
import org.oneat1.android.util.OA1Config;
import org.oneat1.android.util.OA1Util;
import org.oneat1.android.util.OA1Util.ThreadUtil;
import org.oneat1.android.util.PlayerStateChangeListenerAdapter;
import org.oneat1.android.util.TypefaceTextView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.ButterKnife.Action;
import butterknife.OnClick;
import butterknife.Unbinder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

/**
 * Created by parthpadgaonkar on 1/8/17.
 */
public class WatchVideoFragment extends Fragment implements OnInitializedListener {
    private final static Logger LOG = LoggerFactory.getLogger(WatchVideoFragment.class);
    private static final String KEY_RESPONSE = "watch.youtube.response";

    private static final Uri.Builder BASE_URL = Uri.parse("https://www.googleapis.com/youtube/v3/videos")
                                                      .buildUpon()
                                                      .appendQueryParameter("part", "snippet, statistics");
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
                                                          .addInterceptor(new HttpLoggingInterceptor().setLevel(Level.BASIC))
                                                          .build();

    @BindView(R.id.watch_title) TypefaceTextView videoTitle;
    @BindView(R.id.watch_viewercount) TypefaceTextView videoViewCount;
    @BindView(R.id.watch_description) TypefaceTextView videoDescription;
    @BindViews({R.id.watch_title, R.id.watch_viewercount, R.id.watch_description}) List<TypefaceTextView> videoContent;
    @BindView(R.id.watch_progress) ProgressBar progress;

    YTResponseBody responseBody;
    boolean requestedYoutubeInfo = false;
    private Unbinder unbinder;
    private String videoID = RemoteConfigHelper.get().getYoutubeID();
    private YouTubePlayer youtubePlayer;

    public static WatchVideoFragment createInstance() {
        return new WatchVideoFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_watch, container, false);
        unbinder = ButterKnife.bind(this, view);

        progress.setVisibility(View.VISIBLE);
        progress.animate()
              .alpha(1)
              .start();
        ButterKnife.apply(videoContent, new Action<TypefaceTextView>() {
            @Override
            public void apply(@NonNull TypefaceTextView view, int index) {
                view.setVisibility(View.GONE);
            }
        });

        YouTubePlayerFragment f = (YouTubePlayerFragment) getChildFragmentManager().findFragmentById(R.id.watch_fragment);
        if (f != null) { //if this is null, we're *so* boned...
            f.initialize(OA1Config.getInstance(getActivity()).getYoutubeAPIKey(), this);
        }

        LOG.debug("savedInstanceState is null: {}", savedInstanceState == null);
        if (savedInstanceState != null) {
            responseBody = OA1App.getApp()
                                 .getGson()
                                 .fromJson(savedInstanceState.getString(KEY_RESPONSE), YTResponseBody.class);
        }

        if (responseBody != null) {
            bindResponseToView(responseBody);
        } else {
            getVideoInfo();
        }

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            RemoteConfigHelper.get().fetch(false, new CompletionListener() {
                @Override
                public void onComplete(boolean wasSuccessful, @Nullable String youtubeID) {
                    if (wasSuccessful && youtubeID != null) {
                        if (!Objects.equals(videoID, youtubeID)) { //new value!
                            videoID = youtubeID;
                            getVideoInfo(); // we have a new value; we have to update
                            if (youtubePlayer != null) {
                                if (youtubePlayer.isPlaying()) {
                                    youtubePlayer.setPlayerStateChangeListener(new PlayerStateChangeListenerAdapter() {
                                        @Override
                                        public void onVideoEnded() {
                                            if (youtubePlayer == null) return;
                                            youtubePlayer.cueVideo(videoID);
                                        }
                                    });
                                } else {
                                    youtubePlayer.cueVideo(youtubeID);
                                }
                            } //nothing we can do; no UI :(
                        } else if (!requestedYoutubeInfo) {
                            getVideoInfo(); // need to populate the Youtube info
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        youtubePlayer.release();
        youtubePlayer = null;
        OA1Util.safeUnbind(unbinder);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LOG.debug("saving state!");
        outState.putString(KEY_RESPONSE, OA1App.getApp().getGson().toJson(responseBody));
    }

    @OnClick(R.id.watch_share)
    void onShareClick() {
        Intent share = new Intent(Intent.ACTION_SEND)
                              .setType("text/plain")
                              .putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=" + videoID);
        startActivityForResult(Intent.createChooser(share, "Share this week's 1@1 action!"), 191817);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 191817 && !BuildConfig.DEBUG && resultCode == Activity.RESULT_OK) {
            Answers.getInstance().logShare(new ShareEvent().putContentName("Youtube").putContentId(videoID));
        }
    }

    private void getVideoInfo() {
        requestedYoutubeInfo = true;
        String url = BASE_URL.appendQueryParameter("id", videoID)
                           .appendQueryParameter("key", OA1Config.getInstance(getActivity()).getYoutubeAPIKey())
                           .toString();// equivalent of .build().toString();
        HTTP_CLIENT.newCall(new Builder().url(url).build())
              .enqueue(new BaseCallback());
    }

    //Youtube callback
    @Override
    public void onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
        this.youtubePlayer = player;
        LOG.debug("YOUTUBE player successful init; wasRestored {}", wasRestored);
        player.setShowFullscreenButton(false);
        if (!wasRestored) {
            player.cueVideo(videoID);
            LOG.debug("queueing video ID {}", videoID);
        } else {
            player.play();
        }
        if(!BuildConfig.DEBUG){
            CustomEvent event = new CustomEvent("Youtube SDK Init")
                                            .putCustomAttribute("success", "yes");
            Answers.getInstance().logCustom(event);
        }
    }

    //Youtube callback
    @Override
    public void onInitializationFailure(Provider provider, YouTubeInitializationResult errorReason) {
        LOG.error("Error initializing Youtube client: {}", errorReason);
        if(getActivity() == null) return;if(!BuildConfig.DEBUG){
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

    void bindResponseToView(YTResponseBody response) {
        if(videoTitle == null || videoViewCount == null || videoDescription == null) {
            LOG.error("Error - no UI!");
            return;
        }
        String title = null;
        String viewers = null;
        String description = null;
        responseBody = response;
        if (response.items != null && response.items.size() > 0) {
            YTItem videoItem = response.items.get(0); //blindly take first item; ideally, there should only be one anyway
            if (videoItem.snippet != null) {
                if (!TextUtils.isEmpty(videoItem.snippet.title)) {
                    title = videoItem.snippet.title;
                } else {
                    LOG.warn("snippet.title is null!");
                }

                if (!TextUtils.isEmpty(videoItem.snippet.description)) {
                    description = videoItem.snippet.description;
                } else {
                    LOG.warn("snippet.description is null!");
                }
            } else {
                LOG.warn("snippet.title is null!");
            }

            NumberFormat numFormat = NumberFormat.getNumberInstance();
            if (!TextUtils.isEmpty(videoItem.statistics.viewCount)) {
                Number viewCount;
                try {
                    viewCount = Long.parseLong(videoItem.statistics.viewCount);
                } catch (Exception e) {
                    LOG.error("Error parsing {} to long - retrying as BigInt", videoItem.statistics.viewCount, e);
                    viewCount = new BigInteger(videoItem.statistics.viewCount);
                }
                viewers = numFormat.format(viewCount);
            } else {
                LOG.warn("statistics.viewcount is null!");
            }
        }

        if (TextUtils.isEmpty(title)) {
            title = "<Unknown Title>";
        }
        if (TextUtils.isEmpty(viewers)) {
            viewers = "0";
        }
        if(TextUtils.isEmpty(description)){
            description = "";
        }

        videoTitle.setText(title);
        videoViewCount.setText(getString(R.string.watch_num_viewers, viewers));
        videoDescription.setText(description);

        progress.animate().alpha(0).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (progress == null) return;
                progress.setVisibility(View.GONE);
            }
        }).start();

        ButterKnife.apply(videoContent, new Action<TypefaceTextView>() {
            @Override
            public void apply(@NonNull TypefaceTextView view, int index) {
                view.setVisibility(View.VISIBLE);
                view.setAlpha(0f);
                view.animate().alpha(1f).start();
            }
        });
    }

    private class BaseCallback implements Callback{

        @Override
        public void onFailure(Call call, IOException e) {
            LOG.error("Error getting Youtube video data for {}", videoID, e);
            if (!BuildConfig.DEBUG) {
                Answers.getInstance()
                      .logCustom(new CustomEvent("Youtube Response Code")
                                       .putCustomAttribute("code", -1)
                                       .putCustomAttribute("reason", e.getLocalizedMessage()));
            }
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if (!BuildConfig.DEBUG) {
                Answers.getInstance()
                      .logCustom(new CustomEvent("Youtube Response Code").putCustomAttribute("code", Integer.toString(response.code())));
            }
            if (response.isSuccessful()) {
                LOG.debug("successfully obtained Youtube data");
                try (ResponseBody body = response.body()) {
                    Gson gson = OA1App.getApp().getGson();
                    final YTResponseBody ytBody = gson.fromJson(body.charStream(), YTResponseBody.class);
                    body.close();
                    if (ytBody != null) {
                        ThreadUtil.getInstance().runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                onResponseParsed(ytBody);
                            }
                        });
                    }
                } catch (Exception e) {
                    LOG.error("Error completing Youtube data call - ", e);
                }
            }
        }

        @UiThread
        void onResponseParsed(final YTResponseBody response) {
            bindResponseToView(response);
        }
    }
}
