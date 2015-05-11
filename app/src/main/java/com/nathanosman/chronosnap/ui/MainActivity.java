package com.nathanosman.chronosnap.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.nathanosman.chronosnap.R;
import com.nathanosman.chronosnap.preference.SettingsActivity;
import com.nathanosman.chronosnap.service.CaptureService;


/**
 * Main interface for the application
 *
 * This activity displays the start / stop button as well as the status of a
 * capture in progress.
 */
public class MainActivity extends ActionBarActivity
        implements NewSequenceDialogFragment.NewSequenceDialogListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create an intent filter for status updates
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Button buttonStartStop = (Button) findViewById(R.id.buttonStartStop);
                TextView textStartTime = (TextView) findViewById(R.id.textStartTime);
                TextView textImagesCaptured = (TextView) findViewById(R.id.textImagesCaptured);
                TextView textImagesRemaining = (TextView) findViewById(R.id.textImagesRemaining);

                long startTime = intent.getLongExtra (CaptureService.EXTRA_START_TIME, 0);
                int imagesCaptured = intent.getIntExtra (CaptureService.EXTRA_IMAGES_CAPTURED, 0);
                int imagesRemaining = intent.getIntExtra (CaptureService.EXTRA_IMAGES_REMAINING, 0);

                // The button is always enabled
                buttonStartStop.setEnabled(true);

                // A capture is said to be in progress if the start time is nonzero
                if (startTime != 0) {

                    // The button stops the capture when clicked
                    buttonStartStop.setText(R.string.button_stop);
                    buttonStartStop.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sendAction(CaptureService.ACTION_STOP_CAPTURE);
                        }
                    });

                    textStartTime.setText(DateUtils.formatDateTime(MainActivity.this, startTime,
                            DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
                    textImagesCaptured.setText(String.valueOf(imagesCaptured));

                    // If imagesRemaining is set to 0, there is no limit
                    if (imagesRemaining == 0) {
                        textImagesRemaining.setText(R.string.text_na);
                    } else {
                        textImagesRemaining.setText(String.valueOf(imagesRemaining));
                    }

                } else {

                    buttonStartStop.setText(R.string.button_start);
                    buttonStartStop.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            NewSequenceDialogFragment dialog = new NewSequenceDialogFragment();
                            dialog.show(getFragmentManager(), NewSequenceDialogFragment.class.getSimpleName());
                        }
                    });

                    textStartTime.setText(R.string.text_na);
                    textImagesCaptured.setText(R.string.text_na);
                    textImagesRemaining.setText(R.string.text_na);
                }

            }
        }, new IntentFilter(CaptureService.BROADCAST_STATUS));

        // Get the capture service to broadcast the current status
        sendAction(CaptureService.ACTION_BROADCAST_STATUS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNewSequenceDialogAccept(CharSequence sequenceName) {

        Intent intent = new Intent(this, CaptureService.class);
        intent.setAction(CaptureService.ACTION_START_CAPTURE);
        intent.putExtra(CaptureService.EXTRA_SEQUENCE_NAME, sequenceName);
        startService(intent);
    }

    /**
     * Utility method to send an action to the capture service
     */
    private void sendAction(String action) {

        Intent intent = new Intent(this, CaptureService.class);
        intent.setAction(action);
        startService(intent);
    }
}
