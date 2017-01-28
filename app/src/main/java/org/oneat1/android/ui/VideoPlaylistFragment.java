package org.oneat1.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
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

import org.oneat1.android.BuildConfig;
import org.oneat1.android.R;
import org.oneat1.android.firebase.RemoteConfigHelper;
import org.oneat1.android.firebase.RemoteConfigHelper.RemoteConfigValues;
import org.oneat1.android.model.PlaylistItemResponse;
import org.oneat1.android.model.PlaylistItemResponse.PlaylistItem;
import org.oneat1.android.model.VideoItemResponse.VideoItem;
import org.oneat1.android.util.API;
import org.oneat1.android.util.OA1Config;
import org.oneat1.android.util.OA1Util;
import org.oneat1.android.util.TypefaceTextView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BiFunction;

/**
 * Created by parthpadgaonkar on 1/8/17.
 */
public class VideoPlaylistFragment extends Fragment implements OnInitializedListener {
    private final static Logger LOG = LoggerFactory.getLogger(VideoPlaylistFragment.class);
    private static final String KEY_VIDEO_RESPONSE= "watch.youtube.video.response";
    private static final String KEY_PLAYLIST_RESPONSE = "watch.youtube.playlist.response";

    @BindView(R.id.watch_title) TypefaceTextView videoTitle;
    @BindView(R.id.watch_viewercount) TypefaceTextView videoViewCount;
    @BindView(R.id.watch_progress) ProgressBar progress;
    @BindView(R.id.watch_playlist) RecyclerView playlistRecycler;

    private Unbinder unbinder;
    private YouTubePlayer youtubePlayer;
    private PlaylistVideoAdapter adapter;
    private String videoID;
    private String playlistID;

    VideoItem memoizedVideo;
    List<PlaylistItem> memoizedPlaylistItems;
    Disposable subscription;

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

        if(savedInstanceState != null){
            memoizedVideo = savedInstanceState.getParcelable(KEY_VIDEO_RESPONSE);
            memoizedPlaylistItems = savedInstanceState.getParcelableArrayList(KEY_PLAYLIST_RESPONSE);
        }

        //okay for this to be blocking because we're requesting NO NETWORK
        RemoteConfigValues val = RemoteConfigHelper.get().fetch(false, false).blockingGet();
        videoID = val.getVideoID();
        playlistID = val.getPlaylistID();

