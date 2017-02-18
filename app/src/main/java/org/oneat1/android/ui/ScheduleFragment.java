package org.oneat1.android.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.InviteEvent;
import com.crashlytics.android.answers.ShareEvent;

import org.oneat1.android.BuildConfig;
import org.oneat1.android.R;
import org.oneat1.android.util.OA1Util;
import org.oneat1.android.util.TypefaceTextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
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

    private Unbinder unbinder;

    public static ScheduleFragment newInstance() {
        return new ScheduleFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.layout_fragment_schedule, container, false);
        unbinder = ButterKnife.bind(this, view);
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
        String extraText = "https://play.google.com/store/apps/details?id=org.oneat1.android";
        intent.putExtra(Intent.EXTRA_TEXT, extraText);
        startActivityForResult(Intent.createChooser(intent, "Share 1@1 Action"), 191817);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 191817 && !BuildConfig.DEBUG && resultCode == Activity.RESULT_OK) {
            Answers.getInstance().logShare(new ShareEvent().putContentName("1@1 App"));
            Answers.getInstance().logInvite(new InviteEvent().putMethod("share"));
        }
    }

    private static Uri getFacebookURI(Context context) {
        return getLaunchUri(context, "com.facebook.katana", "fb://page/1867705966792928", "https://www.facebook.com/1867705966792928");
    }

    private static Uri getInstagramURI(Context context) {
        return getLaunchUri(context, "com.instagram.android", "instagram://user?username=1at1action", "https://www.instagram.com/1at1action/");
    }

    private static Uri getTwitterURI(Context context) {
        return getLaunchUri(context, "com.twitter.android", "twitter://user?screen_name=1at1Action", "https://twitter.com/1at1Action");
    }

    private static Uri getLaunchUri(Context ctx, String pkg, String deeplink, String weblink) {
        PackageManager pm = ctx.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(new Intent(Intent.ACTION_VIEW, Uri.parse(deeplink)), PackageManager.MATCH_DEFAULT_ONLY);
        boolean exists;
        if (!list.isEmpty()) {
            try {
                PackageInfo info = pm.getPackageInfo(pkg, 0);
                exists = info.applicationInfo.enabled;
            } catch (Exception e) {
                exists = false;
            }
        } else {
            exists = false;
        }

        return Uri.parse(exists ? deeplink : weblink);
    }

}
