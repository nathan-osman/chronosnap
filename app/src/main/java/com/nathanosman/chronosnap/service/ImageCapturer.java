package com.nathanosman.chronosnap.service;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
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
     * Note that the camera may still be open at this point and .close()
     * should be called.
     */
    public interface CaptureCallback {

        /**
         * Called when a capture completes
         * @param errorMessage descriptive message if an error occurred or null
         *
         * errorMessage will be null if no error has occurred. Otherwise it
         * contains a human-readable description of the error.
         */
        void onComplete(String errorMessage);
    }

    // Data initialized in the constructor
    private Context mContext;
    private int mCameraId;
    private boolean mAutofocus;
    private File mSequencePath;

    // Data initialized by startCapture()
    private int mIndex;
    private CaptureCallback mCaptureCallback;

    // Connection to the camera (may be maintained for multiple captures)
    // Because this app works on 4.0.4+, we can't easily use the Camera2 API
    @SuppressWarnings("deprecation")
    private Camera mCamera;

    /**
     * Initialize the capturer
     * @param context calling context
     * @param cameraId ID of the camera to use for capturing
     * @param autofocus true to force the camera to focus before capture
     * @param sequenceName user-supplied name for the sequence
     */
    public ImageCapturer(Context context, int cameraId, boolean autofocus, CharSequence sequenceName) {

        mContext = context;
        mCameraId = cameraId;
        mAutofocus = autofocus;
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

                    // If message is non-null, then pass along the error message,
                    // otherwise start the camera preview and begin autofocus
                    if (message != null) {
                        mCaptureCallback.onComplete(message);
                    } else {
                        setup();
                    }
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
     *
     * Currently, this step is limited to autofocus.
     */
    @SuppressWarnings("deprecation")
    private void setup() {

        // The preview needs to be started after each capture
        mCamera.startPreview();

        if (mAutofocus) {

            // If autofocus is requested, it needs to be completed before the capture
            mCamera.autoFocus(new Camera.AutoFocusCallback() {

                @Override
                public void onAutoFocus(boolean success, Camera camera) {

                    // TODO: error message needs to be localized

                    // If the camera was unable to focus, report the error
                    if (!success) {
                        mCaptureCallback.onComplete("Unable to focus.");
                    } else {
                        capture();
                    }
                }
            });

        } else {

            // Skip to the capture step
            capture();
        }
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
                        mCaptureCallback.onComplete("Unable to create storage directory.");
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

                    mCaptureCallback.onComplete(e.getMessage());
                    return;
                }

                // Have the media scanner add the file
                MediaScannerConnection.scanFile(mContext, new String[]{jpegFile.getAbsolutePath()},
                        null, new MediaScannerConnection.OnScanCompletedListener() {

                            @Override
                            public void onScanCompleted(String path, Uri uri) {

                                // Indicate that the capture was successful
                                mCaptureCallback.onComplete(null);
                            }
                        });
            }
        });
    }
}
