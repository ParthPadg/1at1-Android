package org.oneat1.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.oneat1.android.OA1App;
import org.oneat1.android.R;
import org.oneat1.android.util.Prefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class SplashActivity extends Activity {
    private Logger LOG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        LOG = LoggerFactory.getLogger(SplashActivity.class);
        LOG.debug("displaying splash screen");
        Completable.create(new CompletableOnSubscribe() {
                  @Override
                  public void subscribe(@NonNull CompletableEmitter e) throws Exception {
                      LOG.debug("init in background");
                      OA1App.getApp().init();
                      LOG.debug("Finishing init!");
                      e.onComplete();
                  }
              })
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(new Action() {
                  @Override
                  public void run() throws Exception {
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
              }, new Consumer<Throwable>() {
                  @Override
                  public void accept(@NonNull Throwable throwable) throws Exception {
                      LOG.error("Error loading!", throwable);
                  }
              });
    }
}
