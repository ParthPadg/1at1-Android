package org.oneat1.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.api.services.youtube.YouTube.Thumbnails;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.ThumbnailDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by parthpadgaonkar on 1/23/17.
 */

public class ParcelablePlaylistItem implements Parcelable {
    private List<PlaylistItem> playlist;

    /* Reads/Writes in the following order:
    * 1. PlaylistItem.getSnippet().getTitle()
    * 2. PlaylistItem.getSnippet().getDescription()
    * 3. PlaylistItem.getSnippet.getThumbnails().getStandard().getUrl()
    * 4. PlaylistItem.getSnippet.getThumbnails().getStandard().getWidth()
    * 5. PlaylistItem.getSnippet.getThumbnails().getStandard().getHeight()
    * */
    protected ParcelablePlaylistItem(List<PlaylistItem> playlist) {
        this.playlist = playlist;
    }

    public static final Creator<List<PlaylistItem>> CREATOR = new Creator<List<PlaylistItem>>() {
        @Override
        public List<PlaylistItem> createFromParcel(Parcel in) {
            final int size = in.readInt();
            List<PlaylistItem> list = new ArrayList<>(size);
            if(size > 0){
                for (int i = 0; i < size; i++) {
                    PlaylistItemSnippet snippet = new PlaylistItemSnippet();
                    snippet.setTitle(in.readString());
                    snippet.setDescription(in.readString());

                    ThumbnailDetails td = new ThumbnailDetails();
                    Thumbnail thumb = new Thumbnail();

                    thumb.setUrl(in.readString());
                    thumb.setWidth(in.readLong());
                    thumb.setHeight(in.readLong());
                    td.setStandard(thumb);

                    snippet.setThumbnails(td);
                    PlaylistItem item = new PlaylistItem();
                    item.setSnippet(snippet);
                    list.add(item);
                }

            }
            return list;
        }

        @Override
        public List<PlaylistItem>[] newArray(int size) {
          return new List[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        final int size = playlist.size();
        dest.writeInt(size);
        if(size >0) {
            for (int i = 0; i < size; i++) {
                PlaylistItem item = playlist.get(i);
                PlaylistItemSnippet snippet = item.getSnippet();
                dest.writeString(snippet.getTitle());
                dest.writeString(snippet.getDescription());
                Thumbnail thumb = snippet.getThumbnails().getStandard();
                dest.writeString(thumb.getUrl());
                dest.writeLong(thumb.getWidth()); //long is unnecessary, but it makes the code easier to read than downcasting to Int.
                dest.writeLong(thumb.getHeight());
            }
        }

    }

    @Override
    public int describeContents() {
        return 0;
    }


}
