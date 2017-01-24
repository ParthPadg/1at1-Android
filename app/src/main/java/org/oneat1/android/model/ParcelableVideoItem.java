package org.oneat1.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatistics;

import java.math.BigInteger;

/**
 * Created by parthpadgaonkar on 1/23/17.
 */

public class ParcelableVideoItem implements Parcelable {

    /* Read/Writes the following entries in the following order:
     * 1. Video.getSnipppet().getTitle()
     * 2. Video.getStatistics().getViewCount()
    */
    private Video video;

    public ParcelableVideoItem(Video video) {
        this.video = video;
    }

    public static final Creator<ParcelableVideoItem> CREATOR = new Creator<ParcelableVideoItem>() {
        @Override
        public ParcelableVideoItem createFromParcel(Parcel in) {
            String title = in.readString();

            Video v = new Video();
            VideoSnippet snippet = new VideoSnippet();
            snippet.setTitle(title);

            VideoStatistics stats = new VideoStatistics();
            stats.setViewCount(BigInteger.valueOf(in.readLong()));
            v.setSnippet(snippet);
            v.setStatistics(stats);
            return new ParcelableVideoItem(v);
        }

        @Override
        public ParcelableVideoItem[] newArray(int size) {
            return new ParcelableVideoItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(video.getSnippet().getTitle());
        dest.writeLong(video.getStatistics()
                             .getViewCount()
                             .longValue()); //I'm pretty comfortable downcasting BigInt to long
    }
}
