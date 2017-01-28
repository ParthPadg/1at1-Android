package org.oneat1.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by parthpadgaonkar on 1/23/17.
 */
public class PlaylistItemResponse implements Parcelable {
    public String pageToken;
    //    public PageInfo pageInfo;
    public List<PlaylistItem> items;

    //TODO unclear if we need this
    /*public static class PageInfo implements Parcelable {
        public int resultsPerPage;
        public int totalResults;


        public static final Creator<PageInfo> CREATOR = new Creator<PageInfo>() {
            @Override
            public PageInfo createFromParcel(Parcel in) {
                PageInfo pageInfo = new PageInfo();
                pageInfo.resultsPerPage = in.readInt();
                pageInfo.totalResults = in.readInt();
                return pageInfo;
            }

            @Override
            public PageInfo[] newArray(int size) {
                return new PageInfo[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(resultsPerPage);
            dest.writeInt(totalResults);
        }
    }*/

    public static class PlaylistItem implements Parcelable {
        public Snippet snippet;

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(snippet, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<PlaylistItem> CREATOR = new Creator<PlaylistItem>() {
            @Override
            public PlaylistItem createFromParcel(Parcel in) {
                PlaylistItem item = new PlaylistItem();
                item.snippet = in.readParcelable(Snippet.class.getClassLoader());
                return item;
            }

            @Override
            public PlaylistItem[] newArray(int size) {
                return new PlaylistItem[size];
            }
        };

        public static class Snippet implements Parcelable {
            public String title;
            public String description;
            public ThumbnailDetails thumbnails;

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeString(title);
                dest.writeString(description);
                dest.writeParcelable(thumbnails, flags);
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
                    snippet.thumbnails = in.readParcelable(ThumbnailDetails.class.getClassLoader());
                    return snippet;
                }

                @Override
                public Snippet[] newArray(int size) {
                    return new Snippet[size];
                }
            };
        }

        public static class ThumbnailDetails implements Parcelable {
            public Thumbnail standard;

            public static class Thumbnail implements Parcelable {
                public int width;
                public int height;
                public String url;

                public static final Creator<Thumbnail> CREATOR = new Creator<Thumbnail>() {
                    @Override
                    public Thumbnail createFromParcel(Parcel in) {
                        Thumbnail t = new Thumbnail();
                        t.width = in.readInt();
                        t.height = in.readInt();
                        t.url = in.readString();
                        return t;
                    }

                    @Override
                    public Thumbnail[] newArray(int size) {
                        return new Thumbnail[size];
                    }
                };

                @Override
                public int describeContents() {
                    return 0;
                }

                @Override
                public void writeToParcel(Parcel dest, int flags) {
                    dest.writeInt(width);
                    dest.writeInt(height);
                    dest.writeString(url);
                }
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeParcelable(standard, flags);
            }

            @Override
            public int describeContents() {
                return 0;
            }

            public static final Creator<ThumbnailDetails> CREATOR = new Creator<ThumbnailDetails>() {
                @Override
                public ThumbnailDetails createFromParcel(Parcel in) {
                    ThumbnailDetails d = new ThumbnailDetails();
                    d.standard = in.readParcelable(Thumbnail.class.getClassLoader());

                    return d;
                }

                @Override
                public ThumbnailDetails[] newArray(int size) {
                    return new ThumbnailDetails[size];
                }
            };
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(pageToken);
        dest.writeTypedList(items);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PlaylistItemResponse> CREATOR = new Creator<PlaylistItemResponse>() {
        @Override
        public PlaylistItemResponse createFromParcel(Parcel in) {
            PlaylistItemResponse response = new PlaylistItemResponse();
            response.pageToken = in.readString();
            response.items = in.createTypedArrayList(PlaylistItem.CREATOR);
            return response;
        }

        @Override
        public PlaylistItemResponse[] newArray(int size) {
            return new PlaylistItemResponse[size];
        }
    };
}