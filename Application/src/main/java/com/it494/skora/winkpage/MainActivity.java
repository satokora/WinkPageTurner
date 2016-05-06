/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.it494.skora.winkpage;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
    protected PowerManager.WakeLock mWakeLock;


    private BluetoothWinkService mWinkService = null;
    private PdfRendererBasicFragment pdfFragment=null;

    public static final String FRAGMENT_PDF_RENDERER_BASIC = "pdf_renderer_basic";

    private static final String TAG = "BluetoothHost";
    private static final boolean D = true;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;

    private Button mStartServiceBtn;
    private Button mStopServiceBtn;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    BluetoothAdapter myBt;


    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothWinkService.STATE_CONNECTED:

                            Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                            pdfFragment.setDeviceNameToScreen(mConnectedDeviceName, true);
                            notifySettingsChanged();

                            break;
                        case BluetoothWinkService.STATE_CONNECTING:
                            //connectedDevices.setText(R.string.title_connecting);
                            Toast.makeText(getApplicationContext(), R.string.title_connecting, Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothWinkService.STATE_LISTEN:
                            //not detected

                        case BluetoothWinkService.STATE_NONE:
                            //connectedDevices.setText("not connected");
                            Toast.makeText(getApplicationContext(), "not connected", Toast.LENGTH_SHORT).show();
                            pdfFragment.setDeviceNameToScreen(getString(R.string.turnon), true);


                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    //String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Received");
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                    convertGestureToPageTurn(readMessage);
                    MainActivity.this.sendMessage("Received");
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void notifySettingsChanged()
    {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String nextPage_preference = sharedPreferences.getString("page_fwd_list", GestureMap.WINK);
        String prevPage_preference = sharedPreferences.getString("page_bkwd_list", GestureMap.SHAKE_LEFT);
//        String topPage_preference = sharedPreferences.getString("first_page_list", GestureMap.LOOKUP);
//        String lastPage_preference = sharedPreferences.getString("last_page_list", GestureMap.SHAKE_LEFT);

        //String settingString="SETTINGS NEXT " + nextPage_preference + " PREV " + prevPage_preference + " FIRST " + topPage_preference + " LAST " + lastPage_preference;
        String settingString="SETTINGS NEXT " + nextPage_preference + " PREV " + prevPage_preference;
        MainActivity.this.sendMessage(settingString);
        Log.e(TAG, settingString);
    }
    public void convertGestureToPageTurn(String gesture)
    {

        if(gesture.equals(GestureMap.getNextPageGesture()))
        {
            pdfFragment.goToNextPage();
        }
        else if (gesture.equals(GestureMap.getPrevPageGesture()))
        {
            pdfFragment.goToPrevPage();
        }
//        else if(gesture.equals(GestureMap.getTopPageGesture()))
//        {
//            pdfFragment.goToTopPage();
//        }
//        else if(gesture.equals(GestureMap.getLastPageGesture()))
//        {
//            pdfFragment.goToLastPage();
//        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_real);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        pdfFragment = new PdfRendererBasicFragment();
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, pdfFragment,
                            FRAGMENT_PDF_RENDERER_BASIC)
                    .commit();
        }


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String nextPage_preference = sharedPreferences.getString("page_fwd_list", "" + GestureMap.WINK);
        String prevPage_preference = sharedPreferences.getString("page_bkwd_list", ""+GestureMap.SHAKE_LEFT);
//        String topPage_preference = sharedPreferences.getString("first_page_list", ""+GestureMap.LOOKUP);
//        String lastPage_preference = sharedPreferences.getString("last_page_list", ""+GestureMap.NOD);

        GestureMap.setNextPageGesture(nextPage_preference);
        GestureMap.setPrevPageGesture(prevPage_preference);
//        GestureMap.setTopPageGesture(topPage_preference);
//        GestureMap.setLastPageGesture(lastPage_preference);

    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if(D) Log.e(TAG, "+ ON START +");


    }
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");


        if(mWinkService!=null && mWinkService.getState()==BluetoothWinkService.STATE_CONNECTED)
        {
            notifySettingsChanged();
        }

    }
    public void setupBluetooth() {
        Log.d(TAG, "setupChat()");

        myBt = BluetoothAdapter.getDefaultAdapter();

        if(myBt == null)
        {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();

        }
        else
        {

            if (!myBt.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            if (mWinkService == null)
            {
                // Initialize the BluetoothChatService to perform bluetooth connections
                mWinkService = new BluetoothWinkService(this, mHandler);

                // Initialize the buffer for outgoing messages
                mOutStringBuffer = new StringBuffer("");

                if (mWinkService.getState() == BluetoothWinkService.STATE_NONE) {
                    // Start the Bluetooth chat services
                    mWinkService.start();

                }



            }
        }


    }

    @Override
    public synchronized void onPause() {
        //this.mWakeLock.release();
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {

        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        // Stop the Bluetooth chat services
        //if (mWinkService != null) mWinkService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (myBt.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mWinkService.getState() != BluetoothWinkService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mWinkService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Got OK");
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    Log.e(TAG,"about to connect");
                    BluetoothDevice device = myBt.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mWinkService.connect(device);
                    Log.e(TAG, "connection sent");
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupBluetooth();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");

                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.settings:
                Intent serverIntent2 = new Intent(this, SettingsActivity.class);
                startActivityForResult(serverIntent2, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            case R.id.turnoff:

                if (mWinkService != null)
                {
                    sendMessage("END");
                    mWinkService.stop();
                    myBt.disable();
                }

                pdfFragment.setDeviceNameToScreen("",false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
