package com.example.lukas.euglenapatterns;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;


public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    public static final String TAG = "CameraPreview";

    public CameraPreview (Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void surfaceCreated (SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) { Log.d(TAG, "Error setting camera preview: " + e.getMessage()); }
    }

    public void surfaceDestroyed (SurfaceHolder holder) {
        // Empty. MainActivity takes care of releasing camera preview
    }

    public void surfaceChanged (SurfaceHolder holder, int format, int w, int h) {
        // Empty. The surface never changes
    }
}
