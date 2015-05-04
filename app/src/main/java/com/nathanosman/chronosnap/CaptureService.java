package com.nathanosman.chronosnap;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;


public class CaptureService extends Service {

    public static final String ACTION_START_CAPTURE = "com.nathanosman.chronosnap.action.START_CAPTURE";
    public static final String ACTION_STOP_CAPTURE = "com.nathanosman.chronoshap.action.STOP_CAPTURE";
    public static final String ACTION_GET_INFO = "com.nathanosman.chronosnap.action.GET_INFO";

    // This action is only sent by the alarm
    private static final String ACTION_CAPTURE = "com.nathanosman.chronosnap.action.CAPTURE";

    public static final String BROADCAST_INFO = "com.nathanosman.chronosnap.broadcast.INFO";

    public static final String EXTRA_START_TIME = "com.nathanosman.chronosnap.extra.START_TIME";
    public static final String EXTRA_IMAGES_CAPTURED = "com.nathanosman.chronosnap.extra.IMAGES_CAPTURED";
    public static final String EXTRA_IMAGES_REMAINING = "com.nathanosman.chronosnap.extra.IMAGES_REMAINING";

    public static final int NOTIFICATION_ID = 1;

    // All of these values are initialized by ACTION_START_CAPTURE
    private long mStartTime;
    private long mInterval;
    private int mIndex;
    private int mLimit;

    private AlarmManager mAlarmManager;
    private PendingIntent mCaptureIntent;

    @Override
    public void onCreate() {

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mCaptureIntent = PendingIntent.getService(this, 0,
                new Intent(this, CaptureService.class).setAction(ACTION_CAPTURE), 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Process the command
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START_CAPTURE.equals(action)) {
                handleStartCapture();
            } else if (ACTION_STOP_CAPTURE.equals(action)) {
                handleStopCapture();
            } else if (ACTION_GET_INFO.equals(action)) {
                broadcastInfo();
            } else if (ACTION_CAPTURE.equals(action)) {
                handleCapture();
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressWarnings("deprecation")
    private void handleStartCapture() {

        // Make this a foreground service, which helps keep it from being killed
        Intent intent = new Intent(this, MainActivity.class);
        Notification notification = new Notification.Builder(this)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
                .getNotification();
        startForeground(NOTIFICATION_ID, notification);

        // Prevent the capture from starting if it's already in progress
        if (mStartTime != 0) {
            return;
        }

        mStartTime = System.currentTimeMillis();
        mIndex = 0;

        // TODO: load the interval and limit from preferences
        mInterval = 5000;
        mLimit = 0;

        broadcastInfo();

        // Set an alarm to capture the first image
        setAlarm();
    }

    private void handleStopCapture() {

        // This is no longer a foreground service
        mAlarmManager.cancel(mCaptureIntent);
        stopForeground(true);

        mStartTime = 0;

        broadcastInfo();

        // Stop the service since we don't need it running anymore
        stopSelf();
    }

    /**
     * Send a local intent providing current status information
     */
    private void broadcastInfo() {

        Intent intent = new Intent(BROADCAST_INFO);
        intent.putExtra(EXTRA_START_TIME, mStartTime);
        intent.putExtra(EXTRA_IMAGES_CAPTURED, mIndex);
        intent.putExtra(EXTRA_IMAGES_REMAINING, mLimit == 0 ? 0 : mLimit - mIndex);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Set the alarm for the next capture
     * KitKat requires a separate method to be called in order to ensure timely delivery
     */
    private void setAlarm() {

        long triggerAtMillis = System.currentTimeMillis() + mInterval;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, mCaptureIntent);
        } else {
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, mCaptureIntent);
        }
    }

    private void handleCapture() {

        mIndex++;

        broadcastInfo();

        // Set an alarm to capture the next image
        setAlarm();
    }
}
