package com.example.lukas.euglenapatterns;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import android.support.v4.content.LocalBroadcastManager;
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

import com.google.android.gms.cast.CastRemoteDisplayLocalService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class GameActivity extends AppCompatActivity {

    private boolean stop = true;

    private int pointsPlayerOne = 0;

    private int pointsPlayerTwo = 0;

    private Thread patternSwitchThread = null;

    // Camera used by the app
    private Camera mCamera;

    // Live camera feed
    private CameraPreview mPreview;

    // Timer used in one-player mode
    private TextView timer;

    // System time in milliseconds when a new game is started
    private long startTime;

    // Stores the resID of the current correct pattern
    private int levelPattern;

    private Drawable playerOnePattern = null;

    private Drawable playerTwoPattern = null;

    // Stores the patterns to be used as choices in one-player mode
    private Integer[] choices;

    // Current level of player in one-player mode
    private int currentLevel;

    // Time penalty of player in one-player mode
    private int timePenalty;

    // Handles timer events
    private Handler timerHandler;

    // Controls camera zoom level
    private static final int ZOOMLEVEL = 1;

    // Controls the number of levels in one-player mode
    private static final int LEVELS = 3;

    // Controls the number of options in one-player mode
    private static final int ONEPLAYEROPTIONS = 4;

    // Controls the number of options in two-player mode
    private static final int TWOPLAYEROPTIONS = 8;

    // Used for debugging purposes
    public static final String TAG = "GameActivity";

    // Array containing pattern images to be displayed in two-player mode
    public static int[] patternImages;

    // Min and max pattern numbers
    private int MIN;
    private int MAX;

    private String gameType;

    // Used to update the timer in one-player mode
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {

            // Converts elapsed system time to minutes and seconds
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            // Updates timer with new elapsed time
            timer.setText("Timer: " + String.format("%d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_players);

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
        text.setText("Game");

        if (mCamera == null) {

            // Creates an instance of Camera
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
        if (findViewById(R.id.camera_preview) != null)
            createCameraPreview();

        // Registers a receiver to receive updates from the Bluetooth thread
        LocalBroadcastManager.getInstance(this).registerReceiver(shutterUpdate,
                new IntentFilter("shutter"));

        // Initializes the buttons
        //initButtons();

        //Initializes the choices array
        //choices = new Integer[OPTIONS];

        // Initializes the timer
        //timer = (TextView) findViewById(R.id.timer);
        //timerHandler = new Handler();

        // Focuses the camera
        //mCamera.autoFocus(null);

        PresentationService presentationService = (PresentationService) CastRemoteDisplayLocalService.getInstance();
        if (presentationService != null)
            presentationService.updatePattern(getResources().getDrawable(R.drawable.black));
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stops/pauses the Bluetooth thread
        pauseBluetooth();

        // Makes casted pattern default black
        //stopCastPattern();

        // Releases camera
        releaseCamera();

        // Removes any pending posts of timerRunnable in message queue
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerHandler = null;
        }

        // Unregisters receiver for Bluetooth thread
        LocalBroadcastManager.getInstance(this).unregisterReceiver(shutterUpdate);
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
    private void createCameraPreview() {
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    // Releases camera for other applications
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    // Initializes the buttons in one-player mode
    private void initButtons() {
        int options;
        if (findViewById(R.id.start) == null)
            options = ONEPLAYEROPTIONS;
         else
            options = TWOPLAYEROPTIONS;

        for (int i = 0; i < options; i++) {
            String imgID = "option" + i;
            int resID = getResources().getIdentifier(imgID, "id", "com.example.lukas.euglenapatterns");
            ImageButton img = (ImageButton) findViewById(resID);
            img.setClickable(false);
        }
    }

    // Starts a new game in one-player mode
    public void newGame(View view) {

        if (stop) {
            stop = false;

            Button button = (Button) findViewById(R.id.new_game);
            button.setBackgroundResource(R.drawable.pause);

            // Resets the UI elements
            resetUI(view);

            // Starts/resumes the Bluetooth thread
            startBluetooth();

            // Starts the timer
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);

            // Starts the game
            play();
        } else {
            stop = true;

            // Stops/pauses the Bluetooth thread
            pauseBluetooth();

            // Makes cast pattern black
            stopCastPattern();

            // Pauses UI in one player-mode
            pauseUI(view);

            Button button = (Button) findViewById(R.id.new_game);
            button.setBackgroundResource(R.drawable.play);

        }
    }

    // Resets the UI elements in one-player mode
    private void resetUI(View view) {

        // Makes the "New Game" button not clickable
        //view.setClickable(false);

        // Makes the "Stop" button clickable
        //Button stop = (Button) findViewById(R.id.stop);
        //stop.setClickable(true);

        // Resets game elements
        currentLevel = 0;
        timePenalty = 0;
        TextView total = (TextView) findViewById(R.id.total_time);
        total.setText("Total:     ");
        TextView penalty = (TextView) findViewById(R.id.penalty);
        penalty.setText("Penalty: +0:00");
    }

    // Runs the game in one-player mode
    private void play() {

        // Updates UI to next level
        TextView level = (TextView) findViewById(R.id.level);
        level.setText("Pattern " + (currentLevel + 1) + " /3");

        // temp
        if (gameType == "fixed")
            fixPatterns();
        else if (gameType == "fixedColorOne")
            fixPatternsColorOne();
        else if (gameType == "fixedColorTwo")
            fixPatternsColorTwo();
        else if (gameType == "fixedColorThree")
            fixPatternsColorThree();
        else

            // Randomly selects patterns to be used as options
            randomizePatterns();

        // Updates the available options
        updateOptions();

        // Updates the casted pattern
        updateCastPattern();
    }

    //temp
    private void fixPatternsColorOne() {
        String imgID;
        int resID;
        switch (currentLevel) {
            case 0:
                imgID = "pattern8";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[0] = resID;
                imgID = "pattern6";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[1] = resID;
                imgID = "pattern16";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[2] = resID;
                imgID = "pattern11";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[3] = resID;

                levelPattern = choices[1];
                break;
            case 1:
                imgID = "pattern5";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[0] = resID;
                imgID = "pattern13";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[1] = resID;
                imgID = "pattern14";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[2] = resID;
                imgID = "pattern9";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[3] = resID;

                levelPattern = choices[3];
                break;
            case 2:
                imgID = "pattern34";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[0] = resID;
                imgID = "pattern18";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[1] = resID;
                imgID = "pattern54";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[2] = resID;
                imgID = "pattern1";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[3] = resID;

                levelPattern = choices[0];
                break;
            default:
                break;
        }
    }

    private void fixPatternsColorTwo() {
        String imgID;
        int resID;
        switch (currentLevel) {
            case 0:
                imgID = "pattern15";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[0] = resID;
                imgID = "pattern8";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[1] = resID;
                imgID = "pattern39";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[2] = resID;
                imgID = "pattern57";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[3] = resID;

                levelPattern = choices[3];
                break;
            case 1:
                imgID = "pattern17";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[0] = resID;
                imgID = "pattern44";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[1] = resID;
                imgID = "pattern50";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[2] = resID;
                imgID = "pattern9";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[3] = resID;

                levelPattern = choices[3];
                break;
            case 2:
                imgID = "pattern35";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[0] = resID;
                imgID = "pattern56";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[1] = resID;
                imgID = "pattern5";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[2] = resID;
                imgID = "pattern44";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[3] = resID;

                levelPattern = choices[3];
                break;
            default:
                break;
        }
    }

    private void fixPatternsColorThree() {
        String imgID;
        int resID;
        switch (currentLevel) {
            case 0:
                imgID = "pattern2";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[0] = resID;
                imgID = "pattern12";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[1] = resID;
                imgID = "pattern33";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[2] = resID;
                imgID = "pattern60";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[3] = resID;

                levelPattern = choices[2];
                break;
            case 1:
                imgID = "pattern47";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[0] = resID;
                imgID = "pattern13";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[1] = resID;
                imgID = "pattern36";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[2] = resID;
                imgID = "pattern31";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[3] = resID;

                levelPattern = choices[0];
                break;
            case 2:
                imgID = "pattern2";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[0] = resID;
                imgID = "pattern34";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[1] = resID;
                imgID = "pattern35";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[2] = resID;
                imgID = "pattern33";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[3] = resID;

                levelPattern = choices[3];
                break;
            default:
                break;
        }
    }

    //temp
    private void fixPatterns() {
        String imgID;
        int resID;
        switch (currentLevel) {
            case 0:
                imgID = "pattern8";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[0] = resID;
                imgID = "pattern6";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[1] = resID;
                imgID = "pattern16";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[2] = resID;
                imgID = "pattern11";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[3] = resID;

                levelPattern = choices[1];
                break;
            case 1:
                imgID = "pattern5";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[0] = resID;
                imgID = "pattern13";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[1] = resID;
                imgID = "pattern14";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[2] = resID;
                imgID = "pattern9";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[3] = resID;

                levelPattern = choices[3];
                break;
            case 2:
                imgID = "pattern10";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[0] = resID;
                imgID = "pattern2";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[1] = resID;
                imgID = "pattern13";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[2] = resID;
                imgID = "pattern12";
                resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
                choices[3] = resID;

                levelPattern = choices[2];
                break;
            default:
                break;
        }
    }

    // Randomly selects patterns to be used as options in one-player mode
    private void randomizePatterns() {

        //temp
        if (gameType == "L2S")
            chooseSizeLarge();
        else if (gameType == "S2L")
            chooseSizeSmall();

        Random random = new Random();
        String imgID;
        int resID;
        for (int i = 0; i < ONEPLAYEROPTIONS; i++) {
            int num = random.nextInt(MAX - MIN + 1) + MIN;
            imgID = "pattern" + num;
            resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
            if (Arrays.asList(choices).contains(resID))

                // Array already contains pattern
                i--;
            else
                choices[i] = resID;
        }

        // Assigns the first pattern as the correct pattern
        levelPattern = choices[0];

        // Shuffles selected patterns
        shuffleArray(choices);
    }

    public void randomize(View view) {
        TextView text = (TextView) findViewById(R.id.win_player_one);
        text.setVisibility(View.INVISIBLE);
        text = (TextView) findViewById(R.id.win_player_two);
        text.setVisibility(View.INVISIBLE);

        Button button;
        button = (Button) findViewById(R.id.start);
        if (button.isClickable())
            button.setClickable(false);

        playerOnePattern = null;
        playerTwoPattern = null;

        Random random = new Random();
        String imgID;
        int resID;
        for (int i = 0; i < TWOPLAYEROPTIONS; i++) {
            int num = random.nextInt(MAX - MIN + 1) + MIN;
            imgID = "pattern" + num;
            resID = this.getResources().getIdentifier(imgID, "drawable", this.getPackageName());
            if (Arrays.asList(choices).contains(resID))

                // Array already contains pattern
                i--;
            else {
                choices[i] = resID;

                String imgID2 = "option" + i;
                int resID2 = getResources().getIdentifier(imgID2, "id", "com.example.lukas.euglenapatterns");
                ImageButton img = (ImageButton) findViewById(resID2);
                img.setImageResource(resID);
                if (!img.isClickable())
                    img.setClickable(true);

                //img.setBackgroundResource(0);

                /*imgID2 = "choice" + i;
                resID2 = getResources().getIdentifier(imgID2, "id", "com.example.lukas.euglenapatterns");
                img = (ImageButton) findViewById(resID2);
                img.setImageResource(resID);*/
            }
        }
    }

    // temp
    private void chooseSizeLarge() {
        switch (currentLevel) {
            case 0:
                MIN = 1;
                MAX = 6;
                break;
            case 1:
                MIN = 7;
                MAX = 12;
                break;
            case 2:
                MIN = 13;
                MAX = 18;
                break;
            default:
                break;
        }
    }

    //temp
    private void chooseSizeSmall() {
        switch (currentLevel) {
            case 2:
                MIN = 1;
                MAX = 6;
                break;
            case 1:
                MIN = 7;
                MAX = 12;
                break;
            case 0:
                MIN = 13;
                MAX = 18;
                break;
            default:
                break;
        }
    }

    // Updates the casted pattern in one-player mode
    private void updateCastPattern() {
        PresentationService presentationService = (PresentationService) CastRemoteDisplayLocalService.getInstance();
        if (presentationService != null)
            presentationService.updatePattern(getResources().getDrawable(levelPattern));
    }

    // Updates the available options in one-player mode
    private void updateOptions() {
        String imgID;
        int resID;
        for (int i = 0; i < ONEPLAYEROPTIONS; i++) {
            imgID = "option" + i;
            resID = getResources().getIdentifier(imgID, "id", "com.example.lukas.euglenapatterns");
            ImageButton img = (ImageButton) findViewById(resID);
            img.setImageResource(choices[i]);
            if (!img.isClickable())
                img.setClickable(true);
        }
    }

    // Stops the game
    public void stop(View view) {

        // Stops/pauses the Bluetooth thread
        pauseBluetooth();

        // Makes cast pattern black
        stopCastPattern();

        // Checks if the game is in one-player mode
        if (findViewById(R.id.new_game) != null)

            // Pauses UI in one player-mode
            pauseUI(view);
        else {

            // Pauses UI in two player-mode
            pauseTwoPlayer(view);

            if (patternSwitchThread != null) {
                patternSwitchThread.interrupt();
                patternSwitchThread = null;
            }
        }

    }

    // Makes cast pattern black when game is stopped in one-player mode
    private void stopCastPattern() {
        PresentationService presentationService = (PresentationService) CastRemoteDisplayLocalService.getInstance();
        if (presentationService != null)
            presentationService.updatePattern(getResources().getDrawable(R.drawable.black));
    }

    // Renders the UI in a paused state in one-player mode
    private void pauseUI(View view) {
        timerHandler.removeCallbacks(timerRunnable);
        /*Button newGame = (Button) findViewById(R.id.new_game);
        newGame.setClickable(true);
        if (view != null)
            view.setClickable(false);*/

        for (int i = 0; i < ONEPLAYEROPTIONS; i++) {
            String imgID = "option" + i;
            int resID = getResources().getIdentifier(imgID, "id", "com.example.lukas.euglenapatterns");
            ImageButton img = (ImageButton) findViewById(resID);
            img.setClickable(false);
        }

        String text = timer.getText().toString().substring(7);
        int minutes = Integer.parseInt(text.substring(0, text.indexOf(':')));
        int seconds = Integer.parseInt(text.substring(text.indexOf(':') + 1));
        seconds = seconds + timePenalty;
        minutes = minutes + seconds / 60;
        seconds = seconds % 60;

        TextView total = (TextView) findViewById(R.id.total_time);
        total.setText("Total: " + String.format("%d:%02d", minutes, seconds));
    }

    public void compare(View view) {
        int resID = levelPattern;
        switch (view.getId()) {
            case R.id.option0:
                if (choices[0] == resID) {
                    MediaPlayer mp = MediaPlayer.create(this, R.raw.correct);
                    mp.start();
                    currentLevel++;
                    if (currentLevel == LEVELS)
                        //stop(null);
                        newGame(null);
                    else
                        play();
                } else
                    wrongChoice(R.id.option0);
                break;
            case R.id.option1:
                if (choices[1] == resID) {
                    MediaPlayer mp = MediaPlayer.create(this, R.raw.correct);
                    mp.start();
                    currentLevel++;
                    if (currentLevel == LEVELS)
                        //stop(null);
                        newGame(null);
                    else
                        play();
                } else
                    wrongChoice(R.id.option1);
                break;
            case R.id.option2:
                if (choices[2] == resID) {
                    MediaPlayer mp = MediaPlayer.create(this, R.raw.correct);
                    mp.start();
                    currentLevel++;
                    if (currentLevel == LEVELS)
                        //stop(null);
                        newGame(null);
                    else
                        play();
                } else
                    wrongChoice(R.id.option2);
                break;
            case R.id.option3:
                if (choices[3] == resID) {
                    MediaPlayer mp = MediaPlayer.create(this, R.raw.correct);
                    mp.start();
                    currentLevel++;
                    if (currentLevel == LEVELS)
                        //stop(null);
                        newGame(null);
                    else
                        play();
                } else
                    wrongChoice(R.id.option3);
                break;
            default:
                break;
        }
    }

    // Adjusts UI to reflect a wrong choice
    private void wrongChoice(int id) {
        ImageButton button = (ImageButton) findViewById(id);
        button.setClickable(false);
        button.setImageDrawable(getResources().getDrawable(R.drawable.red_x));

        if (findViewById(R.id.start) == null) {
            TextView penalty = (TextView) findViewById(R.id.penalty);
            timePenalty += 10;
            int minutes = timePenalty / 60;
            int seconds = timePenalty % 60;
            penalty.setText("Penalty: +" + String.format("%d:%02d", minutes, seconds));
        }


        MediaPlayer mp = MediaPlayer.create(this, R.raw.wrong);
        mp.start();
    }

    // Implementing Fisher–Yates shuffle
    private static void shuffleArray(Integer[] ar) {
        Random rnd = new Random();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);

            // Simple swap
            int a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    // Defines a PictureCallback interface to be used when takePicture() is called
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            // Creates a File to save an image
            File pictureFile = getOutputMediaFile();
            if (pictureFile == null){
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
            }
            catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            // Shrinks picture to the size of the ImageView
            ImageView current = (ImageView) findViewById(R.id.current);
            Bitmap bitMap = shrinkBitmap(pictureFile.getAbsolutePath(), current.getWidth(),
                    current.getHeight());
            current.setImageBitmap(bitMap);
        }
    };

    // Creates a File for saving an image
    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "EuglenaPatternsApp");

        // Creates a storage directory if one doesn't exist
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
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

    // Callback to take a picture
    BroadcastReceiver shutterUpdate = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mCamera.takePicture(null, null, mPicture);
        }
    };

    // Readies the activity for one-player mode
    public void onePlayer(View view) {

        // Adjusts the layout
        RelativeLayout choosePlayers = (RelativeLayout) findViewById(R.id.choose);
        choosePlayers.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        choosePlayers.addView(inflater.inflate(R.layout.activity_game, null));

        // Creates a CameraPreview
        createCameraPreview();

        // Initializes the buttons
        initButtons();

        //Initializes the choices array
        choices = new Integer[ONEPLAYEROPTIONS];

        // Initializes the timer
        timer = (TextView) findViewById(R.id.timer);
        timerHandler = new Handler();

        TextView text = (TextView) findViewById(R.id.title);
        text.setText("Game - One Player");
    }

    // Readies the activity for two-player mode
    public void twoPlayers(View view) {

        // Adjusts the layout
        RelativeLayout choosePlayers = (RelativeLayout) findViewById(R.id.choose);
        choosePlayers.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        choosePlayers.addView(inflater.inflate(R.layout.pattern_guess_two, null));

        // Creates a CameraPreview
        createCameraPreview();

        initButtons();

        //Initializes the choices array
        choices = new Integer[TWOPLAYEROPTIONS];

        TextView text = (TextView) findViewById(R.id.title);
        text.setText("Game - 2 Players");
    }

    // Starts/resumes the Bluetooth thread
    private void startBluetooth() {
        synchronized (MainActivity.lock) {
            if (!MainActivity.ready) {
                MainActivity.ready = true;
                MainActivity.lock.notify();
            }
        }
    }

    // Stops/pauses the Bluetooth thread
    private void pauseBluetooth() {
        if (MainActivity.ready)
            MainActivity.ready = false;
    }

    // Changes the currently casted pattern in two-player mode
    public void changePattern(View view) {
        ImageButton selectedPattern = (ImageButton) view;
        PresentationService presentationService = (PresentationService) CastRemoteDisplayLocalService.getInstance();
        if (presentationService != null)
            presentationService.updatePattern(selectedPattern.getDrawable());
    }

    // Starts a game in two-player mode
    public void start(View view) {
        if (stop) {
            stop = false;

            //view.setClickable(false);
            //Button button = (Button) findViewById(R.id.stop);
            //button.setClickable(true);
            Button button = (Button) findViewById(R.id.randomize);
            button.setClickable(false);
            //button = (Button) findViewById(R.id.reset);
            //button.setClickable(false);

            button = (Button) findViewById(R.id.start);
            button.setBackgroundResource(R.drawable.pause);

            TextView text = (TextView) findViewById(R.id.win_player_one);
            text.setVisibility(View.INVISIBLE);
            text = (TextView) findViewById(R.id.win_player_two);
            text.setVisibility(View.INVISIBLE);

            ImageButton img;
            for (int i = 0; i < ONEPLAYEROPTIONS; i++) {
                String imgID = "option" + i;
                int resID = getResources().getIdentifier(imgID, "id", "com.example.lukas.euglenapatterns");
                img = (ImageButton) findViewById(resID);
                img.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        if (((ImageButton) view).getDrawable().getConstantState().equals(playerTwoPattern.getConstantState())) {
                            TextView text = (TextView) findViewById(R.id.win_player_one);
                            text.setVisibility(View.VISIBLE);

                            pointsPlayerOne++;
                            text = (TextView) findViewById(R.id.points_player_one);
                            text.setText("" + pointsPlayerOne);

                            MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.correct);
                            mp.start();

                            //stop(null);
                            start(null);
                        } else {
                            pointsPlayerOne--;
                            TextView text = (TextView) findViewById(R.id.points_player_one);
                            text.setText("" + pointsPlayerOne);
                            wrongChoice(view.getId());
                        }
                    }
                });

                img.setImageResource(choices[i + 4]);
            }

            for (int i = ONEPLAYEROPTIONS; i < TWOPLAYEROPTIONS; i++) {
                String imgID = "option" + i;
                int resID = getResources().getIdentifier(imgID, "id", "com.example.lukas.euglenapatterns");
                img = (ImageButton) findViewById(resID);
                img.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        if (((ImageButton) view).getDrawable().getConstantState().equals(playerOnePattern.getConstantState())) {
                            TextView text = (TextView) findViewById(R.id.win_player_two);
                            text.setVisibility(View.VISIBLE);

                            pointsPlayerTwo++;
                            text = (TextView) findViewById(R.id.points_player_two);
                            text.setText("" + pointsPlayerTwo);

                            MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.correct);
                            mp.start();

                            //stop(null);
                            start(null);
                        } else {
                            pointsPlayerTwo--;
                            TextView text = (TextView) findViewById(R.id.points_player_two);
                            text.setText("" + pointsPlayerTwo);
                            wrongChoice(view.getId());
                        }

                    }
                });

                img.setImageResource(choices[i - 4]);
            }

            startPatternSwitch();
            startBluetooth();
        } else {
            // Stops/pauses the Bluetooth thread
            pauseBluetooth();

            // Makes cast pattern black
            stopCastPattern();


            stop = true;

            Button button = (Button) findViewById(R.id.start);
            button.setBackgroundResource(R.drawable.play);

            // Pauses UI in two player-mode
            pauseTwoPlayer(null);

            if (patternSwitchThread != null) {
                patternSwitchThread.interrupt();
                patternSwitchThread = null;
            }

        }
    }

    // Finishes activity
    public void finish(View view) {
        if (findViewById(R.id.camera_preview) != null) {

            // Adjusts the layout
            RelativeLayout choosePlayers = (RelativeLayout) findViewById(R.id.choose);
            choosePlayers.removeAllViews();
            LayoutInflater inflater = getLayoutInflater();
            choosePlayers.addView(inflater.inflate(R.layout.choose_players, null));

            pauseBluetooth();

            if (patternSwitchThread != null) {
                patternSwitchThread.interrupt();
                patternSwitchThread = null;
            }

            TextView text = (TextView) findViewById(R.id.title);
            text.setText("Game");
        } else
            finish();
    }

    // Focuses camera
    public void focus(View view) {
        mCamera.autoFocus(null);
    }

    // temp
    public void chooseGameType(View view) {
        switch (((Button)view).getText().toString()) {
            case "All patterns":

                // All patterns
                patternImages = new int[] {R.drawable.pattern1, R.drawable.pattern2,
                        R.drawable.pattern3, R.drawable.pattern4, R.drawable.pattern5,
                        R.drawable.pattern6, R.drawable.pattern7, R.drawable.pattern8,
                        R.drawable.pattern9, R.drawable.pattern10, R.drawable.pattern11,
                        R.drawable.pattern12, R.drawable.pattern13, R.drawable.pattern14,
                        R.drawable.pattern15, R.drawable.pattern16, R.drawable.pattern17,
                        R.drawable.pattern18, R.drawable.a, R.drawable.h, R.drawable.k,
                        R.drawable.l, R.drawable.stanford1, R.drawable.stanford2, R.drawable.x
                };

                // Min and max pattern numbers
                MIN = 1;
                MAX = 18;

                onePlayer(null);

                break;
            case "Large to small":

                // Large to small patterns
                patternImages = new int[] {R.drawable.pattern1, R.drawable.pattern2,
                        R.drawable.pattern3, R.drawable.pattern4, R.drawable.pattern5,
                        R.drawable.pattern6, R.drawable.pattern7, R.drawable.pattern8,
                        R.drawable.pattern9, R.drawable.pattern10, R.drawable.pattern11,
                        R.drawable.pattern12, R.drawable.pattern13, R.drawable.pattern14,
                        R.drawable.pattern15, R.drawable.pattern16, R.drawable.pattern17,
                        R.drawable.pattern18, R.drawable.a, R.drawable.h, R.drawable.k,
                        R.drawable.l, R.drawable.stanford1, R.drawable.stanford2, R.drawable.x
                };

                // Min and max pattern numbers
                MIN = 1;
                MAX = 18;
                gameType = "L2S";

                onePlayer(null);

                break;
            case "Small to large":

                // Small to large patterns
                patternImages = new int[] {R.drawable.pattern1, R.drawable.pattern2,
                        R.drawable.pattern3, R.drawable.pattern4, R.drawable.pattern5,
                        R.drawable.pattern6, R.drawable.pattern7, R.drawable.pattern8,
                        R.drawable.pattern9, R.drawable.pattern10, R.drawable.pattern11,
                        R.drawable.pattern12, R.drawable.pattern13, R.drawable.pattern14,
                        R.drawable.pattern15, R.drawable.pattern16, R.drawable.pattern17,
                        R.drawable.pattern18, R.drawable.a, R.drawable.h, R.drawable.k,
                        R.drawable.l, R.drawable.stanford1, R.drawable.stanford2, R.drawable.x
                };

                // Min and max pattern numbers
                MIN = 1;
                MAX = 18;
                gameType = "S2L";

                onePlayer(null);

                break;
            case "Only stripes":

                onePlayer(null);

                break;
            case "Only dots":

                onePlayer(null);

                break;
            case "Only small":

                // Small patterns
                patternImages = new int[] {R.drawable.pattern13, R.drawable.pattern14,
                        R.drawable.pattern15, R.drawable.pattern16, R.drawable.pattern17,
                        R.drawable.pattern18, R.drawable.a, R.drawable.h, R.drawable.k,
                        R.drawable.l, R.drawable.stanford1, R.drawable.stanford2, R.drawable.x
                };

                // Min and max pattern numbers
                MIN = 13;
                MAX = 18;

                onePlayer(null);

                break;
            case "Only medium":

                // Medium patterns
                patternImages = new int[] {R.drawable.pattern7, R.drawable.pattern8,
                        R.drawable.pattern9, R.drawable.pattern10, R.drawable.pattern11,
                        R.drawable.pattern12, R.drawable.a, R.drawable.h, R.drawable.k,
                        R.drawable.l, R.drawable.stanford1, R.drawable.stanford2, R.drawable.x
                };

                // Min and max pattern numbers
                MIN = 7;
                MAX = 12;

                onePlayer(null);

                break;
            case "Only large":

                // Large patterns
                patternImages = new int[] {R.drawable.pattern1, R.drawable.pattern2,
                        R.drawable.pattern3, R.drawable.pattern4, R.drawable.pattern5,
                        R.drawable.pattern6, R.drawable.a, R.drawable.h, R.drawable.k,
                        R.drawable.l, R.drawable.stanford1, R.drawable.stanford2, R.drawable.x
                };

                // Min and max pattern numbers
                MIN = 1;
                MAX = 6;

                onePlayer(null);
                break;
            case "Fixed":
                gameType = "fixed";
                onePlayer(null);
                break;
            case "2 Players":

                // All patterns
                patternImages = new int[] {R.drawable.pattern1, R.drawable.pattern7,
                        R.drawable.pattern13, R.drawable.pattern2, R.drawable.pattern8,
                        R.drawable.pattern14, R.drawable.pattern3, R.drawable.pattern9,
                        R.drawable.pattern15, R.drawable.pattern4, R.drawable.pattern10,
                        R.drawable.pattern16, R.drawable.pattern5, R.drawable.pattern11,
                        R.drawable.pattern17, R.drawable.pattern6, R.drawable.pattern12,
                        R.drawable.pattern18
                };

                // Min and max pattern numbers
                MIN = 1;
                MAX = 18;

                twoPlayers(null);
                break;

            case "Color: Level 1":
                gameType = "fixedColorOne";
                onePlayer(null);
                break;
            case "Color: Level 2":
                gameType = "fixedColorTwo";
                onePlayer(null);
                break;
            case "Color: Level 3":
                gameType = "fixedColorThree";
                onePlayer(null);
                break;
            case "2 Players (Color)":
                patternImages = new int[] {R.drawable.pattern30, R.drawable.pattern31,
                        R.drawable.pattern32, R.drawable.pattern33, R.drawable.pattern34,
                        R.drawable.pattern35, R.drawable.pattern36, R.drawable.pattern37,
                        R.drawable.pattern38, R.drawable.pattern39, R.drawable.pattern40,
                        R.drawable.pattern41, R.drawable.pattern42, R.drawable.pattern43,
                        R.drawable.pattern44, R.drawable.pattern45, R.drawable.pattern46,
                        R.drawable.pattern47, R.drawable.pattern48, R.drawable.pattern49,
                        R.drawable.pattern50, R.drawable.pattern51, R.drawable.pattern52,
                        R.drawable.pattern53, R.drawable.pattern54, R.drawable.pattern55,
                        R.drawable.pattern56, R.drawable.pattern57, R.drawable.pattern58,
                        R.drawable.pattern59, R.drawable.pattern60, R.drawable.pattern61,
                        R.drawable.pattern62
                };

                // Min and max pattern numbers
                MIN = 30;
                MAX = 62;

                twoPlayers(null);
                break;
            default:
                break;
        }
    }

    public void playerOneChoice(View view) {
        playerOnePattern = ((ImageButton) view).getDrawable();
        checkPlayersReady();
    }

    public void playerTwoChoice(View view) {
        playerTwoPattern = ((ImageButton) view).getDrawable();
        checkPlayersReady();
    }

    private void checkPlayersReady() {
        if (playerOnePattern != null && playerTwoPattern != null) {
            Button button = (Button) findViewById(R.id.start);
            button.setClickable(true);
        }
    }

    private void pauseTwoPlayer(View view) {
        /*if (view != null)
            view.setClickable(false);
        else {
            Button button = (Button) findViewById(R.id.stop);
            button.setClickable(false);
        }*/

        Button button = (Button) findViewById(R.id.randomize);
        button.setClickable(true);

        button = (Button) findViewById(R.id.start);
        button.setClickable(false);
        //button = (Button) findViewById(R.id.reset);
        //button.setClickable(true);

        playerOnePattern = null;
        playerTwoPattern = null;

        for (int i = 0; i < ONEPLAYEROPTIONS; i++) {
            String imgID = "option" + i;
            int resID = getResources().getIdentifier(imgID, "id", "com.example.lukas.euglenapatterns");
            ImageButton img = (ImageButton) findViewById(resID);
            img.setClickable(false);

            img.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    playerOneChoice(v);
                }
            });
        }

        for (int i = ONEPLAYEROPTIONS; i < TWOPLAYEROPTIONS; i++) {
            String imgID = "option" + i;
            int resID = getResources().getIdentifier(imgID, "id", "com.example.lukas.euglenapatterns");
            ImageButton img = (ImageButton) findViewById(resID);
            img.setClickable(false);

            img.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    playerTwoChoice(v);
                }
            });
        }
    }

    private void startPatternSwitch() {
        final PresentationService presentationService = (PresentationService) CastRemoteDisplayLocalService.getInstance();

        if (presentationService != null) {
            patternSwitchThread = new Thread() {
                public void run() {
                    while (!this.isInterrupted()) {
                        try {
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    presentationService.updatePattern(playerOnePattern);
                                }
                            });
                            Thread.sleep(500);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    presentationService.updatePattern(playerTwoPattern);
                                }
                            });
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            };

            patternSwitchThread.start();
        }
    }

    public void reset(View view) {
        pointsPlayerOne = 0;
        pointsPlayerTwo = 0;

        TextView text = (TextView) findViewById(R.id.points_player_one);
        text.setText("" + pointsPlayerOne);
        text = (TextView) findViewById(R.id.points_player_two);
        text.setText("" + pointsPlayerTwo);
    }

    @Override
    public void onBackPressed() {
        return;
    }

    public void help(View view) { return; }
}