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
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.nathanosman.chronosnap.R;
import com.nathanosman.chronosnap.ui.MainActivity;


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
     * Sequence name
     */
    public static final String EXTRA_SEQUENCE_NAME = "com.nathanosman.chronosnap.extra.SEQUENCE_NAME";

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

    // Data initialized in the constructor
    private AlarmManager mAlarmManager;
    private SharedPreferences mSharedPreferences;
    private PendingIntent mCaptureIntent;

    // Data initialized when the capture begins
    private long mStartTime;
    private long mInterval;
    private int mIndex;
    private int mLimit;

    // Used for capturing the images
    private ImageCapturer mImageCapturer;

    // Used for tracking stop requests
    private boolean mCaptureInProgress = false;
    private boolean mPendingShutdown = false;

    /**
     * Reimplementation of Service.onCreate()
     */
    @Override
    public void onCreate() {

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mCaptureIntent = PendingIntent.getService(this, 0,
                new Intent(this, CaptureService.class).setAction(ACTION_CAPTURE), 0);
    }

    /**
     * Reimplementation of Service.onStartCommand()
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {

            // Dispatch the command to the appropriate method
            final String action = intent.getAction();
            switch (action) {
                case ACTION_BROADCAST_STATUS:
                    broadcastStatus();
                    break;
                case ACTION_START_CAPTURE:
                    CharSequence sequenceName = intent.getCharSequenceExtra(EXTRA_SEQUENCE_NAME);
                    startCapture(sequenceName);
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
     * Send a broadcast with the current capture status
     *
     * Status currently includes the start time, current index, and remaining
     * image count (0 if no limit).
     */
    private void broadcastStatus() {

        Intent intent = new Intent(BROADCAST_STATUS);
        intent.putExtra(EXTRA_START_TIME, mStartTime);
        intent.putExtra(EXTRA_IMAGES_CAPTURED, mIndex);
        intent.putExtra(EXTRA_IMAGES_REMAINING, mLimit == 0 ? 0 : mLimit - mIndex);

        // Send the broadcast
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Start capturing a sequence of images
     * @param sequenceName name selected for the sequence
     */
    private void startCapture(CharSequence sequenceName) {

        // Prevent a new capture from being started if one is in progress
        if (mStartTime != 0) {
            return;
        }

        // TODO: this method needs heavy refactoring

        log("Starting image capture.");

        createNotification();

        // Set the start time and reset the index
        mStartTime = System.currentTimeMillis();
        mIndex = 0;

        // Load the current settings
        mInterval = Long.parseLong(pref(R.string.pref_interval_key, R.string.pref_interval_default));
        mLimit = Integer.parseInt(pref(R.string.pref_limit_key, R.string.pref_limit_default));

        // Load the camera and focus settings
        int cameraId = Integer.parseInt(pref(R.string.pref_camera_key, R.string.pref_camera_default));
        boolean autofocus = pref(R.string.pref_focus_key, R.string.pref_focus_default).equals("auto");

        // Initialize the capturer
        mImageCapturer = new ImageCapturer(this, cameraId, autofocus, sequenceName);

        // Broadcast the new status (that the capture has started) and set an alarm
        broadcastStatus();
        setAlarm(System.currentTimeMillis() + mInterval);
    }

    /**
     * Stop capturing a sequence of images
     *
     * This may not actually stop the capture immediately since it is
     * currently not possible to interrupt an individual image capture in
     * progress.
     */
    private void stopCapture() {

        log("Stopping image capture.");

        // If a capture is in progress, set a flag to shutdown after the
        // capture completes - otherwise, immediately shut down
        if (mCaptureInProgress) {
            mPendingShutdown = true;
        } else {

            // TODO: this doesn't do anything currently but will be needed later
            mImageCapturer.close();
            shutdown();
        }
    }

    /**
     * Capture a single image
     *
     * The capture process is performed asynchronously and the results are
     * provided through the two callbacks.
     */
    private void capture() {

        log("Capturing image #" + String.valueOf(mIndex) + ".");

        // Grab the current time for calculating the next alarm interval later
        final long captureTime = System.currentTimeMillis();

        // Signal that the capture is in progress
        mCaptureInProgress = true;

        // Begin the capture
        mImageCapturer.startCapture(mIndex, new ImageCapturer.CaptureCallback() {

            @Override
            public void onComplete(String errorMessage) {

                // Log the status of the capture
                if (errorMessage == null) {
                    log("Image #" + String.valueOf(mIndex) + " captured.");
                } else {
                    log("Error: " + errorMessage);
                }

                // Capture is no longer in progress
                mCaptureInProgress = false;

                // Shutdown the capture if one of the following occurred:
                // - the capture was stopped (mPendingShutdown)
                // - an error message was supplied
                // - a limit was supplied and it has been reached
                if (mPendingShutdown || errorMessage != null || mLimit != 0 && (mIndex + 1) == mLimit) {

                    // Close the camera since it won't be needed anymore
                    mImageCapturer.close();

                    // TODO: display an actual error notification instead of a toast

                    // If an error occurred, then display a toast
                    if (errorMessage != null) {
                        Toast.makeText(CaptureService.this, errorMessage, Toast.LENGTH_LONG).show();
                    }

                    mPendingShutdown = false;
                    shutdown();

                } else {

                    // Increment the counter and broadcast the status
                    mIndex++;
                    broadcastStatus();

                    // TODO: check to see if closing the camera is necessary
                    mImageCapturer.close();

                    // Set an alarm for the next capture
                    setAlarm(captureTime + mInterval);
                }
            }
        });
    }

    /**
     * Retrieve the current value of the specified preference
     * @param keyId preference key
     * @param defaultId default preference value
     * @return current value
     */
    private String pref(int keyId, int defaultId) {
        return mSharedPreferences.getString(getString(keyId), getString(defaultId));
    }

    /**
     * Log the specified message
     * @param message a descriptive status message
     */
    private void log(String message) {
        Log.d(CaptureService.class.getSimpleName(), message);
    }

    /**
     * Create the persistent notification that will be displayed during capture
     *
     * The notification includes an action to immediately stop the capture.
     */
    private void createNotification() {

        // Create a pending intent that will display the main UI
        PendingIntent mainIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Create a pending intent that will stop the capture
        PendingIntent stopIntent = PendingIntent.getService(this, 0,
                new Intent(this, CaptureService.class).setAction(ACTION_STOP_CAPTURE), 0);

        // TODO: "stop" is not localized

        // Create the notification, noting that NotificationCompat will ignore
        // any of the methods that aren't available on the current platform
        Notification notification = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_action_stop, "Stop", stopIntent)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setColor(getResources().getColor(R.color.material_primary))
                .setContentIntent(mainIntent)
                .setContentText(getText(R.string.notification_text))
                .setContentTitle(getText(R.string.notification_title))
                .setSmallIcon(R.drawable.ic_stat_notify)
                .build();

        // Build the notification and move the service into the foreground
        startForeground(1, notification);
    }

    /**
     * Set the alarm for the next capture
     * @param triggerAtMillis time at which to capture the next image
     */
    private void setAlarm(long triggerAtMillis) {

        // For KitKat and newer devices, we need to use setExact or we don't
        // end up with the same level of precision as earlier versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, mCaptureIntent);
        } else {
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, mCaptureIntent);
        }
    }

    /**
     * Completely abort the capture
     *
     * All alarms are canceled, the service is pulled out of the foreground,
     * variables are reset, and the status is broadcast one last time. This
     * should never be called while a single image capture is in progress.
     */
    private void shutdown() {

        log("Shutting down service.");

        // Cancel any pending alarms and leave the foreground
        mAlarmManager.cancel(mCaptureIntent);
        stopForeground(true);

        // Reset the start time and broadcast this status
        mStartTime = 0;
        broadcastStatus();

        // Stop the service since there is no need to keep it running
        stopSelf();
    }
}