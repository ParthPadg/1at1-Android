package org.oneat1.android.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;

import org.oneat1.android.R;
import org.oneat1.android.firebase.RemoteConfigHelper;
import org.oneat1.android.firebase.RemoteConfigHelper.RemoteConfigValues;
import org.oneat1.android.model.PlaylistItemResponse.PlaylistItem;
import org.oneat1.android.model.VideoItemResponse.VideoItem;
import org.oneat1.android.network.API;
import org.oneat1.android.util.OA1Util;
import org.oneat1.android.util.Prefs;
import org.oneat1.android.util.TypefaceTextView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import butterknife.BindInt;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.ButterKnife.Action;
import butterknife.OnClick;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;

/**
 * Created by parthpadgaonkar on 1/8/17.
 */
public class VideoPlaylistFragment extends YoutubeAwareFragment {
    private final static Logger LOG = LoggerFactory.getLogger(VideoPlaylistFragment.class);
    private static final String KEY_VIDEO_RESPONSE = "watch.youtube.video.response";
    private static final String KEY_PLAYLIST_RESPONSE = "watch.youtube.playlist.response";
    private static final String KEY_VIDEO_NETWORK_CACHE = "watch.youtube.video.cache";
    private static final int COLLAPSED_NUM_LINES = 4;

    @BindView(R.id.watch_scrollview) NestedScrollView scrollview;
    @BindView(R.id.watch_title) TypefaceTextView videoTitle;
    @BindView(R.id.watch_viewercount) TypefaceTextView videoViewCount;
    @BindView(R.id.watch_description) TypefaceTextView videoDescription;
    @BindView(R.id.watch_description_ellipsis) TypefaceTextView ellipsisText;
    @BindViews({R.id.watch_title, R.id.watch_viewercount, R.id.watch_description, R.id.watch_description_ellipsis}) List<TypefaceTextView> videoContent;
    @BindView(R.id.watch_progress) ProgressBar progress;
    @BindView(R.id.watch_playlist) RecyclerView playlistRecycler;
    @BindView(R.id.watch_action_button_unstarted) TypefaceTextView unstartedButton;
    @BindView(R.id.watch_action_button_completed) TypefaceTextView completedButton;
    @BindView(R.id.playlist_num_completed) TypefaceTextView numVideosCompleted;
    @BindInt(android.R.integer.config_shortAnimTime) int animTime;
    CoordinatorLayout coordinator; //inflated in parent

    private Unbinder unbinder;
    private PlaylistVideoAdapter adapter;
    private String videoID;
    private String playlistID;

    VideoItem memoizedVideo;
    ArrayList<PlaylistItem> memoizedPlaylistItems;
    CompositeDisposable subscription;
    ArrayMap<String, VideoItem> videoIDCache = null;

