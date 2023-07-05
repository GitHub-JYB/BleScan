package com.example.blescan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "test";
    private UUID uuid = UUID.fromString("0003cdd0-0000-1000-8000-00805f9b0131");
    private boolean isScanning = false;
    private ArrayList<String> permissionList = new ArrayList<String>();
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private BluetoothLeScanner scanner;
    private ScanCallback mScanCallback;
    private Handler handler;
    private Runnable runnable;
    private MyBluetoothAdapter1 bluetoothAdapter;
    private BluetoothAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        initBtn();
        initRecycleView();
        initBle();

    }

    private void initBtn() {
        Button start = findViewById(R.id.start);
        start.setOnClickListener(this);
        Button stop = findViewById(R.id.stop);
        stop.setOnClickListener(this);

    }

    private void initBle() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                isScanning = false;
                stopScan();
            }
        };
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bluetoothManager.getAdapter();
        scanner = adapter.getBluetoothLeScanner();
        // 扫描结果Callback
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                // 获取BLE设备信息
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord != null) {
                    if (scanRecord.getBytes() != null) {
                        BluetoothDevice dev = result.getDevice();
                        int index = -1;
                        if (!deviceList.isEmpty()) {
                            for (int i = 0; i < deviceList.size(); i++) {
                                if (dev.getAddress().equals(deviceList.get(i).getAddress())) {
                                    deviceList.set(i, dev);
                                    index = i;
                                    break;
                                }
                                if (i >= deviceList.size() - 1) {
                                    deviceList.add(dev);
                                }
                            }
                        } else {
                            deviceList.add(dev);
                        }
                        bluetoothAdapter.setData(deviceList, index);
                    }
                }
            }
        };
    }


    private void initRecycleView() {
        RecyclerView recyclerView = findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        bluetoothAdapter = new MyBluetoothAdapter1();
        recyclerView.setAdapter(bluetoothAdapter);
        bluetoothAdapter.setOnClickListener(new MyBluetoothAdapter1.OnClickListener() {
            @Override
            public void onClick(int position) {
                checkPermission();
                BluetoothDevice device = adapter.getRemoteDevice(deviceList.get(position).getAddress());
                try {
                    Method createBond = device.getClass().getMethod("createBond");
                    Boolean invoke = (Boolean)createBond.invoke(device);
                    Log.i(TAG, "onClick: " + invoke.booleanValue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    device.connectGatt(BluetoothActivity.this, false, new BluetoothGattCallback() {
//                        @Override
//                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//                            super.onConnectionStateChange(gatt, status, newState);
//                            if (status == BluetoothGatt.GATT_SUCCESS){
//                                if (newState == BluetoothProfile.STATE_CONNECTED){
//                                    handler.post(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            checkPermission();
//                                            gatt.discoverServices();
//
//                                        }
//                                    });
//                                }
//                            }
//                        }
//
//                        @Override
//                        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//                            super.onServicesDiscovered(gatt, status);
//                            checkPermission();
//                            BluetoothGattService gattService = gatt.getService(uuid);
//                            if (gattService != null){
//                                BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(uuid);
//                                if (characteristic != null){
//                                    characteristic.setValue(new byte[]{1, 2, 3, 4});
//                                    gatt.writeCharacteristic(characteristic);
//                                    gatt.readCharacteristic(characteristic);
//                                }
//                            }
//                        }
//
//                        @Override
//                        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
//                            super.onCharacteristicRead(gatt, characteristic, value, status);
//                            if (status == BluetoothGatt.GATT_SUCCESS){
//                                Log.i(TAG, "onCharacteristicRead: " + characteristic.getValue());
//                            }
//                        }
//
//                        @Override
//                        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//                            super.onCharacteristicWrite(gatt, characteristic, status);
//                            if (status == BluetoothGatt.GATT_SUCCESS){
//                                Log.i(TAG, "onCharacteristicWrite: " + characteristic.getValue());
//                            }
//                        }
//                    }, BluetoothDevice.DEVICE_TYPE_LE);
//                }
            }
        });

    }

    private void checkPermission() {
        permissionList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
        }


        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(BluetoothActivity.this, permissionList.toArray(new String[0]), 2);
        }
    }


    public void startScan() {
        // 下面使用Android5.0新增的扫描API，扫描返回的结果更友好，比如BLE广播数据以前是byte[] scanRecord，而新API帮我们解析成ScanRecord类\
        checkPermission();
        if (!isScanning) {
            isScanning = true;
            scanner.startScan(mScanCallback);
            handler.postDelayed(runnable, 20000);
        }
    }

    public void stopScan() {
        checkPermission();
        if (isScanning) {
            handler.removeCallbacks(runnable);
            scanner.startScan(mScanCallback);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.start:
                startScan();
                break;
            case R.id.stop:
                stopScan();
                break;
        }
    }
}