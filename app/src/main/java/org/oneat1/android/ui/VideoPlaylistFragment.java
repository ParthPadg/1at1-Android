package org.oneat1.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.ShareEvent;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.api.client.util.Collections2;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatistics;
import com.twitter.sdk.android.core.internal.scribe.DefaultScribeClient;

import org.oneat1.android.BuildConfig;
import org.oneat1.android.R;
import org.oneat1.android.firebase.RemoteConfigHelper;
import org.oneat1.android.firebase.RemoteConfigHelper.CompletionListener;
import org.oneat1.android.model.ParcelableVideoItem;
import org.oneat1.android.ui.VideoPlaylistFragment.PlaylistVideoAdapter.CellViewHolder;
import org.oneat1.android.util.API;
import org.oneat1.android.util.API.Callback;
import org.oneat1.android.util.OA1Config;
import org.oneat1.android.util.OA1Util;
import org.oneat1.android.util.PlayerStateChangeListenerAdapter;
import org.oneat1.android.util.TypefaceTextView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by parthpadgaonkar on 1/8/17.
 */
public class VideoPlaylistFragment extends Fragment implements OnInitializedListener {
    private final static Logger LOG = LoggerFactory.getLogger(VideoPlaylistFragment.class);
    private static final String KEY_RESPONSE = "watch.youtube.response";

    @BindView(R.id.watch_title) TypefaceTextView videoTitle;
    @BindView(R.id.watch_viewercount) TypefaceTextView videoViewCount;
    @BindView(R.id.watch_progress) ProgressBar progress;
    @BindView(R.id.watch_playlist) RecyclerView playlistRecycler;

    ParcelableVideoItem videoResopnse;
    private Unbinder unbinder;
    private String videoID = RemoteConfigHelper.get().getYoutubeID();
    private String playlistID = RemoteConfigHelper.get().getPlaylistID();
    private YouTubePlayer youtubePlayer;
    private PlaylistVideoAdapter adapter;


    public static VideoPlaylistFragment createInstance() {
        return new VideoPlaylistFragment();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_playlist, container, false);
        unbinder = ButterKnife.bind(this, view);

        adapter = new PlaylistVideoAdapter();
        adapter.setHasStableIds(true);
        playlistRecycler.setHasFixedSize(true);
        playlistRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));


        progress.setVisibility(View.VISIBLE);
        progress.animate()
              .alpha(1)
              .start();
        videoViewCount.setVisibility(View.GONE);
        videoTitle.setVisibility(View.GONE);

        YouTubePlayerFragment f = (YouTubePlayerFragment) getChildFragmentManager().findFragmentById(R.id.watch_fragment);
        if (f != null) { //if this is null, we're *so* boned...
            f.initialize(OA1Config.getInstance(getActivity()).getYoutubeAPIKey(), this);
        }

     /*   LOG.debug("savedInstanceState is null: {}", savedInstanceState == null);
        if (savedInstanceState != null) {
            responseBody = OA1App.getApp()
                                 .getGson()
                                 .fromJson(savedInstanceState.getString(KEY_RESPONSE), YTResponseBody.class);
        }

        if (responseBody == null) { // either there was an error saving, or there's no saved state
            getVideoInfo(videoID);
        } else {
            populateMainVideoDetails(responseBody);
        }*/

        getVideoInfo(videoID);
        getPlaylistInfo(videoID);

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        OA1Util.safeUnbind(unbinder);
    }

    /*@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LOG.debug("saving state!");
        outState.putString(KEY_RESPONSE, OA1App.getApp().getGson().toJson(responseBody));
    }*/

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
        if (requestCode == 191817 && !BuildConfig.DEBUG && resultCode == Activity.RESULT_OK) {
            Answers.getInstance().logShare(new ShareEvent().putContentName("Youtube").putContentId(videoID));
        }
    }

    private void getVideoInfo(String id) {
        API.getVideoList(id, new API.Callback<Video>() {
            @Override
            public void onFailure(IOException e) {
                LOG.error("error loading vide list - ", e);
            }

            @Override
            public void onSuccess(List<Video> videos) {
                if (videos != null && videos.isEmpty()) {
                    populateMainVideoDetails(videos.get(0));
                }

            }
        });
    }

    private void getPlaylistInfo(String playlistID) {
        API.getPlaylistItemList(playlistID, new Callback<PlaylistItem>() {
            @Override
            public void onFailure(IOException e) {
                LOG.error("error obtaining playlist items: ", e);
                playlistRecycler.animate()
                      .alpha(0f)
                      .withEndAction(new Runnable() {
                          @Override
                          public void run() {
                              playlistRecycler.setVisibility(View.GONE);
                          }
                      })
                      .start();
            }

            @Override
            public void onSuccess(List<PlaylistItem> items) {
                adapter.setList(items);
            }
        });
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

    void populateMainVideoDetails(Video video) {
        if (videoTitle == null || videoViewCount == null) {
            LOG.error("Error - no UI!");
            return;
        }
        String title = null;
        String viewers = null;
        //TODO memoize video

        VideoSnippet snippet = video.getSnippet();
        if (snippet != null && !TextUtils.isEmpty(snippet.getTitle())) {
            title = snippet.getTitle();
        } else {
            LOG.warn("snippet.title is null!");
        }

        VideoStatistics statistics = video.getStatistics();
        if (statistics != null && statistics.getViewCount() != null) {
            NumberFormat numFormat = NumberFormat.getNumberInstance();
            viewers = numFormat.format(statistics.getViewCount());
        } else {
            LOG.warn("statistics.viewcount is null!");
        }


        if (TextUtils.isEmpty(title)) {
            title = "<Unknown Title>";
        }
        if (TextUtils.isEmpty(viewers)) {
            viewers = "0";
        }

        videoTitle.setText(title);
        videoViewCount.setText(getString(R.string.watch_num_viewers, viewers));

        videoTitle.setVisibility(View.VISIBLE);
        videoTitle.setAlpha(0f);
        videoViewCount.setVisibility(View.VISIBLE);
        videoViewCount.setAlpha(0f);

        progress.animate().alpha(0).withEndAction(new Runnable() {
            @Override
            public void run() {
                progress.setVisibility(View.GONE);
            }
        }).start();
        videoTitle.animate().alpha(1f).start();
        videoViewCount.animate().alpha(1f).start();
    }

    static class PlaylistVideoAdapter extends Adapter<CellViewHolder> {

        private List<PlaylistItem> list = Collections.emptyList();

        void setList(List<PlaylistItem> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @Override
        public CellViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View cell = LayoutInflater.from(parent.getContext())
                              .inflate(R.layout.watch_video_playlist_cell, parent, false);
            return new CellViewHolder(cell);
        }

        @Override
        public void onBindViewHolder(CellViewHolder holder, int position) {
            PlaylistItem item = list.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }


        @Override
        public long getItemId(int position) {
            return list.get(position).getId().hashCode();
        }

        static class CellViewHolder extends ViewHolder {
            @BindView(R.id.playlist_thumbnail) ImageView thumbnail;
            @BindView(R.id.playlist_title) TypefaceTextView title;
            @BindView(R.id.playlist_descr) TypefaceTextView description;

            CellViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            void bind(PlaylistItem item) {
                PlaylistItemSnippet snippet = item.getSnippet();

                Glide.with(thumbnail.getContext())
                      .load(snippet.getThumbnails().getStandard().getUrl())
                      .crossFade()
                      .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                      .fitCenter()
                      .into(thumbnail);

                title.setText(snippet.getTitle());
                description.setText(snippet.getDescription());
            }
        }
    }
}