    public static VideoPlaylistFragment createInstance() {
        return new VideoPlaylistFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        subscription = new CompositeDisposable();
        View view = inflater.inflate(R.layout.fragment_video_playlist, container, false);
        unbinder = ButterKnife.bind(this, view);
        coordinator = ButterKnife.findById(getActivity(), R.id.coordinator);

        playlistRecycler.setAdapter(adapter = new PlaylistVideoAdapter(playlistClickSubject));
        playlistRecycler.setHasFixedSize(false);
        playlistRecycler.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        playlistRecycler.getLayoutManager().setAutoMeasureEnabled(true);

        videoViewCount.setVisibility(View.GONE);
        videoTitle.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        ellipsisText.setVisibility(View.VISIBLE);
        progress.animate().alpha(1).start();

        if (savedInstanceState != null) {
            memoizedVideo = savedInstanceState.getParcelable(KEY_VIDEO_RESPONSE);
            memoizedPlaylistItems = savedInstanceState.getParcelableArrayList(KEY_PLAYLIST_RESPONSE);

            VideoItem[] fromBundle = (VideoItem[]) savedInstanceState.getParcelableArray(KEY_VIDEO_NETWORK_CACHE);
            if (fromBundle != null) {
                videoIDCache = new ArrayMap<>(fromBundle.length);
                for (VideoItem item : fromBundle) {
                    videoIDCache.put(item.getId(), item);
                }
            } else {
                videoIDCache = new ArrayMap<>(12);
            }
        } else {
            videoIDCache = new ArrayMap<>(12);
        }

        RemoteConfigHelper.get()
              .fetch(false, false)
              .subscribe(new BiConsumer<RemoteConfigValues, Throwable>() {
                  @Override
                  public void accept(RemoteConfigValues values, Throwable throwable) throws Exception {
                      videoID = values.getVideoID();
                      playlistID = values.getPlaylistID();
                  }
              });
        if (memoizedVideo == null || memoizedPlaylistItems == null) {
            getAllVideoInfo();
        } else {
            populateMainVideoDetails(memoizedVideo);
            populateAdapter(memoizedPlaylistItems);
        }

        subscription.add(Prefs.getVideoCompletedSignal()
              .observeOn(AndroidSchedulers.mainThread())
              .doOnSubscribe(new Consumer<Disposable>() {
                  @Override
                  public void accept(Disposable disposable) throws Exception {
                      numVideosCompleted.setText(getString(R.string.num_videos_completed, Prefs.getNumVideosCompleted(), Prefs.getNumVideosInPlaylist()));
                  }
              })
              .subscribe(new Consumer<Object>() {
                  @Override
                  public void accept(Object o) throws Exception {
                      numVideosCompleted.setText(getString(R.string.num_videos_completed, Prefs.getNumVideosCompleted(), Prefs.getNumVideosInPlaylist()));
                  }
              }, new Consumer<Throwable>() {
                  @Override
                  public void accept(Throwable throwable) throws Exception {
                      LOG.error("Error with a Prefs subscription? What's up with that?", throwable);
                  }
              }));

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
                          if (throwable != null || newValues == null) {
                              LOG.error("Error obtaining new RemoteConfig values! falling back to old ones!");
                              return;
                          }

                          String newVideoID = newValues.getVideoID();
                          String newPlaylistID = newValues.getPlaylistID();

                          if (!Objects.equals(videoID, newVideoID)
                                    || !Objects.equals(playlistID, newPlaylistID)) {
                              if (newVideoID != null) videoID = newVideoID;
                              if (newPlaylistID != null) playlistID = newPlaylistID;

                              getAllVideoInfo();

                              handleYoutubeIdUpdate();
                          }
                      }
                  });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LOG.debug("saving state!");
        outState.putParcelable(KEY_VIDEO_RESPONSE, memoizedVideo);
        outState.putParcelableArrayList(KEY_PLAYLIST_RESPONSE, memoizedPlaylistItems);

        /* Q: "Why not just use Map.entrySet() to iterate or Map.values().toArray() instead?"
         * A: Because it's expensive for ArrayMap and creates a lot of excess objects for no good reason.
         * This, admittedly, requires a bit of repeated math, but CPUs are good at bit-shifting. */
        if (!videoIDCache.isEmpty()) {
            int size = videoIDCache.size();
            VideoItem[] toBundle = new VideoItem[size];
            for (int i = 0; i < size; i++) {
                toBundle[i] = videoIDCache.valueAt(i);
            }
            outState.putParcelableArray(KEY_VIDEO_NETWORK_CACHE, toBundle);
        }
    }

    @Override
    public void onDestroyView() {
        videoIDCache.clear();
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
        getVideoItemRx(true)
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

    @OnClick(R.id.watch_action_button)
    void onCompletedClick() {
        //check current state
        boolean didComplete = Prefs.didCompleteVideoAction(videoID);
        View inTarget;
        View outTarget;
        Prefs.completedVideoAction(videoID, !didComplete);
        if (!didComplete) {
            inTarget = completedButton;
            outTarget = unstartedButton;
        } else {
            inTarget = unstartedButton;
            outTarget = completedButton;
        }

        adapter.notifyVideoIDChanged(videoID);
        crossfadeButtons(inTarget, outTarget);
    }

    @OnClick({R.id.watch_description, R.id.watch_description_ellipsis})
    void onDescriptionClick() {
        int numLines;
        if (videoDescription.getMaxLines() == COLLAPSED_NUM_LINES) {
            //should expand
            numLines = videoDescription.getLineCount();
            ellipsisText.setVisibility(View.GONE);
        } else {
            //should contract
            numLines = COLLAPSED_NUM_LINES;
            ellipsisText.setVisibility(View.VISIBLE);
            ellipsisText.setAlpha(0f);
            ellipsisText.animate().alpha(1f).start();
        }
        ObjectAnimator animator = ObjectAnimator.ofInt(videoDescription, PROPERTY_MAX_LINES, numLines);
        animator.setDuration(animTime);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                scrollview.smoothScrollTo(0, 0);
            }
        });
        animator.start();
    }

    private void getAllVideoInfo() {
        BiConsumer<Object, Throwable> subscriber = new BiConsumer<Object, Throwable>() {
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
        };

        if (subscription.isDisposed()) {
            subscription = null;
            subscription = new CompositeDisposable();
        }
        //subscribing separately so that errors on one don't trigger a resubscription of both chains.
        subscription.add(getVideoItemRx(false).subscribe(subscriber));
        subscription.add(getPlaylistItemsRx().subscribe(subscriber));
    }

    private Single<VideoItem> getVideoItemRx(boolean useCache) {
        VideoItem cache = useCache ? videoIDCache.get(videoID) : null;
        Single<VideoItem> observable;
        if (cache != null) {
            observable = Single.just(cache);
        } else {
            observable = API.getVideoList(videoID);
        }
        return observable
                     .retry(2)
                     .observeOn(AndroidSchedulers.mainThread())
                     .doOnSuccess(new Consumer<VideoItem>() {
                         @Override
                         public void accept(VideoItem videoItem) throws Exception {
                             videoIDCache.put(videoItem.getId(), videoItem);
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
                                 if (OA1Util.isFragmentDetached(VideoPlaylistFragment.this)) return;
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
                                 Prefs.setNumPlaylistVideos(playlistItems.size());
                                 playlistRecycler.setVisibility(View.VISIBLE);
                                 populateAdapter(playlistItems instanceof ArrayList
                                                       ? (ArrayList<PlaylistItem>) playlistItems
                                                       : new ArrayList<>(playlistItems));
                             }
                         }
                     });
    }

    void populateMainVideoDetails(VideoItem video) {
        if (videoTitle == null || videoViewCount == null) {
            LOG.error("Error - no UI!");
            return;
        }
        String title = video.getTitle();
        String viewers = null;
        String description = video.getDescription();
        memoizedVideo = video;

        if (video.getViewCount() != -1L) {
            NumberFormat numFormat = NumberFormat.getNumberInstance();
            viewers = numFormat.format(video.getViewCount());
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

        if (Prefs.didCompleteVideoAction(video.getId())) {
            crossfadeButtons(completedButton, unstartedButton);
        } else {
            crossfadeButtons(unstartedButton, completedButton);
        }
    }

    void populateAdapter(ArrayList<PlaylistItem> list) {
        adapter.setList(list);
        memoizedPlaylistItems = list;
    }

    static void crossfadeButtons(View inTarget, final View outTarget) {
        if (inTarget.getVisibility() == View.VISIBLE
                  && outTarget.getVisibility() == View.INVISIBLE) {
            //no need to animate what's already correct
            return;
        }

        inTarget.setVisibility(View.VISIBLE);
        inTarget.setAlpha(0f);
        inTarget.animate().alpha(1f);
        outTarget.setVisibility(View.VISIBLE);
        outTarget.setAlpha(1f);
        outTarget
              .animate()
              .alpha(0f)
              .withEndAction(new Runnable() {
                  @Override
                  public void run() {
                      outTarget.setVisibility(View.INVISIBLE);
                  }
              })
              .start();
    }

    static class PlaylistVideoAdapter extends Adapter<PlaylistVideoAdapter.CellViewHolder> {
        List<PlaylistItem> list = Collections.emptyList();
        Observer<PlaylistItem> clickSubject;
        int currItem = 0;

        public PlaylistVideoAdapter(Observer<PlaylistItem> clickSubject) {
            this.clickSubject = clickSubject;
            setHasStableIds(true);
        }

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
            holder.bind(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public long getItemId(int position) {
            return list.get(position).hashCode();
        }

        public void notifyVideoIDChanged(String videoID) {
            if (videoID == null) return; //not sure how that happened, but whatever.
            for (int i = 0, size = list.size(); i < size; i++) {
                if (Objects.equals(list.get(i).getVideoID(), videoID)) {
                    notifyItemChanged(i);
                    return;
                }
            }
        }

        class CellViewHolder extends ViewHolder {
            @BindView(R.id.playlist_thumbnail) ImageView thumbnail;
            @BindView(R.id.playlist_title) TypefaceTextView title;
            @BindView(R.id.playlist_check) CheckBox checkButton;
            @BindView(R.id.video_dot) ImageView videoDot;

            boolean broadcast = false;

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
                          .centerCrop()
                          .into(thumbnail);
                }

                title.setText(item.getTitle());

                broadcast = true;
                final String videoID = item.getVideoID();
                checkButton.setChecked(Prefs.didCompleteVideoAction(videoID));
                broadcast = false;
                videoDot.setVisibility(currItem == getAdapterPosition() ? View.VISIBLE : View.INVISIBLE);
                itemView.setOnClickListener(new DebouncingOnClickListener() {
                    @Override
                    public void doClick(View v) {
                        int pos = getLayoutPosition();
                        if (pos == RecyclerView.NO_POSITION)
                            return; //we're probably in the middle of a layout pass - we'll ignore the click
                        List<PlaylistItem> list = PlaylistVideoAdapter.this.list;
                        if (pos < list.size()) {
                            int prev = currItem;
                            currItem = pos;
                            PlaylistItem item = list.get(pos);
                            clickSubject.onNext(item);
                            notifyItemChanged(prev);
                            notifyItemChanged(currItem);
                        }
                    }
                });

                checkButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (broadcast) return;
                        Prefs.completedVideoAction(videoID, isChecked);
                    }
                });
            }
        }
    }

    private static final Property<TypefaceTextView, Integer> PROPERTY_MAX_LINES = new Property<TypefaceTextView, Integer>(Integer.class, "MaxLines") {
        @Override
        public Integer get(TypefaceTextView object) {
            return object.getMaxLines();
        }

        @Override
        public void set(TypefaceTextView object, Integer value) {
            object.setMaxLines(value.intValue()); //avoids more autoboxing than necessary
        }
    };

}
