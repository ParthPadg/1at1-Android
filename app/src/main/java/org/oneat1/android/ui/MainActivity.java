package org.oneat1.android.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.Tab;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;

import org.oneat1.android.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by parthpadgaonkar on 1/4/17.
 */
public class MainActivity extends Activity {

    @BindView(R.id.main_tabs) TabLayout tabs;
    @BindView(R.id.main_viewpager) ViewPager viewpager;

    private MainPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        viewpager.setAdapter(pagerAdapter = new MainPagerAdapter(getFragmentManager()));
        viewpager.setOffscreenPageLimit(1);
        setupWithViewPager(tabs, viewpager);
    }

    //This method borrows heavily from TabLayout#setupWithViewPager.
    private void setupWithViewPager(final TabLayout tabs, ViewPager pager) {
        pager.clearOnPageChangeListeners();
        pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
        tabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(pager));
        populateTabs(tabs);
        pagerAdapter
              .registerDataSetObserver(new DataSetObserver() {  //in either case, we just blow away and re-populate the tabs.
                  @Override
                  public void onChanged() {
                      populateTabs(tabs);
                  }

                  @Override
                  public void onInvalidated() {
                      populateTabs(tabs);
                  }
              });
    }

    private void populateTabs(TabLayout tabs) {
        tabs.removeAllTabs();
        final int size = pagerAdapter.getCount();
        for (int i = 0; i < size; i++) {
            Tab tab = tabs.newTab()
                            .setText(pagerAdapter.getPageTitle(i))
                            .setCustomView(R.layout.tablayout_custom_tab); //using custom tab view so we can set typeface
            tabs.addTab(tab, i == 0);
        }
    }

    static class MainPagerAdapter extends FragmentPagerAdapter {
        private static final String[] TAB_TITLES = {"Home", "Watch", "Follow"};

        MainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return ScheduleFragment.newInstance();
                case 1:
                    return WatchVideoFragment.createInstance();
                case 2:
                    return new TweetListFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return TAB_TITLES.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TAB_TITLES[position];
        }
    }
}
