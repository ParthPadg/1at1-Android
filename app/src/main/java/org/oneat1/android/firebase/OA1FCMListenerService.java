package org.oneat1.android.firebase;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v7.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.oneat1.android.R;
import org.oneat1.android.ui.MainActivity;
import org.oneat1.android.util.Prefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by parthpadgaonkar on 1/3/17.
 */

public class OA1FCMListenerService extends FirebaseMessagingService {
    private static final int NOTIF_ID = 16151414;
    private final static Logger LOG = LoggerFactory.getLogger(OA1FCMListenerService.class);

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        LOG.debug("received FCM message: {}", message);
        LOG.debug("received message data: {}", message.getData());
        RemoteMessage.Notification data = message.getNotification();
        LOG.debug("received message \n title: {}\nbody: {}", data.getTitle(), data.getBody());

        Boolean pref = Prefs.getNotificationPreference();
        if (pref != null && !pref) {
            LOG.warn("throwing away message because user has declined notifications!");
            return;
        }

        Builder builder = new NotificationCompat.Builder(this)
                                .setColor(getColor(R.color.cobalt))
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentIntent(PendingIntent.getActivity(this, 10101,
                                      new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                      PendingIntent.FLAG_UPDATE_CURRENT));

        if (data.getTitle() != null) {
            builder.setContentTitle(data.getTitle());
        }
        if (data.getBody() != null) {
            builder.setContentText(data.getBody());
        }

        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(NOTIF_ID, builder.build());
    }
}
