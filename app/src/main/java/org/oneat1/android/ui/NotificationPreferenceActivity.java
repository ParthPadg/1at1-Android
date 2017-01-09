package org.oneat1.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

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
        Prefs.setNotificationPreference(view.getId() == R.id.notif_pref_button_positive_inner);
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