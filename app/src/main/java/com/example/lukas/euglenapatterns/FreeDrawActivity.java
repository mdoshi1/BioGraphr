package com.example.lukas.euglenapatterns;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;
import com.google.android.gms.common.api.Status;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class FreeDrawActivity extends AppCompatActivity {

    private boolean close = false;

    private boolean pause = true;

    private PopupWindow popUp;

    private boolean learnMore = false;

    private CustomAdapter customAdapter;

    // Camera used by the app
    private Camera mCamera;

    // Live camera feed
    private CameraPreview mPreview;

    // View that allows drawing via onTouchEvent(MotionEvent event)
    public static DrawingView mDrawingView;

    // Selected device used for casting
    private CastDevice mSelectedDevice;

    // Variables needed to enable Chromecast
    public static MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    public static MediaRouterCallback mMediaRouterCallback;

    // Controls camera zoom level
    private static final int ZOOMLEVEL = 1;

    // Controls the size of the timestamp
    private static final int TEXTSIZE = 30;

    // Used for debugging purposes
    private static final String TAG = "FreeDrawActivity";

    // Array containing pattern images to be displayed in two-player mode
    public static int[] patternImages = {R.drawable.pattern1, R.drawable.pattern7,
            R.drawable.pattern13, R.drawable.pattern2, R.drawable.pattern8, R.drawable.pattern14,
            R.drawable.pattern3, R.drawable.pattern9, R.drawable.pattern15, R.drawable.pattern4,
            R.drawable.pattern10, R.drawable.pattern16, R.drawable.pattern5, R.drawable.pattern11,
            R.drawable.pattern17, R.drawable.pattern6, R.drawable.pattern12, R.drawable.pattern18,
            R.drawable.a, R.drawable.h, R.drawable.k, R.drawable.l, R.drawable.stanford1,
            R.drawable.stanford2, R.drawable.x, R.drawable.black, R.drawable.white,
            R.drawable.pattern25, R.drawable.pattern26,
            R.drawable.pattern27, R.drawable.pattern28, R.drawable.pattern29, R.drawable.cs1,
            R.drawable.cs2, R.drawable.cs3, R.drawable.cs4, R.drawable.grad1, R.drawable.grad2,
            R.drawable.grad3, R.drawable.tess1, R.drawable.tess1b, R.drawable.tess2,
            R.drawable.tess2b, R.drawable.tess3, R.drawable.tess3b, R.drawable.tess4,
            R.drawable.tess4b, R.drawable.edge1, R.drawable.edge2, R.drawable.edge3,
            R.drawable.edge4, R.drawable.wg1, R.drawable.wg2, R.drawable.wg3, R.drawable.ill1,
            R.drawable.ill2, R.drawable.ill3, R.drawable.sw1, R.drawable.sw2, R.drawable.sw3
    };

    // Defines a PictureCallback interface to be used when takePicture() is called
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            // Creates a File to save an image
            File pictureFile = getOutputMediaFile();
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }

            // Writes picture data to a file
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            // Shrinks picture to the appropriate size to be displayed
            LinearLayout currentPic = (LinearLayout) findViewById(R.id.current_pic);
            Bitmap bmp = shrinkBitmap(pictureFile.getAbsolutePath(), currentPic.getWidth(),
                    currentPic.getHeight());

            // Draws a timestamp on the picture
            drawTimeStamp(bmp);

            // Sets the current picture
            BitmapDrawable bmpd = new BitmapDrawable(bmp);
            currentPic.setBackground(bmpd);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_free_draw);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar);

        // Sets the context of class App
        App.setContext(this);

        // Creates DrawingView to allow draw events on currently selected pattern
        initDrawingView();

        // Initializes MediaRouter to allow Google casting
        initMediaRouter();

        // Initializes the pattern selection ListView
        initPatternSelection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        View decorView = getWindow().getDecorView();

        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        TextView text = (TextView) findViewById(R.id.title);
        text.setText("Explore");

        if (mCamera == null) {

            // Create an instance of Camera
            if ((mCamera = getCameraInstance()) == null) {
                new AlertDialog.Builder(this)
                        .setMessage("Failed to open camera")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .create()
                        .show();
            }

            // Updates camera parameters
            updateCameraParameters();
        }

        // Creates a CameraPreview
        initCameraPreview();

        // Registers receivers to receive communication from Bluetooth thread
        registerReceivers();

        // Checks remote display and starts casting
        /*if (!isRemoteDisplaying())
            if (mSelectedDevice != null)
                startCastService();*/

        // Sets the context of class App
        App.setContext(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Pauses Bluetooth thread
        //pause(null);
        if (!pause)
            start(null);

        // Releases camera
        releaseCamera();

        // Unregisters receivers for Bluetooth thread
        unregisterReceivers();
    }

    // Safely opens camera
    private static Camera getCameraInstance() {
        Camera cam = null;
        Camera.CameraInfo camInfo = new Camera.CameraInfo();
        int camCount = Camera.getNumberOfCameras();

        // Scans CameraInfo for a back-facing camera
        for (int camID = 0; camID < camCount; camID++) {
            Camera.getCameraInfo(camID, camInfo);
            if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    cam = Camera.open(camID);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to open Camera");
                    e.printStackTrace();
                }
                return cam;
            }
        }

        return cam; // Returns null if camera is unavailable
    }

    // Updates camera parameters
    private void updateCameraParameters() {
        Camera.Parameters parameters = mCamera.getParameters();

        // Updates zoom level
        if (parameters.isZoomSupported())
            parameters.setZoom(ZOOMLEVEL);

        // Updates focus mode
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO))
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        mCamera.setParameters(parameters);
    }

    // Creates CameraPreview to display live feed
    private void initCameraPreview() {
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    // Creates a DrawingView to allow MotionEvents on current picture
    private void initDrawingView() {
        mDrawingView = new DrawingView(this);
        LinearLayout mDrawingPad = (LinearLayout) findViewById(R.id.drawing_pad);
        mDrawingPad.addView(mDrawingView);
    }

    // Releases camera for other applications
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();
            mCamera = null;
        }
    }

    // Changes the currently selected pattern
    public void changePattern(View view) {
        ImageButton selectedPattern = (ImageButton) view;
        PresentationService presentationService =
                (PresentationService) CastRemoteDisplayLocalService.getInstance();
        if (presentationService != null)
            presentationService.updatePattern(selectedPattern.getDrawable());

        // Clears mDrawingView
        mDrawingView.eraseCanvas();

        // Refreshes pattern selection
        customAdapter.notifyDataSetChanged();

        // Makes mDrawingView visible
        if (mDrawingView.getVisibility() == View.INVISIBLE)
            mDrawingView.setVisibility(View.VISIBLE);
    }

    // Takes a picture
    public void snapPic(View view) { mCamera.takePicture(null, null, mPicture); }

    // Creates a File for saving an image
    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "EuglenaPatternsApp");

        // Creates a storage directory if one doesn't exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("EuglenaPatternsApp", "failed to create directory");
                return null;
            }
        }

        // Creates and returns a media file
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
    }

    // Shrinks a Bitmap
    private static Bitmap shrinkBitmap(String file, int width, int height) {
        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();

        //Makes it so memory isn't allocated for the bitmap
        bmpFactoryOptions.inJustDecodeBounds = true;

        // Makes the bitmap mutable
        bmpFactoryOptions.inMutable = true;
        BitmapFactory.decodeFile(file, bmpFactoryOptions);

        // Determines how to scale the bitmap
        int heightRatio = (int) Math.ceil(bmpFactoryOptions.outHeight / (float) height);
        int widthRatio = (int) Math.ceil(bmpFactoryOptions.outWidth / (float) width);
        if (heightRatio > 1 || widthRatio > 1) {
            if (heightRatio > widthRatio) bmpFactoryOptions.inSampleSize = heightRatio;
            else bmpFactoryOptions.inSampleSize = widthRatio;
        }

        // Makes it so memory is allocated for the bitmap
        bmpFactoryOptions.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file, bmpFactoryOptions);
    }

    // Clears the DrawingViews
    public void clear(View view) {
        mDrawingView.eraseCanvas();
        if (PresentationService.mDrawingView != null)
            PresentationService.mDrawingView.eraseCanvas();

        // Makes mDrawingView visible
        if (mDrawingView.getVisibility() == View.INVISIBLE)
            mDrawingView.setVisibility(View.VISIBLE);
    }

    // Changes the currently selected drawing color
    public void changeColor(View view) {
        mDrawingView.changeColor(view.getId());
        if (PresentationService.mDrawingView != null)
            PresentationService.mDrawingView.changeColor(view.getId());
    }

    // Callback for Chromecast
    private class MediaRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo info) {
            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
            startCastService();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo info) {
            if (isRemoteDisplaying())
                CastRemoteDisplayLocalService.stopService();
            mSelectedDevice = null;
        }
    }

    // Initializes the MediaRouter needed to implement Google casting
    private void initMediaRouter() {
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(getString(R.string.app_id)))
                .build();
        mMediaRouterCallback = new MediaRouterCallback();
    }

    // Starts Chromecast service
    private void startCastService() {
        Intent intent = new Intent(this,
                FreeDrawActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(
                this, 0, intent, 0);

        CastRemoteDisplayLocalService.NotificationSettings settings =
                new CastRemoteDisplayLocalService.NotificationSettings.Builder()
                        .setNotificationPendingIntent(notificationPendingIntent).build();

        CastRemoteDisplayLocalService.startService(this,
                PresentationService.class, getString(R.string.app_id),
                mSelectedDevice, settings,
                new CastRemoteDisplayLocalService.Callbacks() {
                    @Override
                    public void onRemoteDisplaySessionStarted(
                            CastRemoteDisplayLocalService service) {
                        Log.d(TAG, "onServiceStarted");
                    }

                    @Override
                    public void onRemoteDisplaySessionError(Status errorReason) {
                        Log.d(TAG, "onServiceError: " + errorReason.getStatusCode());
                        initCastError();
                        mSelectedDevice = null;
                    }
                });
    }

    // Returns whether Chromecast is currently casting
    private static boolean isRemoteDisplaying() {
        return CastRemoteDisplayLocalService.getInstance() != null;
    }

    // Uses default media route if an error occurs during Chromecast setup
    private void initCastError() {
        Toast toast = Toast.makeText(
                getApplicationContext(), "Error starting the remote display", Toast.LENGTH_SHORT);
        mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        toast.show();
    }

    // Toggles the display of mDrawingView
    public void toggle(View view) {
        if (mDrawingView.getVisibility() == View.VISIBLE)
            mDrawingView.setVisibility(View.INVISIBLE);
        else
            mDrawingView.setVisibility(View.VISIBLE);
    }

    // Starts/resumes the Bluetooth thread
    public void start(View view) {
        if (pause) {
            pause = false;
            synchronized (MainActivity.lock) {
                if (!MainActivity.ready) {
                    MainActivity.ready = true;
                    MainActivity.lock.notify();
                }
            }

            Button button = (Button) findViewById(R.id.open);
            button.setClickable(false);

            ((Button) view).setText("Auto Off");
            /*button = (Button) findViewById(R.id.close);
            button.setVisibility(View.INVISIBLE);*/
        } else {
            pause = true;
            if (MainActivity.ready)
                MainActivity.ready = false;

            Button button = (Button) findViewById(R.id.open);
            button.setClickable(true);

            ((Button) view).setText("Auto On");
            /*button = (Button) findViewById(R.id.close);
            button.setVisibility(View.VISIBLE);*/
        }
    }

    // Pauses the Bluetooth thread
    /*public void pause(View view) {
        if (MainActivity.ready)
            MainActivity.ready = false;

        Button button = (Button) findViewById(R.id.open);
        button.setVisibility(View.VISIBLE);
        button = (Button) findViewById(R.id.close);
        button.setVisibility(View.VISIBLE);
    }*/

    // Callback to take a picture
    BroadcastReceiver shutterUpdate = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            snapPic(null);
        }
    };

    // Callback to notify that Bluetooth is disconnected
    BroadcastReceiver btDisconnect = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Bluetooth is disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };

    // Creates a ListView from patternImages using a CustomAdapter
    private void initPatternSelection() {
        ListView patternSelection = (ListView) findViewById(R.id.pattern_selection);
        customAdapter = new CustomAdapter(this, patternImages);
        //patternSelection.setAdapter(new CustomAdapter(this, patternImages));
        patternSelection.setAdapter(customAdapter);
    }

    // Registers the receivers needed to receive updates from the Bluetooth thread
    private void registerReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(shutterUpdate,
                new IntentFilter("shutter"));
        LocalBroadcastManager.getInstance(this).registerReceiver(btDisconnect,
                new IntentFilter("disconnect"));
    }

    // Unregisters the receivers used to receive updates from the Bluetooth thread
    private void unregisterReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(shutterUpdate);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(btDisconnect);
    }

    // Draws a timestamp on a picture
    private static void drawTimeStamp(Bitmap bmp) {
        Canvas comboImage = new Canvas(bmp);
        String timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        Paint paint = new Paint();
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(TEXTSIZE);
        paint.setColor(Color.WHITE);
        comboImage.drawText(timeStamp, 0, comboImage.getHeight(), paint);
    }


    // Sends broadcast to Bluetooth thread to open shutter
    public void open(View view) {
        if (close) {
            close = false;
            Intent intentBTConnect = new Intent("open");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intentBTConnect);

            ((Button) view).setText("Light Off");
        } else {
            close = true;
            Intent intentBTConnect = new Intent("close");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intentBTConnect);

            ((Button) view).setText("Light On");
        }
    }

    // Sends broadcast to Bluetooth thread to close shutter
    /*public void close(View view) {
        Intent intentBTConnect = new Intent("close");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentBTConnect);
    }*/

    // Toggles between Image View and Live View
    public void switchView(View view) {
        Button button = (Button) findViewById(R.id.switch_view);

        // Switches to Image View
        if (button.getText().toString() == "Still") {
            button.setText("Live");
            FrameLayout cameraPreview = (FrameLayout) findViewById(R.id.camera_preview);
            cameraPreview.setVisibility(View.INVISIBLE);

        // Switches to Live View
        } else {
            button.setText("Still");
            FrameLayout cameraPreview = (FrameLayout) findViewById(R.id.camera_preview);
            cameraPreview.setVisibility(View.VISIBLE);
        }
    }

    // Finishes calibration
    public void done(View view) {

        // Hides calibration buttons
        view.setVisibility(View.INVISIBLE);
        GridLayout adjust = (GridLayout) findViewById(R.id.adjust);
        adjust.setVisibility(View.INVISIBLE);

        // Makes view toggle button visible
        Button button = (Button) findViewById(R.id.switch_view);
        button.setVisibility(View.VISIBLE);

        // Switches to the view present before calibration
        if (button.getText().toString() == "Go to Live View") {
            FrameLayout cameraPreview = (FrameLayout) findViewById(R.id.camera_preview);
            cameraPreview.setVisibility(View.INVISIBLE);
        }
    }

    // Finishes activity
    public void finish(View view) {
        clear(null);
        finish();
    }

    // Focuses camera
    public void focus(View view) { mCamera.autoFocus(null); }

    //temp
    public void updateGIF(View view) {
        EditText text = (EditText) findViewById(R.id.edit_text);
        int duration = Integer.parseInt(text.getText().toString());

        Log.d(TAG, "Duration: " + duration);

        PresentationService presentationService =
                (PresentationService) CastRemoteDisplayLocalService.getInstance();
        if (presentationService != null)
            presentationService.updateGIF(view.getId(), duration);
    }

    public void learnMore(View view) {
        if (!learnMore) {
            //view.setBackgroundResource(R.drawable.light_bulb_on);

            learnMore = true;
            popUp = new PopupWindow(this);
            RelativeLayout layout = new RelativeLayout(this);

            popUp.setOutsideTouchable(true);
            layout.setBackgroundResource(R.drawable.learn_more);
            popUp.setContentView(layout);

            /*popUp.setTouchInterceptor(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        Button button = (Button) findViewById(R.id.learn_more);
                        button.setBackgroundResource(R.drawable.light_bulb_off);
                        learnMore = false;
                        popUp.dismiss();
                        return true;
                    }
                    return false;
                }
            });*/

            popUp.showAtLocation(layout, Gravity.CENTER, 0, 0);
            popUp.update(0, 0, 1665, 1249);
        } else {
            //view.setBackgroundResource(R.drawable.light_bulb_off);

            learnMore = false;
            popUp.dismiss();
        }
    }

    public void help(View view) { return; }
}
