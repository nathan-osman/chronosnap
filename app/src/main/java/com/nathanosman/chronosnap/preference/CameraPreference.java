package com.nathanosman.chronosnap.preference;

import android.content.Context;
import android.hardware.Camera;
import android.preference.ListPreference;
import android.util.AttributeSet;

import com.nathanosman.chronosnap.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Custom preference type for selecting a camera
 *
 * Since the number and type of hardware cameras can differ from one model to
 * the next, the user needs to be able to select which one to use. Subclassing
 * ListPreference allows us to pick the default value at runtime.
 */
public class CameraPreference extends ListPreference {

    /**
     * Populate the list of cameras
     */
    @SuppressWarnings("deprecation")
    public CameraPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Create a list of camera names and indices, noting the index of the
        // rear-facing camera (if discovered) for setting the default
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entryValues = new ArrayList<>();

        CharSequence defaultCamera = null;

        // Loop through all of the cameras
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {

            // Retrieve the camera information
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);

            // Record the camera name and index in the arrays
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                entries.add(context.getText(R.string.pref_camera_front));
            } else {
                entries.add(context.getText(R.string.pref_camera_back));
                defaultCamera = String.valueOf(i);
            }

            entryValues.add(String.valueOf(i));
        }

        // If no suitable default was found, use the first camera
        if (defaultCamera == null && entryValues.size() != 0) {
            defaultCamera = entryValues.get(0);
        }

        // Add the values to the ListPreference
        setEntries(entries.toArray(new CharSequence[entries.size()]));
        setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));

        // Set the default camera if one is present
        if (defaultCamera != null) {
            setDefaultValue(defaultCamera);
        }
    }
}
