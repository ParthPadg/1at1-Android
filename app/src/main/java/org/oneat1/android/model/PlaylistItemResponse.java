package org.oneat1.android.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by parthpadgaonkar on 1/23/17.
 */
public class PlaylistItemResponse implements Parcelable {
    public String nextPageToken;
    public List<PlaylistItem> items;

    public static class PlaylistItem implements Parcelable {
        Snippet snippet;
        String id;

        @Nullable
        public String getTitle() {
            if (snippet != null) return snippet.title;
            return null;
        }

        @Nullable
        public String getDescription() {
            if (snippet != null) return snippet.description;
            return null;
        }

        @Nullable
        public String getThumnailURL() {
            if (snippet != null
                      && snippet.thumbnails != null
                      && snippet.thumbnails.standard != null) {
                return snippet.thumbnails.standard.url;
            }
            return null;
        }

        @Nullable
        public String getVideoID() {
            if (snippet != null && snippet.resourceId != null) {
                return snippet.resourceId.videoId;
            }
            return null;
        }

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
            String title;
            String description;
            ThumbnailDetails thumbnails;
            ResourceID resourceId;

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

            public static class ThumbnailDetails implements Parcelable {
                Thumbnail standard;

                public static class Thumbnail implements Parcelable {
                    int width;
                    int height;
                    String url;

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

            public static class ResourceID implements Parcelable {
                String videoId;

                public static final Creator<ResourceID> CREATOR = new Creator<ResourceID>() {
                    @Override
                    public ResourceID createFromParcel(Parcel in) {
                        ResourceID resourceID = new ResourceID();
                        resourceID.videoId = in.readString();
                        return resourceID;
                    }

                    @Override
                    public ResourceID[] newArray(int size) {
                        return new ResourceID[size];
                    }
                };

                @Override
                public int describeContents() {
                    return 0;
                }

                @Override
                public void writeToParcel(Parcel dest, int flags) {
                    dest.writeString(videoId);
                }
            }
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nextPageToken);
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
            response.nextPageToken = in.readString();
            response.items = in.createTypedArrayList(PlaylistItem.CREATOR);
            return response;
        }

        @Override
        public PlaylistItemResponse[] newArray(int size) {
            return new PlaylistItemResponse[size];
        }
    };
}