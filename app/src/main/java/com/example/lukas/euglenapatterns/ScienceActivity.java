package com.example.lukas.euglenapatterns;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.cast.CastRemoteDisplayLocalService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ScienceActivity extends AppCompatActivity {

    private boolean pause = true;

    private boolean close = false;

    // Camera used by the app
    private Camera mCamera;

    // Live camera feed
    private CameraPreview mPreview;

    private int level;
    private int levelPattern;

    // Controls camera zoom level
    private static final int ZOOMLEVEL = 1;

    // Used for debugging purposes
    private static final String TAG = "ScienceActivity";

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
            ImageView currentPic = (ImageView) findViewById(R.id.current_pic);
            Bitmap bmp = shrinkBitmap(pictureFile.getAbsolutePath(), currentPic.getWidth(),
                    currentPic.getHeight());

            // Sets the current picture
            BitmapDrawable bmpd = new BitmapDrawable(bmp);
            currentPic.setBackground(bmpd);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_science);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        View decorView = getWindow().getDecorView();

        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        TextView text = (TextView) findViewById(R.id.title);
        text.setText("Science");

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

        // Sets the context of class App
        App.setContext(this);

        PresentationService presentationService = (PresentationService) CastRemoteDisplayLocalService.getInstance();
        if (presentationService != null)
            presentationService.updatePattern(getResources().getDrawable(R.drawable.black));
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Releases camera
        releaseCamera();

        pauseBluetooth();

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

    // Registers the receivers needed to receive updates from the Bluetooth thread
    private void registerReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(btDisconnect,
                new IntentFilter("disconnect"));
        LocalBroadcastManager.getInstance(this).registerReceiver(shutterUpdate,
                new IntentFilter("shutter"));
    }

    // Callback to notify that Bluetooth is disconnected
    BroadcastReceiver btDisconnect = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Bluetooth is disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };

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
    private Bitmap shrinkBitmap(String file, int width, int height) {
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

    // Unregisters the receivers used to receive updates from the Bluetooth thread
    private void unregisterReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(btDisconnect);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(shutterUpdate);
    }

    public void focus(View view) { mCamera.autoFocus(null); }

    // Changes the currently selected pattern
    public void changePattern(View view) {
        PresentationService presentationService =
                (PresentationService) CastRemoteDisplayLocalService.getInstance();
        if (presentationService != null)
            presentationService.updatePattern(((ImageButton) view).getDrawable());
    }

    // Sends broadcast to Bluetooth thread to open shutter
    public void open(View view) {
        if (close) {
            close = false;
            Intent intentBTConnect = new Intent("open");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intentBTConnect);

            ((Button) view).setText("Light On");
        } else {
            close = true;
            Intent intentBTConnect = new Intent("close");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intentBTConnect);

            ((Button) view).setText("Light Off");
        }
    }

    // Sends broadcast to Bluetooth thread to close shutter
    /*public void close(View view) {
        Intent intentBTConnect = new Intent("close");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentBTConnect);
    }*/

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

            ((Button) view).setText("Auto On");
        } else {
            pause = true;

            pauseBluetooth();

            Button button = (Button) findViewById(R.id.open);
            button.setClickable(true);

            ((Button) view).setText("Auto Off");
        }
    }

    // Pauses the Bluetooth thread
    /*public void pause(View view) {
        pauseBluetooth();

        Button button = (Button) findViewById(R.id.open);
        button.setVisibility(View.VISIBLE);
        button = (Button) findViewById(R.id.close);
        button.setVisibility(View.VISIBLE);
    }*/

    // Takes a picture
    public void snapPic(View view) { mCamera.takePicture(null, null, mPicture); }

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

    /*public void experiment(View view) {
        FrameLayout cameraPreview = (FrameLayout) findViewById(R.id.camera_preview);
        cameraPreview.setVisibility(View.INVISIBLE);

        Button button = (Button) findViewById(R.id.switch_view);
        button.setVisibility(View.INVISIBLE);
        button = (Button) findViewById(R.id.close);
        button.setVisibility(View.INVISIBLE);
        button = (Button) findViewById(R.id.open);
        button.setVisibility(View.INVISIBLE);

        // Adjusts the layout
        RelativeLayout buttons = (RelativeLayout) findViewById(R.id.buttons);
        buttons.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        buttons.addView(inflater.inflate(R.layout.experiment, null));

        PresentationService presentationService = (PresentationService) CastRemoteDisplayLocalService.getInstance();
        if (presentationService != null)
            presentationService.updatePattern(getResources().getDrawable(R.drawable.pattern1));

        level = 0;
    }

    public void compare(View view) {
        String imgID;
        int resID = view.getId();

        switch (level) {
            case 0:
                if (resID == R.id.choice2) {
                    ImageButton button;

                    MediaPlayer mp = MediaPlayer.create(this, R.raw.correct);
                    mp.start();

                    imgID = "pattern2";
                    resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                    button = (ImageButton) findViewById(R.id.choice1);
                    button.setImageResource(resID);

                    PresentationService presentationService = (PresentationService) CastRemoteDisplayLocalService.getInstance();
                    if (presentationService != null)
                        presentationService.updatePattern(button.getDrawable());

                    imgID = "redhor";
                    resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                    button = (ImageButton) findViewById(R.id.choice2);
                    button.setImageResource(resID);

                    TextView text = (TextView) findViewById(R.id.patterns);
                    text.setText("B/W vs. Red");

                    level++;
                } else
                    wrongChoice(resID);
                break;
            case 1:
                if (resID == R.id.choice1) {
                    ImageButton button;
                    MediaPlayer mp = MediaPlayer.create(this, R.raw.correct);
                    mp.start();

                    imgID = "cs1";
                    resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                    button = (ImageButton) findViewById(R.id.choice1);
                    button.setImageResource(resID);

                    imgID = "cs3";
                    resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                    button = (ImageButton) findViewById(R.id.choice2);
                    button.setImageResource(resID);

                    PresentationService presentationService = (PresentationService) CastRemoteDisplayLocalService.getInstance();
                    if (presentationService != null)
                        presentationService.updatePattern(button.getDrawable());

                    TextView text = (TextView) findViewById(R.id.patterns);
                    text.setText("Red vs. Blue");

                    level++;
                } else
                    wrongChoice(resID);
                break;
            case 2:
                if (resID == R.id.choice2) {
                    MediaPlayer mp = MediaPlayer.create(this, R.raw.correct);
                    mp.start();
                    pauseBluetooth();
                } else
                    wrongChoice(resID);
                break;
            default: break;
        }
    }

    // Adjusts UI to reflect a wrong choice
    private void wrongChoice(int id) {
        ImageButton button = (ImageButton) findViewById(id);
        button.setClickable(false);
        button.setImageDrawable(getResources().getDrawable(R.drawable.red_x));

        MediaPlayer mp = MediaPlayer.create(this, R.raw.wrong);
        mp.start();
    }*/

    // Callback to take a picture
    BroadcastReceiver shutterUpdate = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mCamera.takePicture(null, null, mPicture);
        }
    };

    // Stops/pauses the Bluetooth thread
    private void pauseBluetooth() {
        if (MainActivity.ready)
            MainActivity.ready = false;
    }

    public void finish(View view) { finish(); }

    public void learnMore(View view) {
        return;
    }

    public void help(View view) { return; }
}
