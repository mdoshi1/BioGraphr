package com.example.lukas.euglenapatterns;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.Status;

import java.util.List;
import java.util.Set;


@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {

    // Camera used by the app
    private Camera mCamera;

    // Live camera feed
    private CameraPreview mPreview;

    // View that allow drawing via onTouchEvent(MotionEvent event)
    public static DrawingView mDrawingView;

    // Represents the device's Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Thread that communicates with the Bluetooth device
    public static BluetoothThread btt;

    // Controls when the Bluetooth thread should be running
    public static boolean ready = false;

    // Selected device used for casting
    private CastDevice mSelectedDevice;

    // Variables needed to enable Chromecast
    public static MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    public static MediaRouterCallback mMediaRouterCallback;

    // Object used for synchronization between the UI thread and the Bluetooth thread
    public static final Object lock = new Object();

    // Controls camera zoom level
    private static final int ZOOMLEVEL = 1;

    // Used to start Bluetooth request activity
    private static final int REQUEST_ENABLE_BT = 1;

    // Controls the size of the timestamp
    private static final int TEXTSIZE = 30;

    // Used for debugging purposes
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keeps screen on when in app
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar);

        // Checks if the correct version of Google Play Services is on the device
        checkGooglePlayServices();

        // Checks if device has a camera
        checkCameraHardware(this);

        // Initializes MediaRouter to allow Google casting
        initMediaRouter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Adds functionality to the Chromecast menu item
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handles presses on action bar items (excluding Chromecast item)
        switch (item.getItemId()) {
            case R.id.calibrate:
                calibrate();
                return true;
            case R.id.connect_bluetooth:
                connect();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        TextView text = (TextView) findViewById(R.id.title);
        text.setText("Biotic Processing Unit");

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
        if (!isRemoteDisplaying())
            if (mSelectedDevice != null)
                startCastService();

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

        // Releases camera
        releaseCamera();

        // Unregisters receivers for Bluetooth thread
        unregisterReceivers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaRouter.removeCallback(mMediaRouterCallback);

        // Stops the Bluetooth thread
        if (btt != null) {
            btt.interrupt();
            btt = null;
        }
    }

    // Checks if device has a camera
    private void checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {

            // Camera detected on device. Do nothing

        } else {

            // No camera detected on device

            // Dialog that notifies user that no camera was detected on the device and closes app
            new AlertDialog.Builder(context)
                    .setMessage("No camera detected on device")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .create()
                    .show();
        }
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

    // Connects the device to a remote device via Bluetooth
    private void connect() {

        // Verifies if device supports Bluetooth and enables it
        try {
            setUpBt();
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect!", e);
            return;
        }

        // Checks for paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {

            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                if (device.getAddress().equals(getString(R.string.address))) {
                    unregisterReceiver(mReceiver);
                    mBluetoothAdapter.cancelDiscovery();
                    btt = new BluetoothThread(device, this);
                    btt.start();
                    return;
                }
            }
        }

        // Starts Bluetooth discovery
        if (!mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.startDiscovery();
    }

    // Sets up Bluetooth on device
    private void setUpBt() throws Exception {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)

            // Device does not support Bluetooth
            throw new Exception("Device does not support Bluetooth");

        // If Bluetooth isn't enabled, asks user to enable Bluetooth
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {

            // Register the BroadcastReceiver
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Check if the BluetoothDevice object is the correct device
                if (device.getAddress().equals(getString(R.string.address))) {
                    unregisterReceiver(mReceiver);
                    mBluetoothAdapter.cancelDiscovery();

                    // Start new BluetoothThread
                    btt = new BluetoothThread(device, getApplicationContext());
                    btt.start();
                }
            }
        }
    };

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

    // Validates that the appropriate version of Google Play Services is available on the device.
    // If not, opens a dialog and directs the user to the Play Store if Google Play Services is out
    // of date or missing or to system settings if Google Play services is disabled
    private boolean checkGooglePlayServices() {
        int googlePlayServicesCheck = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (googlePlayServicesCheck == ConnectionResult.SUCCESS)
            return true;
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(googlePlayServicesCheck, this, 0);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });
        dialog.show();
        return false;
    }

    // Starts Chromecast service
    private void startCastService() {
        Intent intent = new Intent(this,
                MainActivity.class);
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

    // Callback to notify that Bluetooth is connected
    BroadcastReceiver btConnect = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Bluetooth is connected",
                    Toast.LENGTH_SHORT).show();
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

    // Registers the receivers needed to receive updates from the Bluetooth thread
    private void registerReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(btConnect,
                new IntentFilter("connect"));
        LocalBroadcastManager.getInstance(this).registerReceiver(btDisconnect,
                new IntentFilter("disconnect"));
    }

    // Unregisters the receivers used to receive updates from the Bluetooth thread
    private void unregisterReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(btConnect);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(btDisconnect);
    }

    // Starts calibration
    private void calibrate() {

        if (findViewById(R.id.adjust).getVisibility() == View.INVISIBLE) {
            TextView text;
            Button button;

            // Makes "Done" button visible
            button = (Button) findViewById(R.id.done);
            button.setVisibility(View.VISIBLE);

            // Hides menu
            button = (Button) findViewById(R.id.free_draw);
            button.setVisibility(View.INVISIBLE);
            button = (Button) findViewById(R.id.pattern_guessing);
            button.setVisibility(View.INVISIBLE);
            button = (Button) findViewById(R.id.science);
            button.setVisibility(View.INVISIBLE);

            // Hides text
            text = (TextView) findViewById(R.id.text_explore);
            text.setVisibility(View.INVISIBLE);
            text = (TextView) findViewById(R.id.text_game);
            text.setVisibility(View.INVISIBLE);
            text = (TextView) findViewById(R.id.text_science);
            text.setVisibility(View.INVISIBLE);

            // Makes calibration tools visible
            GridLayout adjust = (GridLayout) findViewById(R.id.adjust);
            adjust.setVisibility(View.VISIBLE);

            // Adds a DrawingView for calibration
            mDrawingView = new DrawingView(this);
            LinearLayout mDrawingPad = (LinearLayout) findViewById(R.id.drawing_pad);
            mDrawingPad.addView(mDrawingView);
        }
    }

    // Calibrates the casted pattern
    public void movePattern(View view) {
        PresentationService presentationService =
                (PresentationService) CastRemoteDisplayLocalService.getInstance();
        if (presentationService != null)
            presentationService.movePattern(view);
    }

    // Finishes calibration
    public void done(View view) {
        TextView text;
        Button button;

        // Hides "Done" button
        view.setVisibility(View.INVISIBLE);

        // Makes menu visible
        button = (Button) findViewById(R.id.free_draw);
        button.setVisibility(View.VISIBLE);
        button = (Button) findViewById(R.id.pattern_guessing);
        button.setVisibility(View.VISIBLE);
        button = (Button) findViewById(R.id.science);
        button.setVisibility(View.VISIBLE);

        // Makes text visible
        text = (TextView) findViewById(R.id.text_explore);
        text.setVisibility(View.VISIBLE);
        text = (TextView) findViewById(R.id.text_game);
        text.setVisibility(View.VISIBLE);
        text = (TextView) findViewById(R.id.text_science);
        text.setVisibility(View.VISIBLE);

        // Hides calibration tools
        GridLayout adjust = (GridLayout) findViewById(R.id.adjust);
        adjust.setVisibility(View.INVISIBLE);

        // Removes DrawingView
        LinearLayout mDrawingPad = (LinearLayout) findViewById(R.id.drawing_pad);
        mDrawingPad.removeAllViews();
        if (PresentationService.mDrawingView != null)
            PresentationService.mDrawingView.eraseCanvas();
    }

    // Handles presses on menu options
    public void chooseItem (View view) {
        switch (view.getId()) {

            // Starts FreeDrawActivity
            case R.id.text_explore:
            case R.id.free_draw:
                Intent freeDrawIntent = new Intent(this, FreeDrawActivity.class);
                startActivity(freeDrawIntent);
                break;

            // Starts GameActivity
            case R.id.text_game:
            case R.id.pattern_guessing:
                Intent gameIntent = new Intent(this, GameActivity.class);
                startActivity(gameIntent);
                break;

            //Starts ScienceActivity
            case R.id.text_science:
            case R.id.science:
                Intent scienceIntent = new Intent(this, ScienceActivity.class);
                startActivity(scienceIntent);
                break;
            default:
                break;
        }
    }

    // Focuses camera
    public void focus(View view) {
        mCamera.autoFocus(null);
    }
}

