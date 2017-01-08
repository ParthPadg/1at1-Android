package org.oneat1.android.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by parthpadgaonkar on 1/4/17.
 */
public class Prefs {
    private static final String PREF_FILE_KEY = "com.oneat1.android";
    private static SharedPreferences prefs;

    private static final String NOTIFICATION_PREFERENCE = "pref.notify";

    public static void init(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_FILE_KEY, Context.MODE_PRIVATE);
    }

    /**
     * Returns the user's notification preference, if it exists.
     *
     * @return a boolean if the user has set a notification preference,
     * or <code>null</code> if no preference has been set.
     */
    public static Boolean getNotificationPreference() {
        if (prefs.contains(NOTIFICATION_PREFERENCE)) {
            return prefs.getBoolean(NOTIFICATION_PREFERENCE, false);
        }
        return null;
    }

    public static void setNotificationPreference(boolean allowNotifications) {
        prefs.edit()
              .putBoolean(NOTIFICATION_PREFERENCE, allowNotifications)
              .apply();
    }
}
