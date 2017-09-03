package in.pradeepmeti.bluetoothtest;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by Pradeep.K.V on 8/29/2017.
 */

class BluetoothHandlerClass {
    private static BluetoothHandlerClass instance;
    BluetoothConnectionClass btConnectionThread;
    private static BluetoothListener btListener = null;

//    public BluetoothHandlerClass(BluetoothListener btListener){
//        btListener = btListener;
//    }
    public static BluetoothHandlerClass getInstance(){
        if (instance == null){
            instance = new BluetoothHandlerClass();
        }
        return instance;
    }

    public void connectBtDevice(BluetoothDevice device){
        btConnectionThread = new BluetoothConnectionClass(mHandler);
        btConnectionThread.btConnect(device);
    }

    public int btState(){return btConnectionThread.getBtState();}

    public void btMsgWrite(String data){
        if(btConnectionThread.getBtState() != Constants.bt_Connection_State_Connected) {
            return;
        }
        btConnectionThread.btSend(data);
    }

    public void btCloseAllConnection(){
        btConnectionThread.killEverything();
    }

    public void setBtListener(BluetoothListener listener){
        btListener = listener;
    }

    public interface BluetoothListener {
        public void btNoConnection();
        public void btOnConnecting();
        public void btOnConnected();
        public void btOnConnectionFailed();
        public void btOnConnectionLost();
        public void btOnMsgReceived();
        public void btOnMsgWrite();
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constants.MESSAGE_WRITE:
                    Log.v("msgWrite",msg.getData().toString());
//                    Log.v("msgWrite",String.valueOf(msg));
//                    Log.v("msgWrite",String.valueOf(msg.arg1));
//                    Log.v("msgWrite",String.valueOf(msg.arg2));
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Log.v("msgWrite",writeMessage);
                    btListener.btOnMsgWrite();
                    break;
                case Constants.MESSAGE_READ:
                    Log.v("msgRead",msg.getData().toString());
//                    Log.v("msgRead",String.valueOf(msg));
//                    Log.v("msgRead",String.valueOf(msg.arg1));
//                    Log.v("msgRead",String.valueOf(msg.arg2));
                    String readMsg = msg.obj.toString();
                    byte[] readBuf = readMsg.getBytes();
                    // construct a string from the valid bytes in the buffer
//                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.v("msgRead",readMsg);
                    btListener.btOnMsgReceived();
                    break;
                case Constants.MESSAGE_TOAST:
                    Log.v("receiveToastMsg",msg.getData().toString());
                    break;
                case Constants.btStateChange:
                    int btState = msg.arg1;
                    Log.v("btStateChange", String.valueOf(msg.arg1));
                    switch (btState){
                        case Constants.bt_Connection_State_None :
                            btListener.btNoConnection();
                            break;
                        case Constants.bt_Connection_State_Connecting :
                            btListener.btOnConnecting();
                            break;
                        case Constants.bt_Connection_State_Connected :
                            Log.v("btStateChangeWritten",msg.getData().toString());
                            btListener.btOnConnected();
                            break;
                        case Constants.bt_Connection_State_Falied :
                            btListener.btOnConnectionFailed();
                            break;
                        case Constants.bt_Connection_State_Lost :
                            btListener.btOnConnectionLost();
                            break;
                        default:
                            break;
                    }
                    break;
                case Constants.bt_Data_Write_Falied:
                    Log.v("bt_Data_Write_Failed",msg.getData().toString());
                    break;
                case Constants.btDeviceName:
                    Log.v("btDeviceName",msg.getData().toString());
                    break;
            }
        }
    };


}
