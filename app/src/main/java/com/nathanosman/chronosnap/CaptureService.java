package com.nathanosman.chronosnap;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;


public class CaptureService extends IntentService {

    public static final String ACTION_START_CAPTURE = "com.nathanosman.chronosnap.action.START_CAPTURE";
    public static final String ACTION_STOP_CAPTURE = "com.nathanosman.chronoshap.action.STOP_CAPTURE";
    public static final String ACTION_GET_INFO = "com.nathanosman.chronosnap.action.GET_INFO";

    public static final String BROADCAST_INFO = "com.nathanosman.chronosnap.broadcast.INFO";

    public static final String EXTRA_START_TIME = "com.nathanosman.chronosnap.extra.START_TIME";
    public static final String EXTRA_IMAGES_CAPTURED = "com.nathanosman.chronosnap.extra.IMAGES_CAPTURED";
    public static final String EXTRA_IMAGES_REMAINING = "com.nathanosman.chronosnap.extra.IMAGES_REMAINING";

    // All of these values are initialized by ACTION_START_CAPTURE,
    // so there is no need to initialize them here
    private int mIndex;

    public CaptureService() {
        super("CaptureService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START_CAPTURE.equals(action)) {
                handleStartCapture();
            } else if (ACTION_STOP_CAPTURE.equals(action)) {
                handleStopCapture();
            } else if (ACTION_GET_INFO.equals(action)) {
                broadcastInfo();
            }
        }
    }

    private void handleStartCapture() {
        mIndex = 0;
    }

    private void handleStopCapture() {
        //...
    }

    private void broadcastInfo() {
        Intent intent = new Intent(BROADCAST_INFO);
        intent.putExtra(EXTRA_START_TIME, 0);
        intent.putExtra(EXTRA_IMAGES_CAPTURED, 0);
        intent.putExtra(EXTRA_IMAGES_REMAINING, 0);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
