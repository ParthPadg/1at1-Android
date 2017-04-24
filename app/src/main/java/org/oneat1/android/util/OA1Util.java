package org.oneat1.android.util;

import android.app.Activity;
import android.app.Fragment;

import butterknife.Unbinder;

/**
 * Created by parthpadgaonkar on 1/8/17.
 */

public class OA1Util {

    public static void safeUnbind(Unbinder unbinder) {
        if (unbinder != null) {
            try {
                unbinder.unbind();
            } catch (Exception e) {
                //no op
            }
        }
    }

    public static boolean isActivityFinishing(Activity activity){
        return activity == null || activity.isFinishing() || activity.isDestroyed();
    }

    public static boolean isFragmentDetached(Fragment fragment){
        return fragment != null && (isActivityFinishing(fragment.getActivity()) || fragment.isRemoving() || fragment.isDetached());
    }
}
