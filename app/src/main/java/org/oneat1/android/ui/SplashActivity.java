package org.oneat1.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.oneat1.android.OA1App;
import org.oneat1.android.R;
import org.oneat1.android.util.OA1Util.ThreadUtil;
import org.oneat1.android.util.Prefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplashActivity extends Activity {
    private Logger LOG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        LOG = LoggerFactory.getLogger(SplashActivity.class);
        LOG.debug("displaying splash screen");
        ThreadUtil.getInstance().runNowInBackground(new Runnable() {
            @Override
            public void run() {
                LOG.debug("init in background");
                OA1App.getApp().init();
                LOG.debug("Finishing init!");

                bounceToRealScreen();
            }
        });
    }

    void bounceToRealScreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Boolean pref = Prefs.getNotificationPreference();
                LOG.debug("checking notification preference; it's {}", pref);
                if (pref == null) { //we haven't asked the user about notifications!
                    startActivity(new Intent(SplashActivity.this, NotificationPreferenceActivity.class));
                } else {
                    //go to main screen
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                }
                finish();
            }
        });
    }

}
