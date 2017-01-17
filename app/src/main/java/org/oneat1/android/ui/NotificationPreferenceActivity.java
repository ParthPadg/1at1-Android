package org.oneat1.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.oneat1.android.BuildConfig;
import org.oneat1.android.R;
import org.oneat1.android.util.Prefs;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class NotificationPreferenceActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_preference);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.notif_pref_button_positive, R.id.notif_pref_button_negative})
    void onButtonsClick(View view) {
        boolean accepted = view.getId() == R.id.notif_pref_button_positive_inner;
        Prefs.setNotificationPreference(accepted);
        CustomEvent event = new CustomEvent("Notification Preference")
                                  .putCustomAttribute("opted in", accepted ? "yes" : "no");
        if(!BuildConfig.DEBUG){
            Answers.getInstance().logCustom(event);
        }
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @OnLongClick(R.id.notif_pref_button_negative)
    boolean onDebugLongClick() {
        if (BuildConfig.DEBUG) {
            startActivity(new Intent(this, MainActivity.class));
            return true;
        }
        return false;
    }
}
