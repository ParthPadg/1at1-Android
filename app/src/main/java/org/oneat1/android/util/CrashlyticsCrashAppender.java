package org.oneat1.android.util;

import com.crashlytics.android.Crashlytics;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Created by parthpadgaonkar on 1/16/17.
 */

public class CrashlyticsCrashAppender extends AppenderBase<ILoggingEvent> {
    private PatternLayoutEncoder encoder;

    public CrashlyticsCrashAppender(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    protected void append(ILoggingEvent event) {
        String s = encoder.getLayout().doLayout(event);

        Crashlytics.log(s);
    }

    @Override
    public void start() {
        super.start();
        addError("No layout set for this appender!");
    }
}
