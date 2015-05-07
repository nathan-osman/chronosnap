package com.nathanosman.chronosnap.preference;

import android.content.Context;
import android.hardware.Camera;
import android.preference.ListPreference;
import android.util.AttributeSet;

import com.nathanosman.chronosnap.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Custom preference type for selecting a hardware camera
 */
public class CameraPreference extends ListPreference {

    // An invalid index for a camera
    public static final int INVALID_INDEX = -1;

    /**
     * Reimplementation of ListPreference()
     */
    @SuppressWarnings("deprecation")
    public CameraPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entryValues = new ArrayList<>();

        // Loop through all of the cameras
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {

            // Retrieve the camera information
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);

            // Record the camera name and index in the arrays
            switch (cameraInfo.facing) {
                case Camera.CameraInfo.CAMERA_FACING_FRONT:
                    entries.add(context.getText(R.string.pref_camera_front));
                    entryValues.add(String.valueOf(i));
                    break;
                case Camera.CameraInfo.CAMERA_FACING_BACK:
                    entries.add(context.getText(R.string.pref_camera_back));
                    entryValues.add(String.valueOf(i));
                    break;
            }
        }

        // Add the values to the ListPreference
        setEntries(entries.toArray(new CharSequence[entries.size()]));
        setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
    }
}
