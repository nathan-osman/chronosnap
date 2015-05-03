package com.nathanosman.chronosnap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create an intent filter for status updates
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Button buttonStartStop = (Button)findViewById(R.id.buttonStartStop);
                TextView textStartTime = (TextView)findViewById(R.id.textStartTime);
                TextView textImagesCaptured = (TextView)findViewById(R.id.textImagesCaptured);
                TextView textImagesRemaining = (TextView)findViewById(R.id.textImagesRemaining);

                int startTime = intent.getIntExtra(CaptureService.EXTRA_START_TIME, 0);
                int imagesCaptured = intent.getIntExtra(CaptureService.EXTRA_IMAGES_CAPTURED, 0);
                int imagesRemaining = intent.getIntExtra(CaptureService.EXTRA_IMAGES_REMAINING, 0);

                // The button is always enabled
                buttonStartStop.setEnabled(true);

                // A capture is said to be in progress if the start time is nonzero
                if (startTime != 0) {
                    //...
                } else {
                    buttonStartStop.setText(R.string.button_start);
                    textStartTime.setText(R.string.text_na);
                    textImagesCaptured.setText(R.string.text_na);
                    textImagesRemaining.setText(R.string.text_na);
                }

            }
        }, new IntentFilter(CaptureService.BROADCAST_INFO));

        // Get the capture service to broadcast the current status
        Intent intent = new Intent(this, CaptureService.class);
        intent.setAction(CaptureService.ACTION_GET_INFO);
        startService(intent);
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
}
