package com.example.blescan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BluetoothActivity extends AppCompatActivity {

    private String TAG = "test";
    private BluetoothDevice targetDevice;
    private boolean isScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<String> permissionList = new ArrayList<String>();
    private BluetoothLeScanner bluetoothLeScanner;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private MyBluetoothAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        checkBluetooth();
        initRecycleView();


    }

    private void checkBluetooth() {
        //获取系统蓝牙适配器管理类
//        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
//        if (!mBluetoothAdapter.isEnabled()) {
//            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(intent, 1);
//        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 询问打开蓝牙
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(BluetoothActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                }
            }
            startActivityForResult(enableBtIntent, 1);
        }
    }


    // 申请打开蓝牙请求的回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "蓝牙已经开启", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "没有蓝牙权限", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "权限已获得", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "没有获得权限", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    private void initRecycleView() {
        RecyclerView recyclerView = findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyBluetoothAdapter();
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
            ActivityCompat.requestPermissions(BluetoothActivity.this, permissionList.toArray(new String[0]), 2);
        }
    }

    public void startScan(View view) {
        // 下面使用Android5.0新增的扫描API，扫描返回的结果更友好，比如BLE广播数据以前是byte[] scanRecord，而新API帮我们解析成ScanRecord类\
        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();


        // 扫描结果Callback
        ScanCallback mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                // 获取BLE设备信息
                BluetoothDevice dev = result.getDevice();
                if (ActivityCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(BluetoothActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                }
//                if (dev.getName() != null && dev.getName().startsWith("``")) {

                    if (!deviceList.isEmpty()) {
                        for (int i = 0; i < deviceList.size(); i++) {
                            if (dev.getAddress().equals(deviceList.get(i).getAddress())) {
                                deviceList.set(i, dev);
                                adapter.setData(deviceList);
                                break;
                            }
                            if (i >= deviceList.size() - 1) {
                                deviceList.add(dev);
                                adapter.setData(deviceList);
                            }
                        }
                    } else {
                        deviceList.add(dev);
                        adapter.setData(deviceList);
                   }
                    Log.i(TAG, "onScanResult: " + deviceList.size());
//                }

//                deviceList.add(dev);
//                dev.getAddress();
//                Log.i(TAG, "ult: " + dev);
//                // result.getScanRecord() 获取BLE广播数据
//                if (result.getScanRecord().getServiceData() != null & result.getScanRecord().getServiceUuids() != null) {
//                    byte[] uuid = result.getScanRecord().getServiceData().get(result.getScanRecord().getServiceUuids().get(0));
//                }
            }
        };

        BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {

            @Override
            public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                // 获取BLE设备信息
                if (ActivityCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(BluetoothActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    }
                }
//                if (bluetoothDevice.getName() != null && bluetoothDevice.getName().startsWith("``")){
                    if (!deviceList.isEmpty()){
                        for (int j = 0; j < deviceList.size(); j++){
                            if (bluetoothDevice.getAddress().equals(deviceList.get(j).getAddress())){
                                deviceList.set(j, bluetoothDevice);
                                adapter.setData(deviceList);
                                break;
                            }
                            if (j >= deviceList.size() -1){
                                deviceList.add(bluetoothDevice);
                                adapter.setData(deviceList);
                            }
                        }
                    }else {
                        deviceList.add(bluetoothDevice);
                        adapter.setData(deviceList);
                    }
//                }

                Log.i(TAG, "onScanResult: " + deviceList.size());
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(BluetoothActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
            }
        }
//        mBluetoothAdapter.startLeScan(leScanCallback);
        bluetoothLeScanner.startScan(mScanCallback);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(BluetoothActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
                    }
                }
                bluetoothLeScanner.stopScan(mScanCallback); //停止扫描
//                mBluetoothAdapter.stopLeScan(leScanCallback);
                isScanning = false;
            }
        }, 5000);
//        // 旧API是BluetoothAdapter.startLeScan(LeScanCallback callback)方式扫描BLE蓝牙设备，如下：
//        BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
//            @Override
//            public void onLeScan(BluetoothDevice device, int arg1, byte[] arg2) {
//                //device为扫描到的BLE设备
//                if (ActivityCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    // TODO: Consider calling
//                    //    ActivityCompat#requestPermissions
//                    // here to request the missing permissions, and then overriding
//                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                    //                                          int[] grantResults)
//                    // to handle the case where the user grants the permission. See the documentation
//                    // for ActivityCompat#requestPermissions for more details.
//                }
//                if (device.getName().equals("目标设备名称")) {
//                    //获取目标设备
//                    targetDevice = device;
//                }
//            }
//        };
//        mBluetoothAdapter.startLeScan(callback);


    }

    public void stopScan(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(BluetoothActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
            }
        }
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }
}