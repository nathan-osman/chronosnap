package com.nathanosman.chronosnap;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * Abstracts the process of capturing an image
 *
 * This class takes care of initializing the camera, applying any settings that are required,
 * attempting to auto-focus if requested, taking the picture, and writing it to disk.
 */
public class ImageCapturer {

    /**
     * Callback interface used to provide notification of capture state
     *
     * Both of the callbacks may leave the camera open (to save time when
     * starting the next capture), so it needs to be explicitly closed when
     * capturing is finished.
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
    private File mSequencePath;

    // Data initialized by startCapture()
    private int mIndex;
    private CaptureCallback mCaptureCallback;

    // (Potentially) persistent connection to the camera
    @SuppressWarnings("deprecation")
    private Camera mCamera;

    /**
     * Initialize the capturer
     * @param cameraId ID of the camera to use for capturing
     * @param sequenceName user-supplied name for the sequence
     */
    public ImageCapturer(int cameraId, String sequenceName) {

        mCameraId = cameraId;
        mSequencePath = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "ChronoSnap" + File.separator + sequenceName
        );
    }

    /**
     * Start capture of the specified image
     * @param index numerical index of the image to capture
     * @param captureCallback callback for capture events
     */
    public void startCapture(int index, CaptureCallback captureCallback) {

        mIndex = index;
        mCaptureCallback = captureCallback;

        // If the camera is already open, we can skip immediately to the
        // setup step, otherwise, we need to open the camera. It needs to be
        // done in a separate thread since it may block
        if (mCamera != null) {

            setup();

        } else {

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
                        return e.getMessage();
                    }
                }

                @Override
                protected void onPostExecute(String message) {

                    // If message is non-null, then call CaptureCallback.onError(),
                    // otherwise start the camera preview and begin autofocus
                    if (message != null) {
                        mCaptureCallback.onError(message);
                        return;
                    }

                    // Move to the setup step
                    setup();
                }
            }.execute();
        }
    }

    /**
     * Close the camera
     *
     * Calling this method will require the camera to be re-initialized the
     * next time that startCapture() is called.
     */
    public void close() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * Setup the camera in preparation for image capture
     */
    @SuppressWarnings("deprecation")
    private void setup() {

        // The preview needs to be started after each capture
        mCamera.startPreview();
        mCamera.autoFocus(new Camera.AutoFocusCallback() {

            @Override
            public void onAutoFocus(boolean success, Camera camera) {

                // TODO: if unable to focus, try again a couple of times
                // TODO: error message needs to be localized

                // If setup was not successful, we need to report an error
                if (!success) {
                    mCaptureCallback.onError("Unable to focus.");
                    return;
                }

                // Move to the capture step
                capture();
            }
        });
    }

    /**
     * Capture an image
     */
    @SuppressWarnings("deprecation")
    private void capture() {

        mCamera.takePicture(null, null, new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                // Ensure that the destination directory exists and create it otherwise
                if (!mSequencePath.exists()) {

                    // TODO: error message needs to be localized

                    // Report an error if the directory could not be created
                    if (!mSequencePath.mkdirs()) {
                        mCaptureCallback.onError("Unable to create storage directory.");
                        return;
                    }
                }

                // Create the file that will be used for storing the image
                File jpegFile = new File(mSequencePath, String.format("%04d", mIndex) + ".jpg");

                // Write the data to disk - note that this can fail and result in an error
                try {

                    OutputStream outputStream = new FileOutputStream(jpegFile);
                    outputStream.write(data);
                    outputStream.close();

                } catch (IOException e) {

                    mCaptureCallback.onError(e.getMessage());
                    return;
                }

                // Indicate that the capture was successful
                mCaptureCallback.onSuccess();
            }
        });
    }
}
