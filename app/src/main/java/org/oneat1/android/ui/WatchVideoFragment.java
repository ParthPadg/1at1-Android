package org.oneat1.android.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
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
import org.oneat1.android.model.youtube.YTItem;
import org.oneat1.android.model.youtube.YTResponseBody;
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
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
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

    YTResponseBody responseBody;
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

        if (responseBody == null) { // either there was an error saving, or there's no saved state
            getVideoInfo(videoID);
        } else {
            bindResponseToView(responseBody);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        RemoteConfigHelper.get().fetch(false, new CompletionListener() {
            @Override
            public void onComplete(boolean wasSuccessful, @Nullable String youtubeID) {
                if (wasSuccessful) {
                    if (youtubeID != null && !Objects.equals(videoID, youtubeID)) { //new value!
                        videoID = youtubeID;
                        if (youtubePlayer != null && youtubePlayer.isPlaying()) {
                            youtubePlayer.setPlayerStateChangeListener(new PlayerStateChangeListenerAdapter() {
                                @Override
                                public void onVideoEnded() {
                                    youtubePlayer.cueVideo(videoID);
                                }
                            });
                        } else if (youtubePlayer != null) {
                            youtubePlayer.cueVideo(youtubeID);
                        } //nothing we can do; no UI :(
                    }
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
        startActivityForResult(Intent.createChooser(share, "Check out the 1@1 Action!"), 191817);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 191817 && !BuildConfig.DEBUG) {
            Answers.getInstance().logCustom(new CustomEvent("Shared Youtube Video"));
        }
    }

    private void getVideoInfo(String id) {
        String url = BASE_URL.appendQueryParameter("id", id)
                           .appendQueryParameter("key", OA1Config.getInstance(getActivity()).getYoutubeAPIKey())
                           .toString();// equivalent of .build().toString();
        HTTP_CLIENT.newCall(new Builder().url(url).build())
              .enqueue(new BaseCallback());
    }

    //Youtube callback
    @Override
    public void onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
        this.youtubePlayer = player;
        LOG.debug("YOUTUBE player successful init");
        player.setShowFullscreenButton(false);
        if (!wasRestored) {
            player.cueVideo(videoID);
            LOG.debug("queueing video ID {}", videoID);
        }
    }

    //Youtube callback
    @Override
    public void onInitializationFailure(Provider provider, YouTubeInitializationResult errorReason) {
        LOG.error("Error initializing Youtube client: {}", errorReason);
        if(getActivity() == null) return;
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
        if(videoTitle == null || videoViewCount == null) {
            LOG.error("Error - no UI!");
            return;
        }
        String title = null;
        String viewers = null;
        responseBody = response;
        if (response.items != null && response.items.size() > 0) {
            YTItem videoItem = response.items.get(0); //blindly take first item; ideally, there should only be one anyway
            if (videoItem.snippet != null && !TextUtils.isEmpty(videoItem.snippet.title)) {
                title = videoItem.snippet.title;
            }else{
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

        videoTitle.setText(title);
        videoViewCount.setText(getString(R.string.watch_num_viewers, viewers));
    }

    private class BaseCallback implements Callback{

        @Override
        public void onFailure(Call call, IOException e) {
            LOG.error("Error getting Youtube video data for {}", videoID, e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
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