        if(memoizedVideo == null || memoizedPlaylistItems == null){
            getAllVideoInfo();
        }else{
            populateMainVideoDetails(memoizedVideo);
            populateAdapter(memoizedPlaylistItems);
        }

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            RemoteConfigHelper.get()
                  .fetch(false, true)
                  .subscribe(new BiConsumer<RemoteConfigValues, Throwable>() {
                      @Override
                      public void accept(RemoteConfigValues newValues, Throwable throwable) throws Exception {
                          if (throwable != null) {
                              LOG.error("Error obtaining new RemoteConfig values! falling back to old ones!");
                              return;
                          }

                          videoID = newValues.getVideoID();
                          playlistID = newValues.getPlaylistID();

                          if (youtubePlayer != null) {
                              if (youtubePlayer.isPlaying()) {
                                  youtubePlayer.loadVideo(videoID);
                              } else {
                                  youtubePlayer.cueVideo(videoID);
                              }
                          } // the player isn't loaded - we'll just have to wait :(
                      }
                  });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LOG.debug("saving state!");
        outState.putParcelable(KEY_VIDEO_RESPONSE, memoizedVideo);
        outState.putParcelableArrayList(KEY_PLAYLIST_RESPONSE, (ArrayList<? extends Parcelable>) memoizedPlaylistItems); //we're pretty damned sure it's an ArrayList
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 191817 && !BuildConfig.DEBUG && resultCode == Activity.RESULT_OK) {
            Answers.getInstance().logShare(new ShareEvent().putContentName("Youtube").putContentId(videoID));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        OA1Util.safeUnbind(unbinder);
        subscription.dispose();
    }

    @OnClick(R.id.watch_share)
    void onShareClick() {
        Intent share = new Intent(Intent.ACTION_SEND)
                             .setType("text/plain")
                             .putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=" + videoID);
        startActivityForResult(Intent.createChooser(share, "Check out the 1@1 Action!"), 191817);
    }

    private void getAllVideoInfo() {
        Single<VideoItem> videoObservable =
              API.getVideoList(videoID)
                    .retry(2)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnEvent(new BiConsumer<VideoItem, Throwable>() {
                        @Override
                        public void accept(VideoItem videoItem, Throwable throwable) throws Exception {
                            if (throwable != null) {
                                LOG.error("Error while loading video list!", throwable);
                            } else {
                                populateMainVideoDetails(videoItem);
                            }
                        }
                    });

        Single<List<PlaylistItem>> playlistObservable =
              API.getPlaylistItemList(playlistID)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnEvent(new BiConsumer<List<PlaylistItem>, Throwable>() {
                        @Override
                        public void accept(List<PlaylistItem> playlistItems, Throwable throwable) throws Exception {
                            if (throwable != null) {
                                LOG.error("error obtaining playlist items: ", throwable);
                                if (OA1Util.isFragmentDetached(VideoPlaylistFragment.this))
                                    return;
                                playlistRecycler.animate()
                                      .alpha(0f)
                                      .withEndAction(new Runnable() {
                                          @Override
                                          public void run() {
                                              playlistRecycler.setVisibility(View.GONE);
                                          }
                                      })
                                      .start();
                            } else {
                                populateAdapter(playlistItems);
                            }

                        }
                    });

        subscription = Single.zip(videoObservable, playlistObservable, new BiFunction<VideoItem, List<PlaylistItem>, Object>() {
            @Override
            public Object apply(VideoItem videoItem, List<PlaylistItem> playlistItems) throws Exception {
                return Boolean.TRUE; //we don't care about the result of the zip; we just wanted to kick off the two network calls
            }
        }).subscribe(new BiConsumer<Object, Throwable>() {
            @Override
            public void accept(Object o, Throwable throwable) throws Exception {
                if (throwable != null) {
                    //TODO
                }
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

    void populateMainVideoDetails(VideoItem video) {
        if (videoTitle == null || videoViewCount == null) {
            LOG.error("Error - no UI!");
            return;
        }
        String title = null;
        String viewers = null;
        memoizedVideo = video;


        VideoItem.Snippet snippet = video.snippet;
        if (snippet != null && !TextUtils.isEmpty(snippet.title)) {
            title = snippet.title;
        } else {
            LOG.warn("snippet.title is null!");
        }

        VideoItem.Statistics statistics = video.statistics;
        if (statistics != null && statistics.viewCount != null) {
            NumberFormat numFormat = NumberFormat.getNumberInstance();
            viewers = numFormat.format(statistics.viewCount);
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

    void populateAdapter(List<PlaylistItem> list) {
        adapter.setList(list);
        memoizedPlaylistItems = list;
    }

    static class PlaylistVideoAdapter extends Adapter<PlaylistVideoAdapter.CellViewHolder> {

        private List<PlaylistItem> list = Collections.emptyList();

        void setList(List<PlaylistItemResponse.PlaylistItem> list) {
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

        static class CellViewHolder extends ViewHolder {
            @BindView(R.id.playlist_thumbnail) ImageView thumbnail;
            @BindView(R.id.playlist_title) TypefaceTextView title;
            @BindView(R.id.playlist_descr) TypefaceTextView description;

            CellViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            void bind(PlaylistItem item) {
                PlaylistItem.Snippet snippet = item.snippet;
                Glide.with(thumbnail.getContext())
                      .load(snippet.thumbnails.standard.url) //todo make this not as scary
                      .crossFade()
                      .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                      .fitCenter()
                      .into(thumbnail);

                title.setText(snippet.title);
                description.setText(snippet.description);
            }
        }
    }
}
