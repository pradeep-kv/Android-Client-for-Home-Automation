package in.pradeepmeti.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.provider.Settings.Global.DEVICE_NAME;



/**
 * Created by Pradeep.K.V on 8/25/2017.
 */

class BluetoothConnectionClass {

    private static final String TAG = "MY_APP_DEBUG_TAG";
//    private static final UUID bt_UUID =
//            UUID.fromString("ef3f4231-ec1c-495f-b65e-610f66e6f1a0");
    private static final UUID bt_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String DEVICE_ADDRESS = "testDevice";
    private final BluetoothAdapter mBluetoothAdapter;
    private ConnectThread btConnectThread;
    private ConnectedThread btConnectedThread;
    private Handler mHandler; // handler that gets info from Bluetooth service
    public int bt_Connection_State = Constants.bt_Connection_State_None;

    public BluetoothConnectionClass(Handler handler){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
    }

    public int getBtState(){ return bt_Connection_State;}

    public void setBtState(int state){
        bt_Connection_State = state;

        mHandler.obtainMessage(Constants.btStateChange,bt_Connection_State,-1).sendToTarget();
    }

    public void btConnect(BluetoothDevice device) {

        Log.v("btConnect","btConnect");
        if (btConnectThread != null) {
            btConnectThread.cancel();
            btConnectThread = null;
        }

        btConnectThread = new ConnectThread(device);
        btConnectThread.start();
        setBtState(Constants.bt_Connection_State_Connecting);
    }

    public void btConnected(BluetoothSocket socket,BluetoothDevice
            device) {

//        if (btConnectThread != null) {
//            btConnectThread.cancel();
//            btConnectThread = null;
//        }

        if (btConnectedThread != null) {
            btConnectedThread.cancel();
            btConnectedThread = null;
        }

        btConnectedThread = new ConnectedThread(socket);
        btConnectedThread.start();

        Message msg = mHandler.obtainMessage(Constants.btDeviceName);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        bundle.putString(DEVICE_ADDRESS, device.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setBtState(Constants.bt_Connection_State_Connected);
    }


    public void btSend(String data){
        if(bt_Connection_State != Constants.bt_Connection_State_Connected){return;}
        ConnectedThread r;
        r = btConnectedThread;
        r.write(data.getBytes());
    }

    public void killEverything(){
        if (btConnectThread != null) {
            btConnectThread.cancel();
            btConnectThread = null;
        }

        if (btConnectedThread != null) {
            btConnectedThread.cancel();
            btConnectedThread = null;
        }
        setBtState(Constants.bt_Connection_State_None);
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(bt_UUID);
                Log.v("connceted","bt socket connceted");
            } catch (IOException e) {
                Log.e("Error_tag", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("Error_tag", "Could not close the client socket", closeException);
                    Log.v("connceted","socket error bt didn't connceted");
                }
                setBtState(Constants.bt_Connection_State_Falied);
                killEverything();
                Log.v("connceted","bt didn't connceted");
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            Log.v("connceted","bt connceted");
            btConnected(mmSocket,mmDevice);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("Error_tag", "Could not close the client socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[256];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    Log.v("inputStream",mmBuffer.toString());
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(
                            Constants.MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    setBtState(Constants.bt_Connection_State_Lost);
                    killEverything();
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = mHandler.obtainMessage(
                        Constants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(Constants.bt_Data_Write_Falied);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
//    }
}
