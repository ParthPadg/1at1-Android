package org.oneat1.android.util;

import android.os.Handler;
import android.os.Looper;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import butterknife.Unbinder;

/**
 * Created by parthpadgaonkar on 1/8/17.
 */

public class OA1Util {

    public static void safeUnbind(Unbinder unbinder) {
        if (unbinder == null) return;
        else {
            try {
                unbinder.unbind();
            } catch (Exception e) {
                //no op
            }
        }
    }

    public static class ThreadUtil {
        private static ThreadUtil sInstance;
        private static final Map<Runnable, Future<?>> futureMap = Collections.synchronizedMap(new WeakHashMap<Runnable, Future<?>>());
        private ScheduledExecutorService executor = null;
        private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        public static synchronized ThreadUtil getInstance() {
            if (sInstance == null) {
                sInstance = new ThreadUtil();
            }
            return sInstance;
        }

        private ThreadUtil() {
            executor = Executors.newScheduledThreadPool(2);
        }

        public Future runNowInBackground(Runnable runnable) {
            Future<?> future = executor.submit(runnable);
            futureMap.put(runnable, future);
            return future;
        }

        public void runOnUIThread(Runnable runnable) {
            if (Looper.myLooper() == Looper.getMainLooper()) {//already on the UI thread
                runnable.run();
            } else {
                mainThreadHandler.post(runnable);
            }
        }

        public void cancel(Runnable runnable) {
            if (runnable == null) {
                return;
            }

            Future<?> future = futureMap.remove(runnable);
            if (future != null) {
                future.cancel(true);
            }
        }
    }
}
