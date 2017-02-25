package org.oneat1.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ShareEvent;

import org.oneat1.android.BuildConfig;
import org.oneat1.android.R;
import org.oneat1.android.firebase.RemoteConfigHelper;
import org.oneat1.android.firebase.RemoteConfigHelper.RemoteConfigValues;
import org.oneat1.android.model.PlaylistItemResponse;
import org.oneat1.android.model.PlaylistItemResponse.PlaylistItem;
import org.oneat1.android.model.VideoItemResponse.VideoItem;
import org.oneat1.android.network.API;
import org.oneat1.android.ui.VideoPlaylistFragment.PlaylistVideoAdapter.CellViewHolder;
import org.oneat1.android.util.OA1Util;
import org.oneat1.android.util.TypefaceTextView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.ButterKnife.Action;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;

/**
 * Created by parthpadgaonkar on 1/8/17.
 */
public class VideoPlaylistFragment extends YoutubeAwareFragment {
    private final static Logger LOG = LoggerFactory.getLogger(VideoPlaylistFragment.class);
    private static final String KEY_VIDEO_RESPONSE= "watch.youtube.video.response";
    private static final String KEY_PLAYLIST_RESPONSE = "watch.youtube.playlist.response";

    @BindView(R.id.watch_title) TypefaceTextView videoTitle;
    @BindView(R.id.watch_viewercount) TypefaceTextView videoViewCount;
//    @BindView(R.id.watch_description) TypefaceTextView videoDescription;
    @BindViews({R.id.watch_title, R.id.watch_viewercount/*, R.id.watch_description*/}) List<TypefaceTextView> videoContent;
    @BindView(R.id.watch_progress) ProgressBar progress;
    @BindView(R.id.watch_playlist) RecyclerView playlistRecycler;
    CoordinatorLayout coordinator; //inflated in parent, so we can't use ButterKnife :(

    private Unbinder unbinder;
    private PlaylistVideoAdapter adapter;
    private String videoID;
    private String playlistID;

    boolean requestedYoutubeInfo = false;
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
        coordinator = ButterKnife.findById(getActivity(), R.id.coordinator);

