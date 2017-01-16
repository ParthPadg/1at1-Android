package org.oneat1.android.firebase;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by parthpadgaonkar on 1/3/17.
 */

public class OA1FCMInstanceIDService extends FirebaseInstanceIdService {
    private final static Logger LOG = LoggerFactory.getLogger(OA1FCMInstanceIDService.class);

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        LOG.debug("fcm token refreshed; {}", FirebaseInstanceId.getInstance().getToken());
    }
}
