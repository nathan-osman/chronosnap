package com.nathanosman.chronosnap;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.AsyncTask;

import java.io.IOException;


/**
 * Abstracts the process of capturing an image
 *
 * This class takes care of initializing the camera, applying any settings that are required,
 * attempting to auto-focus if requested, taking the picture, and writing it to disk.
 */
public class ImageCapturer {

    private static final int INVALID_INDEX = -1;

    /**
     * Callback interface used to provide notification of capture state
     */
    public interface CaptureCallback {

        /**
         * Called when a capture completes successfully
         */
        void onSuccess();

        /**
         * Called when an error condition occurs
         * @param description human-readable description of the error condition
         */
        void onError(String description);
    }

    // Data initialized in the constructor
    private int mCameraId;
    private String mSequenceName;
    private CaptureCallback mCaptureCallback;

    // Data required while communicating with the camera
    @SuppressWarnings("deprecation")
    private Camera mCamera;
    private int mCurrentIndex = INVALID_INDEX;

    /**
     * Initialize the capturer
     * @param cameraId ID of the camera to use for capturing
     * @param sequenceName user-supplied name for the sequence
     */
    public ImageCapturer(int cameraId, String sequenceName, CaptureCallback captureCallback) {

        mCameraId = cameraId;
        mSequenceName = sequenceName;
        mCaptureCallback = captureCallback;
    }

    /**
     * Begin capture of the specified image
     * @param index the numerical index of the image to capture
     */
    public void start(int index) {

        mCurrentIndex = index;

        // Camera.open() needs to be done in a separate task since it blocks
        new AsyncTask<Void, Void, String>() {

            @Override
            @SuppressWarnings("deprecation")
            protected String doInBackground(Void... params) {

                // Camera.open() throws whenever the camera can't be opened
                // and Camera.setPreviewTexture() may throw an error as well
                try {
                    mCamera = Camera.open(mCameraId);
                    mCamera.setPreviewTexture(new SurfaceTexture(0));
                    return null;
                } catch (RuntimeException | IOException e) {
                    return e.getLocalizedMessage();
                }
            }

            @Override
            protected void onPostExecute(String message) {

                // If message is non-null, then call CaptureCallback.onError(),
                // otherwise start the camera preview
                if (message == null) {
                    mCaptureCallback.onError(message);
                } else {
                    mCamera.startPreview();
                }
            }
        };
    }
}
