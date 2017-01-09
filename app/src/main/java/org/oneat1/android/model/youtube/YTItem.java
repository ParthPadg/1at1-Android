package org.oneat1.android.model.youtube;

/**
 * Created by parthpadgaonkar on 1/8/17.
 */

public class YTItem {
    public String kind;
    public String id;
    public YTSnippet snippet;
    public YTStatistics statistics;

    public static class YTStatistics {
        public String viewCount;
    }

    public static class YTSnippet {
        public String title;
    }
}
