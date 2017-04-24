package org.oneat1.android.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.oneat1.android.BuildConfig;
import org.oneat1.android.model.PlaylistItemResponse;
import org.oneat1.android.model.PlaylistItemResponse.PlaylistItem;
import org.oneat1.android.model.VideoItemResponse;
import org.oneat1.android.model.VideoItemResponse.VideoItem;
import org.oneat1.android.util.OA1Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by parthpadgaonkar on 1/22/17.
 */

public class API {
    private final static Logger LOG = LoggerFactory.getLogger(API.class);

    interface YouTubeAPI {
        @GET("videos?part=snippet,statistics&fields=items(id,snippet(title,description),statistics(viewCount))")
        Single<VideoItemResponse> getVideoList(@Query("key") String apiKey, @Query("id") String videoID);

        @GET("playlistItems?part=snippet&fields=nextPageToken,pageInfo,items(id,snippet(title,thumbnails(medium),resourceId(videoId)))")
        Observable<PlaylistItemResponse> getPlaylistItems(@Query("key") String apiKey,
                                                               @Query("playlistId") String playlistID,
                                                               @Query("pageToken") @Nullable String pageToken,
                                                               @Query("maxResults") int maxResult);
    }

    private static String youtubeAPIKey;
    private static YouTubeAPI _api;

    public static void init(@NonNull Context context) {
        context = context.getApplicationContext();
        youtubeAPIKey = OA1Config.getInstance(context).getYoutubeAPIKey();

        OkHttpClient okHttpClient = new Builder()
                                          .addInterceptor(new HttpLoggingInterceptor().setLevel(BuildConfig.DEBUG ? Level.BASIC : Level.NONE))
                                          .build();
        _api = new Retrofit.Builder()
                     .client(okHttpClient)
                     .baseUrl("https://www.googleapis.com/youtube/v3/")
                     .addConverterFactory(GsonConverterFactory.create())
                     .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                     .validateEagerly(true)
                     .build()
                     .create(YouTubeAPI.class);
    }

    /**
     * In the event that the YouTube v3 videos.list API returns more than 1 item for the VideoID query,
     * this method will only emit the first item.
     */
    public static Single<VideoItem> getVideoList(final String videoID) {
        return _api.getVideoList(youtubeAPIKey, videoID)
                     .map(new Function<VideoItemResponse, VideoItem>() {
                         @Override
                         public VideoItem apply(VideoItemResponse container) throws Exception {
                             //will throw NPE or bad index, but that's okay.
                             List<VideoItem> items = container.getItems();
                             LOG.debug("container has {} VideoItems", items.size());
                             return items.get(0);
                         }
                     });
    }

    public static Single<List<PlaylistItem>> getPlaylistItemList(final String playlistID) {
        return getPlaylistItemList(playlistID, null)
                     .collectInto(new LinkedList<PlaylistItem>(), new BiConsumer<LinkedList<PlaylistItem>, List<PlaylistItem>>() {
                         @Override
                         public void accept(LinkedList<PlaylistItem> accumulator, List<PlaylistItem> toAdd) throws Exception {
                             accumulator.addAll(toAdd);
                         }
                     })
                     //not thrilled that this a little verbose, but LinkedList is good for an unknown number of items, and ArrayList is RandomAccess...what to do??
                     .map(new Function<LinkedList<PlaylistItem>, List<PlaylistItem>>() {
                         @Override
                         public List<PlaylistItem> apply(LinkedList<PlaylistItem> items) throws Exception {
                             return new ArrayList<>(items);
                         }
                     });
    }

    //could cause a stackoverflow if we recurse too deeply, but given that this playlist is going to have <200 items
    // and the page size is 50, I'm not worried about it.
    private static Observable<List<PlaylistItem>> getPlaylistItemList(final String playlistID, final String pageToken) {
        return _api.getPlaylistItems(youtubeAPIKey, playlistID, pageToken, 50) //always request 50 items in the response
                     .retry(2)
                     .concatMap(new Function<PlaylistItemResponse, ObservableSource<List<PlaylistItem>>>() {
                         @Override
                         public ObservableSource<List<PlaylistItem>> apply(PlaylistItemResponse response) throws Exception {
                             Observable<List<PlaylistItem>> base = Observable.just(response.getItems());
                             if (TextUtils.isEmpty(response.getPageToken())) {
                                 return base;
                             } else {
                                 return Observable.concat(base, getPlaylistItemList(playlistID, response.getPageToken()));
                             }
                         }
                     });
    }
}
