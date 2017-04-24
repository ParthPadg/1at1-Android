package org.oneat1.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by parthpadgaonkar on 1/23/17.
 */

public class VideoItemResponse implements Parcelable {
    List<VideoItem> items;

    public List<VideoItem> getItems() {
        return items;
    }

    public static class VideoItem implements Parcelable {
        String id;
        Snippet snippet;
        Statistics statistics;

        public String getId() {
            return id;
        }

        public String getTitle() {
            if (snippet != null) return snippet.title;
            return null;
        }

        public String getDescription() {
            if (snippet != null) return snippet.description;
            return null;
        }

        public long getViewCount() {
            if (statistics != null) return statistics.viewCount;
            else return -1L;
        }

        public static class Statistics implements Parcelable {
            long viewCount;

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeLong(viewCount);
            }

            @Override
            public int describeContents() {
                return 0;
            }

            public static final Creator<Statistics> CREATOR = new Creator<Statistics>() {
                @Override
                public Statistics createFromParcel(Parcel in) {
                    Statistics s = new Statistics();
                    s.viewCount = in.readLong();
                    return s;
                }

                @Override
                public Statistics[] newArray(int size) {
                    return new Statistics[size];
                }
            };
        }

        public static class Snippet implements Parcelable {
            public String title;
            public String description;

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeString(title);
                dest.writeString(description);
            }

            @Override
            public int describeContents() {
                return 0;
            }

            public static final Creator<Snippet> CREATOR = new Creator<Snippet>() {
                @Override
                public Snippet createFromParcel(Parcel in) {
                    Snippet snippet = new Snippet();
                    snippet.title = in.readString();
                    snippet.description = in.readString();
                    return snippet;
                }

                @Override
                public Snippet[] newArray(int size) {
                    return new Snippet[size];
                }
            };
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(id);
            dest.writeParcelable(snippet, flags);
            dest.writeParcelable(statistics, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<VideoItem> CREATOR = new Creator<VideoItem>() {
            @Override
            public VideoItem createFromParcel(Parcel in) {
                VideoItem item = new VideoItem();
                item.id = in.readString();
                item.snippet = in.readParcelable(Snippet.class.getClassLoader());
                item.statistics = in.readParcelable(Statistics.class.getClassLoader());
                return item;
            }

            @Override
            public VideoItem[] newArray(int size) {
                return new VideoItem[size];
            }
        };
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(items);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VideoItemResponse> CREATOR = new Creator<VideoItemResponse>() {
        @Override
        public VideoItemResponse createFromParcel(Parcel in) {
            VideoItemResponse response = new VideoItemResponse();
            response.items = in.createTypedArrayList(VideoItem.CREATOR);
            return response;
        }

        @Override
        public VideoItemResponse[] newArray(int size) {
            return new VideoItemResponse[size];
        }
    };
}
