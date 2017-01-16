package org.oneat1.android.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.tweetui.SearchTimeline;
import com.twitter.sdk.android.tweetui.SearchTimeline.Builder;
import com.twitter.sdk.android.tweetui.TimelineResult;
import com.twitter.sdk.android.tweetui.TweetTimelineListAdapter;

import org.oneat1.android.R;
import org.oneat1.android.util.OA1Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by parthpadgaonkar on 1/14/17.
 */

public class TweetListFragment extends Fragment {
    private final static Logger LOG = LoggerFactory.getLogger(TweetListFragment.class);

    @BindView(android.R.id.list) ListView listview;
    @BindView(R.id.twitter_refresh) SwipeRefreshLayout refreshLayout;

    private Unbinder unbinder;
    private RefreshManager refresh = new RefreshManager();
    private TweetTimelineListAdapter listAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_twitter, container, false);
        unbinder = ButterKnife.bind(this, view);

        SearchTimeline searchTimeline = new Builder()
                                              .query("#1at1action")
                                              .build();
        listAdapter = new TweetTimelineListAdapter.Builder(getActivity())
                            .setTimeline(searchTimeline)
                            .build();

        listview.setAdapter(listAdapter);

        refreshLayout.setOnRefreshListener(refresh);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        OA1Util.safeUnbind(unbinder);
    }

    private class RefreshManager extends Callback<TimelineResult<Tweet>> implements OnRefreshListener {

        @Override
        public void onRefresh() {
            listAdapter.refresh(this);
        }

        @Override
        public void success(Result<TimelineResult<Tweet>> result) {
            refreshLayout.setRefreshing(false);
        }

        @Override
        public void failure(TwitterException exception) {
            LOG.error("Error refreshing twitter stream", exception);
            Toast.makeText(getActivity(), "Error refreshing tweets", Toast.LENGTH_SHORT).show();
        }
    }
}
