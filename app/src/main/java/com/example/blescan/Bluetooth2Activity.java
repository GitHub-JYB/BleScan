package com.example.blescan;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

public class Bluetooth2Activity extends AppCompatActivity {

    private String TAG = "test";
    private BluetoothDevice targetDevice;
    private boolean isScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<String> permissionList = new ArrayList<String>();
    private BluetoothLeScanner bluetoothLeScanner;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private MyBluetoothAdapter adapter;
    private BluetoothBroadcastReceiver mBluetoothBroadcastReceiver;
    private int mFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        checkBluetooth();
        initRecycleView();
        initReceiver();


    }

    private void initReceiver() {
        mBluetoothBroadcastReceiver = new BluetoothBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getBaseContext().registerReceiver(mBluetoothBroadcastReceiver, filter);

    }

    private void checkBluetooth() {
        //????????????????????????????????????
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // ??????????????????
        if (mBluetoothAdapter == null) {
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(Bluetooth2Activity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                }
            }
            startActivityForResult(enableBtIntent, 1);
//             mBluetoothAdapter.enable();
        }
//        mBluetoothAdapter.startDiscovery();
//        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
    }


    // ?????????????????????????????????
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "???????????????", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    private void initRecycleView() {
        RecyclerView recyclerView = findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyBluetoothAdapter();
        adapter.setOnClickListener(new MyBluetoothAdapter.OnClickListener() {
            @Override
            public void onClick(int position) {
                bondAndConnect(deviceList.get(position));
            }
        });
        recyclerView.setAdapter(adapter);

    }

    private void checkPermission() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionList.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(Bluetooth2Activity.this, permissionList.toArray(new String[0]), 2);
        }
    }


    class BluetoothBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //????????????
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice scanDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(Bluetooth2Activity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(Bluetooth2Activity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                }
                if (scanDevice == null || scanDevice.getName() == null) {
                    return;
                }

                int btType = scanDevice.getType();
                if (btType == BluetoothDevice.DEVICE_TYPE_LE || btType == BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
                    return;
                }
                if (deviceList.isEmpty()) {
                    deviceList.add(scanDevice);
                } else {
                    for (int i = 0; i < deviceList.size(); i++) {
                        if (scanDevice.equals(deviceList.get(i))) {
                            deviceList.set(i, deviceList.get(i));
                            break;
                        }
                        if (i == deviceList.size() - 1) {
                            deviceList.add(scanDevice);
                        }
                    }

                }
                //???????????????????????????????????????
//                deviceList.add(scanDevice);

                short rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
//                rssiList.add(rssi);
                adapter.setData(deviceList);
            }

        }
    }

    public void startScan(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Bluetooth2Activity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
        }
        mBluetoothAdapter.startDiscovery();
    }

    /**
     * ?????????????????????
     */
    public void bondAndConnect(BluetoothDevice mCurDevice) {
        //????????????
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Bluetooth2Activity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
        }
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        if(mCurDevice==null){
            Log.i(TAG, "bondAndConnect: ???????????????????????????");
            return;
        }

        //????????????????????????????????????????????????
        if(mCurDevice.getBondState()==BluetoothDevice.BOND_NONE){
            Log.d(TAG,"create bond to "+mCurDevice.getName());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    //??????
                    Method method;
                    try {
                        method = BluetoothDevice.class.getMethod("createBond");
                        method.invoke(mCurDevice);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    Log.e(getPackageName(), "????????????");
                }
            }).start();


//            boolean nRet= true;
////                    BluetoothUtil.createBond(mCurDevice);
//            if(!nRet){
//                Log.i(TAG, "bondAndConnect: createBond fail???");
//                return;
//            }
//            mFlag=0;
//            while(mFlag==0){
//                SystemClock.sleep(250);
//            }
//            if(mFlag==-1){
//                Log.i(TAG, "bondAndConnect: "+mCurDevice.getName()+"?????????????????????");
//                return;
//            }
        }

//        if(mCurDevice.getBondState()==BluetoothDevice.BOND_BONDED){
//            try {
//                //??????Socket
//                BluetoothSocket socket = mCurDevice.createRfcommSocketToServiceRecord(GlobalDef.BT_UUID);
//                //???????????????????????????
//                socket.connect();
//                mThread=new SocketThread(socket);
//                mThread.start();
//                Log.i(TAG, "bondAndConnect: ????????????"+mCurDevice.getName()+"???????????????");
//            } catch (IOException e) {
//                Log.d(TAG,"socket connect fail");
//                Log.i(TAG, "bondAndConnect: ?????????"+mCurDevice.getName()+"?????????");
//                e.printStackTrace();
//            }
//        }
    }

    public void stopScan(View view) {}


}