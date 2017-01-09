package org.oneat1.android.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.gson.Gson;

import org.oneat1.android.OA1App;
import org.oneat1.android.R;
import org.oneat1.android.model.youtube.YTItem;
import org.oneat1.android.model.youtube.YTResponseBody;
import org.oneat1.android.util.OA1Config;
import org.oneat1.android.util.OA1Util;
import org.oneat1.android.util.TypefaceTextView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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

/**
 * Created by parthpadgaonkar on 1/8/17.
 */
public class WatchVideoFragment extends Fragment implements Callback, OnInitializedListener {
    private final static Logger LOG = LoggerFactory.getLogger(WatchVideoFragment.class);

    @BindView(R.id.watch_fragment) FrameLayout videoFragment;
    @BindView(R.id.watch_title) TypefaceTextView videoTitle;
    @BindView(R.id.watch_viewercount) TypefaceTextView videoViewCount;

    private Unbinder unbinder;
    private String videoID = "gymPFiPE88I";

    public static WatchVideoFragment createInstance() {
        return new WatchVideoFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_watch, container, false);
        unbinder = ButterKnife.bind(this, view);

        YouTubePlayerFragment frag = YouTubePlayerFragment.newInstance();
        getChildFragmentManager()
              .beginTransaction()
              .replace(videoFragment.getId(), frag)
              .commit();
        frag.initialize(OA1Config.getInstance(getActivity()).getYoutubeAPIKey(), this);

        getVideoInfo(videoID); //eagle video


        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        OA1Util.safeUnbind(unbinder);
    }

    @OnClick(R.id.watch_share)
    void onShareClick() {

    }

    private void getVideoInfo(String id) {
        String url = Uri.parse("https://www.googleapis.com/youtube/v3/videos")
                           .buildUpon()
                           .appendQueryParameter("part", "snippet, statistics")
                           .appendQueryParameter("id", id)
                           .appendQueryParameter("key", OA1Config.getInstance(getActivity()).getYoutubeAPIKey())
                           .toString();// equivalent of .build().toString();
        OkHttpClient client = new OkHttpClient();
        client.newCall(new Builder()
                             .url(url)
                             .get()
                             .build())
              .enqueue(this);
    }

    private void bindResponseToView(YTResponseBody response) {
        if (response.items != null && response.items.size() > 0) {
            YTItem videoItem = response.items.get(0); //blindly take first item
            if (videoItem.snippet != null && !TextUtils.isEmpty(videoItem.snippet.title)) {
                videoTitle.setText(videoItem.snippet.title);
            } else {
                videoTitle.setText("<Unknown Video Title>");
            }

            if (videoItem.snippet != null && !TextUtils.isEmpty(videoItem.statistics.viewCount)) {
                videoViewCount.setText(getString(R.string.watch_num_viewers, videoItem.statistics.viewCount));
            }
        }
    }

    //OKHTTP callback
    @Override
    public void onResponse(Call call, Response response) {
        if (response.isSuccessful()) {
            LOG.debug("successfully obtained Youtube data - parsing now!");
            try (ResponseBody body = response.body()) {
                Gson gson = OA1App.getInstance().getGson();
                YTResponseBody ytBody = gson.fromJson(body.charStream(), YTResponseBody.class);
                if(ytBody !=null) {
                    bindResponseToView(ytBody);
                }
            } catch (Exception e) {
                LOG.error("Error completing Youtube data call - ", e);
            }
        }
    }

    //OKHTTP callback
    @Override
    public void onFailure(Call call, IOException e) {
        //TODO
    }

    //Youtube callback
    @Override
    public void onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
        if(!wasRestored){
            player.cueVideo(videoID);
        }
    }

    //Youtube callback
    @Override
    public void onInitializationFailure(Provider provider, YouTubeInitializationResult errorReason) {
        if (errorReason.isUserRecoverableError()) {
            errorReason.getErrorDialog(getActivity(), 19181).show();
        } else {
            new AlertDialog.Builder(getActivity())
                  .setTitle("Error Loading YouTube")
                  .setMessage("It seems like there was an error loading the YouTube player; please ensure that the YouTube app is installed.")
                  .show();
        }
    }
}
