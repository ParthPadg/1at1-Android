package org.oneat1.android.model.youtube;

import android.text.TextUtils;

/**
 * Created by parthpadgaonkar on 1/8/17.
 */

public class YTItem {
    public String kind;
    public String id;
    public YTSnippet snippet;
    public YTStatistics statistics;
    public YTLiveDetails liveStreamingDetails;

    public static class YTStatistics {
        public String viewCount;
    }

    public static class YTSnippet {
        public String title;
        public String liveBroadcastContent;

        public boolean isLivestream() {
            return !TextUtils.isEmpty(liveBroadcastContent)
                         && !liveBroadcastContent.equals("none");
        }
    }

    public class YTLiveDetails {
        public String concurrentViewers;
    }
}
