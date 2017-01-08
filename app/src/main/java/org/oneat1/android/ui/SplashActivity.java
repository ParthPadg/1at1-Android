package org.oneat1.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.oneat1.android.OA1App;
import org.oneat1.android.R;
import org.oneat1.android.util.Prefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplashActivity extends Activity {
    private final static Logger LOG = LoggerFactory.getLogger(SplashActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        LOG.debug("displaying splash screen");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        OA1App.getInstance().init();
        if(Prefs.getNotificationPreference() == null){ //we haven't asked the user about notifications!
            startActivity(new Intent(this, NotificationPreferenceActivity.class));
        }else{
            //go to main screen
        }

    }
}
