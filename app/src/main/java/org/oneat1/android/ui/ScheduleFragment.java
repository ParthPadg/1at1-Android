package org.oneat1.android.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.oneat1.android.R;
import org.oneat1.android.util.OA1Util;
import org.oneat1.android.util.TypefaceTextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by parthpadgaonkar on 1/4/17.
 */

public class ScheduleFragment extends Fragment {

    @BindView(R.id.schedule_datetime) TypefaceTextView scheduleDatetime;
    private Unbinder unbinder;

    public static ScheduleFragment newInstance() {
        return new ScheduleFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.layout_fragment_schedule, container, false);
        unbinder = ButterKnife.bind(this, view);
        Calendar instance = GregorianCalendar.getInstance();
        instance.set(2017, 0, 21, 13, 0);
        instance.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy / h:mm aa zzz", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getDefault());
        SpannableStringBuilder ssb = new SpannableStringBuilder(getString(R.string.schedule_text, formatter.format(new Date(instance
                                                                                                                                  .getTimeInMillis()))));
        ssb.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.darkBlue)), 13, 15, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        scheduleDatetime.setText(ssb);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        OA1Util.safeUnbind(unbinder);
    }

    @OnClick({R.id.schedule_facebook, R.id.schedule_instagram, R.id.schedule_twitter})
    void onClick(View view) {
        Uri uri = null;
        switch (view.getId()) {
            case R.id.schedule_facebook_inner:
                uri = getFacebookURI(getActivity());
                break;
            case R.id.schedule_instagram_inner:
                uri = getInstagramURI(getActivity());
                break;
            case R.id.schedule_twitter_inner:
                uri = getTwitterURI(getActivity());
                break;
        }
        if(uri != null) {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }

    @OnClick(R.id.schedule_share)
    void shareAppClick() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "1@1 Action");
        String extraText = "Check out the 1@1 action for national equality! 1at1.org \n\n"
                            + "https://play.google.com/store/apps/details?id=org.oneat1.android";
        intent.putExtra(Intent.EXTRA_TEXT, extraText);
        startActivity(Intent.createChooser(intent, "Share 1@1 Action"));
    }

    private static Uri getFacebookURI(Context context) {
        Uri uri;
        try {
            context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            uri = Uri.parse("fb://page/1867705966792928");
        } catch (Exception e) {
            uri = Uri.parse("https://www.facebook.com/oneatone");
        }
        return uri;
    }

    private static Uri getInstagramURI(Context context) {
        Uri uri;
        try {
            context.getPackageManager().getPackageInfo("com.instagram.android", 0);
            uri = Uri.parse("instagram://user?username=1at1action");
        } catch (Exception e) {
            uri = Uri.parse("https://www.instagram.com/1at1action/");
        }
        return uri;
    }

    private static Uri getTwitterURI(Context context) {
        Uri uri;
        try {
            context.getPackageManager().getPackageInfo("com.twitter.android", 0);
            uri = Uri.parse("twitter://user?screen_name=1at1Action");
        } catch (Exception e) {
            uri = Uri.parse("https://twitter.com/1at1Action");
        }
        return uri;
    }

}
