package in.pradeepmeti.bluetoothtest;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public int BLUETOOTH_TURNED_OFF = 99;
    Button btOnBtn, btLocationBtn,btDataSendBtn;
    EditText btSendText;
    private ArrayAdapter<String> btDevicesArrayAdapter;
    BluetoothHandlerClass btHandler;
    ProgressDialog progress;

//    Map<String, String[]> btDeviceArray = new HashMap<String, String[]>();
    ArrayList<BluetoothDevice> btDeviceArray = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btOnBtn = (Button) findViewById(R.id.btOnBtn);
        btLocationBtn = (Button) findViewById(R.id.btLocationBtn);
        btDataSendBtn = (Button) findViewById(R.id.btDataSendBtn);
        btHandler = BluetoothHandlerClass.getInstance();
        btSendText = (EditText)findViewById(R.id.btSendText);

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Bluetooth is not supported in your device", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
            startBluetoothFunction();
        }

        btOnBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        });

        btDataSendBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                btSendData();
            }
        });

        btLocationBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                checkCoarseLocation();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            // If user clicks OK button in alert dialog
            if (resultCode == Activity.RESULT_OK) {
                btOnBtn.setVisibility(android.view.View.INVISIBLE);
                //Create a listener for bt disabled state.............
                Toast.makeText(getApplicationContext(), "Whoosh Bluetooth is Enabled!!!!", Toast.LENGTH_SHORT).show();
                startBluetoothFunction();
            }else if(resultCode == Activity.RESULT_CANCELED){
                btOnBtn.setVisibility(android.view.View.VISIBLE);
                Toast.makeText(getApplicationContext(), "Oh Bluetooth is disabled!!!!", Toast.LENGTH_SHORT).show();
            }else{
                btOnBtn.setVisibility(android.view.View.VISIBLE);
                Toast.makeText(getApplicationContext(), "Oh Unknown Error occurred!!!!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    protected void startBluetoothFunction(){

        IntentFilter btDisconnectFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btDisconnect, btDisconnectFilter);
        Set<BluetoothDevice> pairedDevicesList = getPairedDevices();

        if (pairedDevicesList.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevicesList) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                //Add Paired Devices to the list here...............
                Log.v("Device Name",deviceName);
                Log.v("MAC Address",deviceHardwareAddress);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                checkCoarseLocation();
            }else{
                startBTDeviceDiscovery();
            }
        }else{
            checkCoarseLocation();
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver btDisconnect = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    unregisterReceiver(btDisconnect);
                    Log.v("D3", "Activity Resart.");
                    finish();
                    Toast.makeText(getApplicationContext(), "Activity Resart.", Toast.LENGTH_SHORT).show();
                    Intent deviceListIntent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivityForResult(deviceListIntent, BLUETOOTH_TURNED_OFF);
                }
            }
        }
    };

    protected Set<BluetoothDevice> getPairedDevices(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        return pairedDevices;
    }

    protected void startBTDeviceDiscovery(){
        //if bt is in discovery mode then cancel it and start again........
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }

        Boolean startDiscovery = mBluetoothAdapter.startDiscovery();
        if(startDiscovery){
            Log.v("D1", "Device Discovery Started.");
            Toast.makeText(getApplicationContext(), "Device Discovery Started.", Toast.LENGTH_SHORT).show();

            btDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.btdevicenameview);
            // Find and set up the ListView for newly discovered devices
            ListView btDeviceListView = (ListView) findViewById(R.id.btDeviceListView);
            btDeviceListView.setAdapter(btDevicesArrayAdapter);
            btDeviceListView.setOnItemClickListener(btDeviceClickListner);

            // if discovery started then Register for broadcasts when a device is discovered.
            IntentFilter filter =  new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mReceiver, filter);
        }
    }


    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                Log.v("D2", "State Change");
                Toast.makeText(getApplicationContext(), "State Change", Toast.LENGTH_SHORT).show();
            }else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if(findViewById(R.id.btDeviceListView).getVisibility() == View.INVISIBLE){
                    findViewById(R.id.btDeviceListView).setVisibility(View.VISIBLE);
                }
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = String.valueOf(device.getName());
                String deviceHardwareAddress = device.getAddress(); // MAC address
                //check if the discovered device is listed if not add to the list.....
                if(deviceName.length() == 0){
                    deviceName = "Test";
                }
                if(deviceName == "null"){
                    deviceName = "Test";
                }

                btDeviceArray.add(device);
                btDevicesArrayAdapter.add(deviceName);
                btDevicesArrayAdapter.notifyDataSetChanged();

                //Log.v("MAC Address",deviceHardwareAddress);
                //Log.v("Device Name",deviceName);
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                deviceDiscoveryStopped();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(btHandler.btState() == Constants.bt_Connection_State_Connected || btHandler.btState() ==Constants.bt_Connection_State_Connecting){
            btHandler.btCloseAllConnection();
        }

        if(btHandler != null){
            btHandler = null;
        }
        btDeviceArray.clear();
        try {
            btDevicesArrayAdapter.clear();
        }catch (IllegalFormatException e){
            e.printStackTrace();
        }

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }
        // Don't forget to unregister the ACTION_FOUND receiver.
        try {
            if (mReceiver!=null) {
                unregisterReceiver(mReceiver);
                Log.v("mReceiver"," service unregistered");
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        try{
            if(btDisconnect != null){
                unregisterReceiver(btDisconnect);
                Log.v("btDisconnect"," service unregistered");
            }
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    protected void deviceDiscoveryStopped(){
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }
        Log.v("D2", "Device Discovery Finished.");
        Log.v("D2", btDeviceArray.toString());
        Toast.makeText(getApplicationContext(), "Device Discovery Finished.", Toast.LENGTH_SHORT).show();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }

    protected void checkCoarseLocation(){
        btLocationBtn.setVisibility(android.view.View.INVISIBLE);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startBTDeviceDiscovery();
        }else{
            btLocationBtn.setVisibility(android.view.View.VISIBLE);
            Log.v("D3", "Location Permission Denied.");
            Toast.makeText(getApplicationContext(), "Location Permission Denied.", Toast.LENGTH_SHORT).show();
        }

        return;
    }

    private AdapterView.OnItemClickListener btDeviceClickListner
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int itemPosition, long id) {
            Log.v("Device","test1");
            try {
                Log.v("Device","test3");
                if(mBluetoothAdapter.isDiscovering()){
                    mBluetoothAdapter.cancelDiscovery();
                }
                String selectedBtDeviceName = ((TextView) v).getText().toString();
//                selectedBtDeviceMACAddress = btDeviceArray.get(itemPosition);
//                Log.v("MAC Address", String.valueOf(selectedBtDeviceMACAddress));
                Log.v("Device",btDeviceArray.get(itemPosition).toString());
                btStartConnection(btDeviceArray.get(itemPosition));
            } catch (NullPointerException e) {
                Log.v("Device","test2");
                e.printStackTrace();
            }
        }
    };

    private void btStartConnection(BluetoothDevice device){
        btHandler.setBtListener(new BluetoothHandlerClass.BluetoothListener(){

            @Override
            public void btNoConnection() {
                Log.v("noConnection","noConnection");
            }

            @Override
            public void btOnConnecting() {
                showProgressDialog(true,"Connecting To Bluetooth Device");
                Log.v("btConnecting","Device");
            }

            @Override
            public void btOnConnected() {
                showProgressDialog(false,"");
                findViewById(R.id.btDeviceListView).setVisibility(View.INVISIBLE);
                findViewById(R.id.btDataSendBtn).setVisibility(View.VISIBLE);
                findViewById(R.id.btSendText).setVisibility(View.VISIBLE);
                Log.v("Connected","Connected");
            }

            @Override
            public void btOnConnectionFailed() {
                Log.v("ConnectionFailed","ConnectionFailed");
            }

            @Override
            public void btOnConnectionLost() {
                Log.v("ConnectionLost","ConnectionLost");
            }

            @Override
            public void btOnMsgReceived() {
                Log.v("MsgReceived","MsgReceived");
            }

            @Override
            public void btOnMsgWrite() {
                Log.v("MsgWritten","MsgWritten");
            }
        });
        btHandler.connectBtDevice(device);

    }

    private void btSendData(){
        String btDataToSend = btSendText.getText().toString();
        btHandler.btMsgWrite(btDataToSend);
        btSendText.setText("");
    }

    private void showProgressDialog(Boolean show, String msg){
        if(show){
            progress = new ProgressDialog(this);
            progress.setTitle("Please Wait ");
            progress.setMessage(msg);
            progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            progress.show();
        }else{
            progress.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("Close App")
                .setMessage("Are you sure you want to close the app?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        btHandler.btCloseAllConnection();
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
