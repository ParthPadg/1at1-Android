package org.oneat1.android.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import org.oneat1.android.R;
import org.oneat1.android.util.Prefs;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class NotificationPreferenceActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_preference);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.notif_pref_button_positive, R.id.notif_pref_button_negative})
    public void onButtonsClick(View view) {
        Prefs.setNotificationPreference(view.getId() == R.id.notif_pref_button_positive_inner);

    }
}