        playlistRecycler.setAdapter(adapter = new PlaylistVideoAdapter(playlistClickSubject));
        playlistRecycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.HORIZONTAL));
        playlistRecycler.setHasFixedSize(false);
        playlistRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        progress.setVisibility(View.VISIBLE);
        progress.animate()
              .alpha(1)
              .start();
        videoViewCount.setVisibility(View.GONE);
        videoTitle.setVisibility(View.GONE);

        if (savedInstanceState != null) {
            memoizedVideo = savedInstanceState.getParcelable(KEY_VIDEO_RESPONSE);
            memoizedPlaylistItems = savedInstanceState.getParcelableArrayList(KEY_PLAYLIST_RESPONSE);
        }

        //okay for this to be blocking because we're requesting NO NETWORK
        RemoteConfigValues val = RemoteConfigHelper.get().fetch(false, false).blockingGet();
        videoID = val.getVideoID();
        playlistID = val.getPlaylistID();

        if (memoizedVideo == null || memoizedPlaylistItems == null) {
            getAllVideoInfo();
        } else {
            populateMainVideoDetails(memoizedVideo); //TODO debug
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

                          String newVideoID = newValues.getVideoID();
                          String newPlaylistID = newValues.getPlaylistID();

                          boolean updated = !Objects.equals(videoID, newVideoID)
                                                  || !Objects.equals(playlistID, newPlaylistID);

                          if (updated || !requestedYoutubeInfo) {
                              if (newVideoID != null) videoID = newVideoID;
                              if (newPlaylistID != null) playlistID = newVideoID;

                              getAllVideoInfo();
                          }

                          handleYoutubeIdUpdate();
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
        OA1Util.safeUnbind(unbinder);
        if (subscription != null) {
            subscription.dispose();
        }
        super.onDestroyView();
    }

    @Override
    protected String getCurrentVideoID() {
        return videoID;
    }

    @Override
    protected void setCurrentVideoID(String newVideoID) {
        videoID = newVideoID;
        getVideoItemRx()
              .subscribe(new BiConsumer<VideoItem, Throwable>() {
                  @Override
                  public void accept(VideoItem videoItem, Throwable throwable) throws Exception {
                      //not worried about the videoItem; we've already handled that
                      if (throwable != null) {
                          LOG.error("Error refreshing video information - ", throwable);
                      }
                  }
              });
    }

    @OnClick(R.id.watch_share)
    void onShareClick() {
        Intent share = new Intent(Intent.ACTION_SEND)
                             .setType("text/plain")
                             .putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=" + videoID);
        startActivityForResult(Intent.createChooser(share, "Check out the 1@1 Action!"), 191817);
    }

    private void getAllVideoInfo() {
        subscription = Single.zip(getVideoItemRx(), getPlaylistItemsRx(), new BiFunction<VideoItem, List<PlaylistItem>, Object>() {
            @Override
            public Object apply(VideoItem videoItem, List<PlaylistItem> playlistItems) throws Exception {
                return Boolean.TRUE; //we don't care about the result of the zip; we just wanted to kick off the two network calls
            }
        }).subscribe(new BiConsumer<Object, Throwable>() {
            @Override
            public void accept(Object o, Throwable throwable) throws Exception {
                if (throwable != null) {
                    LOG.error("Error loading video and playlist info: ", throwable);
                    Snackbar.make(coordinator,
                          "There was an error loading the video information - please try again",
                          Snackbar.LENGTH_SHORT)
                          .show();
                }
            }
        });
    }

    private Single<VideoItem> getVideoItemRx() {
        return API.getVideoList(videoID)
              .retry(2)
              .observeOn(AndroidSchedulers.mainThread())
              .doOnSuccess(new Consumer<VideoItem>() {
                  @Override
                  public void accept(VideoItem videoItem) throws Exception {
                      populateMainVideoDetails(videoItem);
                  }
              });
    }

    private Single<List<PlaylistItem>> getPlaylistItemsRx() {
        return API.getPlaylistItemList(playlistID)
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
                                 playlistRecycler.setVisibility(View.VISIBLE);
                                 populateAdapter(playlistItems);
                             }
                         }
                     });
    }

    void populateMainVideoDetails(VideoItem video) {
        if (videoTitle == null || videoViewCount == null) {
            LOG.error("Error - no UI!");
            return;
        }
        String title = null;
        String viewers = null;
        String description = null;
        memoizedVideo = video;

        VideoItem.Snippet snippet = video.snippet;
        if (snippet != null) {
            if (!TextUtils.isEmpty(snippet.title)) {
                title = snippet.title;
            } else {
                LOG.warn("snippet.title is null!");
            }
            if (!TextUtils.isEmpty(snippet.description)) {
                description = snippet.description;
            } else {
                LOG.warn("snippet.description is null!");
            }
        } else {
            LOG.warn("snippet.title is null!");
        }

        VideoItem.Statistics statistics = video.statistics;
        if (statistics != null && statistics.viewCount != Long.MIN_VALUE) {
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
        if (TextUtils.isEmpty(description)) {
            description = "";
        }

        videoTitle.setText(title);
        videoViewCount.setText(getString(R.string.watch_num_viewers, viewers));
//        videoDescription.setText(description);

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

    void populateAdapter(List<PlaylistItem> list) {
        adapter.setList(list);
        memoizedPlaylistItems = list;
    }

    static class PlaylistVideoAdapter extends Adapter<PlaylistVideoAdapter.CellViewHolder> {

        List<PlaylistItem> list = Collections.emptyList();
        Observer<PlaylistItem> clickSubject;

        public PlaylistVideoAdapter(Observer<PlaylistItem> clickSubject) {
            this.clickSubject = clickSubject;
        }

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
            holder.bind(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class CellViewHolder extends ViewHolder {
            @BindView(R.id.playlist_thumbnail) ImageView thumbnail;
            @BindView(R.id.playlist_title) TypefaceTextView title;
//            @BindView(R.id.playlist_descr) TypefaceTextView description;

            CellViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            void bind(PlaylistItem item) {
                if (!TextUtils.isEmpty(item.getThumnailURL())) {
                    Glide.with(thumbnail.getContext())
                          .load(item.getThumnailURL())
                          .crossFade()
                          .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                          .fitCenter()
                          .into(thumbnail);
                }

                title.setText(item.getTitle());
//                description.setText(item.getDescription());
                super.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        List<PlaylistItem> list = PlaylistVideoAdapter.this.list;
                        int pos = getLayoutPosition();
                        if (pos < list.size()) {
                            clickSubject.onNext(list.get(pos));
                        }
                    }
                });
            }
        }

    }


}
