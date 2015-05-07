package com.nathanosman.chronosnap.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.nathanosman.chronosnap.MainActivity;
import com.nathanosman.chronosnap.R;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Captures images at the predefined interval
 *
 * Capture parameters are initialized at the beginning of the capture.
 */
public class CaptureService extends Service {

    /**
     * Broadcast the current status of the capture
     */
    public static final String ACTION_BROADCAST_STATUS = "com.nathanosman.chronosnap.action.BROADCAST_STATUS";

    /**
     * Start capturing a sequence
     */
    public static final String ACTION_START_CAPTURE = "com.nathanosman.chronosnap.action.START_CAPTURE";

    /**
     * Explicitly stop capturing a sequence
     */
    public static final String ACTION_STOP_CAPTURE = "com.nathanosman.chronoshap.action.STOP_CAPTURE";

    /**
     * Capture the next image in the sequence
     * <p/>
     * This action is triggered by an alarm set to the appropriate interval
     * and therefore can only be sent from within the service.
     */
    private static final String ACTION_CAPTURE = "com.nathanosman.chronosnap.action.CAPTURE";

    /**
     * Broadcast containing capture status
     */
    public static final String BROADCAST_STATUS = "com.nathanosman.chronosnap.broadcast.STATUS";

    /**
     * Start time of the capture
     */
    public static final String EXTRA_START_TIME = "com.nathanosman.chronosnap.extra.START_TIME";

    /**
     * Number of images captured so far
     */
    public static final String EXTRA_IMAGES_CAPTURED = "com.nathanosman.chronosnap.extra.IMAGES_CAPTURED";

    /**
     * Number of images remaining to be captured (0 if not limit)
     */
    public static final String EXTRA_IMAGES_REMAINING = "com.nathanosman.chronosnap.extra.IMAGES_REMAINING";

    // Alarm manager and intent for triggering a capture
    private AlarmManager mAlarmManager;
    private PendingIntent mCaptureIntent;

    // Data initialized when the capture begins
    private long mStartTime;
    private long mInterval;
    private int mIndex;
    private int mLimit;

    // Used for capturing the images
    private ImageCapturer mImageCapturer;

    /**
     * Reimplementation of Service.onCreate()
     */
    @Override
    public void onCreate() {

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mCaptureIntent = PendingIntent.getService(this, 0,
                new Intent(this, CaptureService.class).setAction(ACTION_CAPTURE), 0);
    }

    /**
     * Reimplementation of Service.onStartCommand()
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // TODO: figure out why the intent is sometimes NULL

        if (intent != null) {

            // Dispatch the command to the appropriate method
            final String action = intent.getAction();
            switch (action) {
                case ACTION_BROADCAST_STATUS:
                    broadcastStatus();
                    break;
                case ACTION_START_CAPTURE:
                    startCapture();
                    break;
                case ACTION_STOP_CAPTURE:
                    stopCapture();
                    break;
                case ACTION_CAPTURE:
                    capture();
                    break;
            }
        }

        return START_STICKY;
    }

    /**
     * Not implemented
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Send a broadcast with current status
     */
    private void broadcastStatus() {

        Intent intent = new Intent(BROADCAST_STATUS);
        intent.putExtra(EXTRA_START_TIME, mStartTime);
        intent.putExtra(EXTRA_IMAGES_CAPTURED, mIndex);
        intent.putExtra(EXTRA_IMAGES_REMAINING, mLimit == 0 ? 0 : mLimit - mIndex);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Start capturing a sequence of images
     */
    @SuppressWarnings("deprecation")
    private void startCapture() {

        log("Starting image capture.");

        // Indicate that this is a foreground service by creating a persistent
        // notification displayed while the capture is in progress
        Intent intent = new Intent(this, MainActivity.class);
        Notification notification = new Notification.Builder(this)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
                .getNotification();
        startForeground(1, notification);

        // Set the start time and reset the index
        mStartTime = System.currentTimeMillis();
        mIndex = 0;

        // Load the current settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mInterval = Long.parseLong(sharedPreferences.getString("interval", ""));
        mLimit = Integer.parseInt(sharedPreferences.getString("limit", ""));

        // Generate a name for the sequence based on the current date and time
        int cameraId = Integer.parseInt(sharedPreferences.getString("camera", ""));
        boolean autofocus = sharedPreferences.getString("focus", "").equals("auto");

        // TODO: user should be able to select the sequence name

        String sequenceName = new SimpleDateFormat("yyyymmdd_hhmmss").format(new Date());

        // Initialize the capturer
        mImageCapturer = new ImageCapturer(cameraId, autofocus, sequenceName);

        // Broadcast the new status (that the capture has started) and set an alarm
        broadcastStatus();
        setAlarm();
    }

    /**
     * Stop capturing a sequence of images
     */
    private void stopCapture() {

        log("Stopping image capture.");

        // TODO: this currently cannot cancel an image capture in progress

        // Cancel any pending capture intents and leave the foreground
        mAlarmManager.cancel(mCaptureIntent);
        stopForeground(true);

        // Reset the start time (to indicate no transfer) and broadcast this status
        mStartTime = 0;
        broadcastStatus();

        // Stop the service
        stopSelf();
    }

    /**
     * Capture a single image
     */
    private void capture() {

        log("Capturing image #" + String.valueOf(mIndex) + ".");

        mImageCapturer.startCapture(mIndex, new ImageCapturer.CaptureCallback() {

            @Override
            public void onSuccess() {

                log("Image #" + String.valueOf(mIndex) + " captured.");

                // Increment the counter and broadcast the status
                mIndex++;
                broadcastStatus();

                // Check to see if more images should be captured (and set the alarm)
                // or if the limit was reached (and the capture may be stopped
                if (mLimit == 0 || mIndex < mLimit) {

                    // TODO: this should be a configurable setting
                    mImageCapturer.close();

                    setAlarm();

                } else {
                    stopCapture();
                }
            }

            @Override
            public void onError(String description) {

                log("Error: " + description);

                // Inform the user that an error has occurred during capture
                Toast.makeText(CaptureService.this, R.string.toast_error_storage_img,
                        Toast.LENGTH_LONG).show();

                // Stop the capture
                stopCapture();
            }
        });
    }

    /**
     * Set the alarm for the next capture
     */
    private void setAlarm() {

        long triggerAtMillis = System.currentTimeMillis() + mInterval;

        // For KitKat and newer devices, we need to use setExact or we don't
        // end up with the same level of precision as earlier versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, mCaptureIntent);
        } else {
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, mCaptureIntent);
        }
    }

    /**
     * Log the specified message
     */
    private void log(String message) {
        Log.d(CaptureService.class.getSimpleName(), message);
    }
}