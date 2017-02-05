package org.oneat1.android.model.youtube;

import java.util.List;

/**
 * Created by parthpadgaonkar on 1/8/17.
 */

public class YTResponseBody {
    public String kind;
    public List<YTItem> items;

    public static class YTItem {
        public String kind;
        public String id;
        public YTSnippet snippet;
        public YTStatistics statistics;

        public static class YTStatistics {
            public String viewCount;
        }

        public static class YTSnippet {
            public String title;
            public String description;
        }


    }

}
