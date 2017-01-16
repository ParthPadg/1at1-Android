package org.oneat1.android.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.gson.Gson;

import org.oneat1.android.BuildConfig;
import org.oneat1.android.OA1App;
import org.oneat1.android.R;
import org.oneat1.android.model.youtube.YTItem;
import org.oneat1.android.model.youtube.YTResponseBody;
import org.oneat1.android.util.OA1Config;
import org.oneat1.android.util.OA1Util;
import org.oneat1.android.util.OA1Util.ThreadUtil;
import org.oneat1.android.util.TypefaceTextView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
    private static final long UPDATE_THRESHOLD = TimeUnit.MINUTES.toMillis(2);
    private static final Uri.Builder BASE_URL = Uri.parse("https://www.googleapis.com/youtube/v3/videos")
                                                      .buildUpon()
                                                      .appendQueryParameter("part", "snippet, statistics");
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
                                                          .addInterceptor(new HttpLoggingInterceptor().setLevel(Level.BASIC))
                                                          .build();

    @BindView(R.id.watch_title) TypefaceTextView videoTitle;
    @BindView(R.id.watch_viewercount) TypefaceTextView videoViewCount;

    private Unbinder unbinder;
    private Handler handler = new Handler(Looper.getMainLooper());
    private String videoID = BuildConfig.DEBUG
                                   ? "YB_nM296Ky8" //fake live video
                                   : null;

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

        getVideoInfo(videoID);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        OA1Util.safeUnbind(unbinder);
    }

    @OnClick(R.id.watch_share)
    void onShareClick() {
        Intent share = new Intent(Intent.ACTION_SEND)
                              .setType("text/plain")
                              .putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=" + videoID)
                              .putExtra(Intent.EXTRA_SUBJECT, "Check out the 1@1 action for national equality!");
        startActivity(share);
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
        LOG.debug("YOUTUBE player successful init");
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

    private void bindResponseToView(YTResponseBody response) {
        String title = null;
        String viewers = null;
        String liveViewers = null;
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
        if (liveViewers == null) {
            videoViewCount.setText(getString(R.string.watch_num_viewers, viewers));
        } else {
            videoViewCount.setText(getString(R.string.watch_num_viewers_live, viewers, liveViewers));
        }
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
