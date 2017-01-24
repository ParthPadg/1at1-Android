package org.oneat1.android.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.PlaylistItems;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import org.oneat1.android.util.OA1Util.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by parthpadgaonkar on 1/22/17.
 */

public class API {
    private final static Logger LOG = LoggerFactory.getLogger(API.class);

    private static final YouTube YOUTUBE = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
        @Override
        public void initialize(HttpRequest request) throws IOException {
        } // no op
    }).setApplicationName("1@1").build();
    private static YouTube.Videos.List VIDEO_LIST;
    private static YouTube.PlaylistItems.List PLAYLIST_LIST;

    public interface Callback<T> {
        @UiThread
        void onFailure(IOException e);

        @UiThread
        void onSuccess(List<T> listResponse);
    }

    public static void init(@NonNull Context context) {
        context = context.getApplicationContext();
        String youtubeAPIKey = OA1Config.getInstance(context).getYoutubeAPIKey();
        try {
            VIDEO_LIST = YOUTUBE.videos()
                               .list("snippet, statistics")
                               .setFields("items(id,snippet(title),statistics(viewCount))")
                               .setKey(youtubeAPIKey);

            PLAYLIST_LIST = YOUTUBE.playlistItems()
                                  .list("snippet")
                                  .setKey(youtubeAPIKey)
                                  .setFields("nextPageToken,pageInfo,items(snippet(title,description,thumbnails(standard)))");
        } catch (IOException e) {
            LOG.error("Error initializing Youtube APIs - ", e);
        }
    }

    public static void getVideoList(final String videoID, final API.Callback<Video> callback) {
        ThreadUtil.getInstance().runNowInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    VideoListResponse list = VIDEO_LIST
                                                   .setId(videoID)
                                                   .execute();
                    postToCallback(callback, list.getItems(), null);
                } catch (IOException e) {
                    LOG.error("Error getting video list - ", e);
                    postToCallback(callback, null, e);
                }
            }
        });
    }

    public static void getPlaylistItemList(final String playlistID, final API.Callback<PlaylistItem> callback) {
      ThreadUtil.getInstance().runNowInBackground(new Runnable() {
          @Override
          public void run() {
              try {
                  PlaylistItemListResponse response = PLAYLIST_LIST
                                                            .setId(playlistID) //equivalent to .setPlaylistId
                                                            .execute();
                  List<PlaylistItem> items = new ArrayList<>(response.getPageInfo().getTotalResults());
                  items.addAll(response.getItems());

                  String token = response.getNextPageToken();
                  while (token != null) {
                      PlaylistItems.List modifiedRequest = PLAYLIST_LIST.setPageToken(token);
                      PlaylistItemListResponse pagedResponse = modifiedRequest.execute();
                      items.addAll(pagedResponse.getItems());
                      token = pagedResponse.getNextPageToken();
                  }

                  postToCallback(callback, items, null);
              } catch (IOException e) {
                  LOG.error("Error getting playlist items list - ", e);
                  postToCallback(callback, null, e);
              }
          }
      });

    }

    @WorkerThread
    static <T extends GenericJson> void postToCallback(final Callback<T> callback, final List<T> response, final IOException error) {
        ThreadUtil.getInstance()
              .runOnUIThread(new Runnable() {
                  @Override
                  public void run() {
                      if (error == null) {
                          callback.onSuccess(response);
                      } else {
                          callback.onFailure(error);
                      }
                  }
              });
    }

}
