//Brodcast src: http://stackoverflow.com/questions/17082393/handlers-and-multiple-activities

package com.example.lukas.euglenapatterns;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class BluetoothThread extends Thread {

    // Bluetooth device
    private BluetoothDevice btShield;

    // Bluetooth socket that provides connection between device and Bluetooth device
    private BluetoothSocket socket = null;

    // Streams to write to and receive from Bluetooth device
    private OutputStream outStream = null;
    private InputStream inStream = null;

    //private Intent intent = new Intent("shutter");

    // Context used by LocalBroadcastManager
    private Context mContext;

    // Bluetooth device UUID
    private static final UUID ID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Used for debugging purposes
    private static final String TAG = "BluetoothThread";

    // Creates a BluetoothSocket for communication
    public BluetoothThread(BluetoothDevice device, Context context) {
        btShield = device;
        mContext = context;

        // Creates socket
        try {
            socket = btShield.createRfcommSocketToServiceRecord(ID);
        }
        catch (IOException createSocketException) {
            Log.e(TAG, "A problem occured while creating the socket", createSocketException);
        }

        // Registers receivers to receive communication from main thread
        registerReceivers();
    }

    @Override
    public void run() {

        // Connects to Bluetooth device through the socket. This will block until it succeeds or
        // throws an IOException
        try {
            socket.connect();
        }
        catch (IOException e) {

            //Failed to make a connection
            Log.e(TAG, "Failed to connect!", e);
            disconnect();
            return;
        }

        // Opens input and output streams
        try {
            outStream = socket.getOutputStream();
            inStream = socket.getInputStream();
        } catch (IOException e) {

            // Failed to open streams
            Log.e(TAG, "Failed to get I/O Stream");
            disconnect();
        }

//        Intent intentBTConnect = new Intent("connect");
//        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intentBTConnect);
        sendBroadcast("connect");

        //Intent intent = new Intent("shutter");

        // Loop continuously, writing data, until thread.interrupt() is called
        while (!this.isInterrupted()) {

            // Checks if app is ready to start writing to Bluetooth device
            synchronized (MainActivity.lock) {
                while (!MainActivity.ready) {
                    try {
                        MainActivity.lock.wait();
                    } catch (InterruptedException e) {
                        //blank
                    }
                }

                // Makes sure connection is still valid
                if ((inStream == null) || (outStream == null)) {
                    Log.e(TAG, "Lost bluetooth connection!");
                    break;
                }

                // Writes to servo to cover projector
                write("Z");

                // Sleeps for 900 milliseconds
                try {
                    //Thread.sleep(1200);
                    Thread.sleep(1200);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                sendBroadcast("shutter");

                //Sleeps for 100 milliseconds
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                // Writes to servo to uncover projector
                write("A");

                // Sleeps for 3 seconds
                try {
                    Thread.sleep(3700);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // If thread is interrupted, close connections
        disconnect();
    }

    // Cancels a connection and closes the socket
    public void disconnect() {
        if (inStream != null) {
            try {inStream.close();} catch (Exception e) {e.printStackTrace();}
        }
        if (outStream != null) {
            try {outStream.close();} catch (Exception e) {e.printStackTrace();}
        }
        if (socket != null) {
            try {socket.close();} catch (Exception e) {e.printStackTrace();}
        }

        // Unregisters receivers for main thread
        unregisterReceivers();

//        Intent intentBTDisconnect = new Intent("disconnect");
//        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intentBTDisconnect);
        sendBroadcast("disconnect");
    }

    // Sends data to remote device
    private void write(String s) {
        try {

            // Convert to bytes and write
            outStream.write(s.getBytes());
        } catch (Exception e) {
            Log.e(TAG, "Write failed!", e);
        }
    }

    // Sends broadcasts to main thread
    private void sendBroadcast(String message) {
        Intent intent = new Intent(message);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    // Registers receivers needed to receive updates from main thread
    private void registerReceivers() {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(open,
                new IntentFilter("open"));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(close,
                new IntentFilter("close"));
    }

    // Unregisters the receivers used to receive updates from the main thread
    private void unregisterReceivers() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(open);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(close);
    }

    // Callbacks to open and close shutter
    BroadcastReceiver open = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            write("A");
        }
    };
    BroadcastReceiver close = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            write("Z");
        }
    };
}
