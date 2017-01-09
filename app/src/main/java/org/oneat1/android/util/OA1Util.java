package org.oneat1.android.util;

import butterknife.Unbinder;

/**
 * Created by parthpadgaonkar on 1/8/17.
 */

public class OA1Util {

    public static void safeUnbind(Unbinder unbinder) {
        if(unbinder == null) return;
        else{
            try {
                unbinder.unbind();
            } catch (Exception e) {
                //no op
            }
        }
    }
}
