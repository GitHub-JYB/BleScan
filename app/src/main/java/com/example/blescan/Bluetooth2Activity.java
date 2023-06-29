package com.example.blescan;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
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
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.feasycom.feasymesh.library.MeshManagerApi;
import com.feasycom.feasymesh.library.MeshManagerCallbacks;
import com.feasycom.feasymesh.library.MeshNetwork;
import com.feasycom.feasymesh.library.MeshProvisioningStatusCallbacks;
import com.feasycom.feasymesh.library.MeshStatusCallbacks;
import com.feasycom.feasymesh.library.provisionerstates.ProvisioningState;
import com.feasycom.feasymesh.library.provisionerstates.UnprovisionedMeshNode;
import com.feasycom.feasymesh.library.transport.ControlMessage;
import com.feasycom.feasymesh.library.transport.MeshMessage;
import com.feasycom.feasymesh.library.transport.ProvisionedMeshNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Bluetooth2Activity extends AppCompatActivity implements View.OnClickListener {

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

    private BluetoothGatt bluetoothGatt;

    private HandlerThread handlerThread;
    private Handler handler;
    private UUID HT_SERVICE_UUID;
    private boolean isHTServiceFound = false;
    private boolean isBatteryServiceFound = false;

    private BluetoothGattService mHTService, mBatteryService;
    private UUID BATTERY_LEVEL_CHARACTERISTIC;
    private double tempValue;
    private int MESSAGE_UPDATE_BATTERY_LEVEL;
    private UUID HT_IMEDIATE_MEASUREMENT_CHARACTERISTIC_UUID;
    private int MESSAGE_UPDATE_TEMPERATURE;
    private UUID HT_MEASUREMENT_CHARACTERISTIC_UUID;
    private UUID HT_MEASUREMENT_INTERVAL_CHARACTERISTIC_UUID;
    private UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID;
    private MeshManagerApi mMeshManagerApi;

    private ArrayList<byte[]> uuidList = new ArrayList<>();


    public BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (ActivityCompat.checkSelfPermission(Bluetooth2Activity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                }
            } else if (status == 133) {

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (ActivityCompat.checkSelfPermission(Bluetooth2Activity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> serviceList = gatt.getServices();
                for (BluetoothGattService service : serviceList) {
                    Log.i("gattService", String.valueOf(service.getUuid()));
                    Log.i("gattService", String.valueOf(service.getCharacteristic(service.getUuid())));

                    if (service.getUuid().equals(HT_SERVICE_UUID)) {
                        isHTServiceFound = true;
                        mHTService = service;
                    }
                    if (service.getUuid().equals(BATTERY_SERVICE)) {
                        isBatteryServiceFound = true;
                        mBatteryService = service;
                    }
                }
                if (!isHTServiceFound) {
                    gatt.disconnect();
                }
                if (isBatteryServiceFound) {
                    readBatteryLevel();
                }
            }
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getUuid().equals(BATTERY_LEVEL_CHARACTERISTIC)) {
                    int batteryValue = characteristic.getValue()[0];
                    Message message = Message.obtain();
                    message.arg1 = batteryValue;
                    message.what = MESSAGE_UPDATE_BATTERY_LEVEL;
                    handler.sendMessage(message);
                    if (isHTServiceFound) {
                        enableHTIndication();
                        ChangeHTP_Interval();
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            if ((characteristic.getUuid().equals(HT_IMEDIATE_MEASUREMENT_CHARACTERISTIC_UUID)) || (characteristic.getUuid().equals(HT_MEASUREMENT_CHARACTERISTIC_UUID))) {
                try {
//                    tempValue = decodeTemperature(characteristic.getValue());
                    Message message = Message.obtain();
                    Bundle bundle = new Bundle();
                    bundle.putDouble("temperature", tempValue);
                    message.setData(bundle);
                    message.what = MESSAGE_UPDATE_TEMPERATURE;
                    handler.sendMessage(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceChanged(@NonNull BluetoothGatt gatt) {
            super.onServiceChanged(gatt);
        }

        private void ChangeHTP_Interval() {
            if (ActivityCompat.checkSelfPermission(Bluetooth2Activity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            final byte[] interval_val = {0x0a, 0x00};
            BluetoothGattCharacteristic mHTServiceCharacteristic = mHTService.getCharacteristic(HT_MEASUREMENT_INTERVAL_CHARACTERISTIC_UUID);
            mHTServiceCharacteristic.setValue(interval_val);
            bluetoothGatt.writeCharacteristic(mHTServiceCharacteristic);
        }

        private void enableHTIndication() {
            if (ActivityCompat.checkSelfPermission(Bluetooth2Activity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            BluetoothGattCharacteristic mHTServiceCharacteristic = mHTService.getCharacteristic(HT_MEASUREMENT_CHARACTERISTIC_UUID);
            bluetoothGatt.setCharacteristicNotification(mHTServiceCharacteristic, true);
            BluetoothGattDescriptor descriptor = mHTServiceCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }

        private void readBatteryLevel() {
            if (ActivityCompat.checkSelfPermission(Bluetooth2Activity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            BluetoothGattCharacteristic mBatteryCharacteristic = mBatteryService.getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC);
            if (mBatteryCharacteristic != null) {
                bluetoothGatt.readCharacteristic(mBatteryCharacteristic);
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        checkBluetooth();
        intBtn();
        initRecycleView();
        initReceiver();
        handlerThread = new HandlerThread("test");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        mMeshManagerApi = new MeshManagerApi(this);
        mMeshManagerApi.setMeshManagerCallbacks(new MeshManagerCallbacks() {
            @Override
            public void onNetworkLoaded(MeshNetwork meshNetwork) {

            }

            @Override
            public void onNetworkUpdated(MeshNetwork meshNetwork) {

            }

            @Override
            public void onNetworkLoadFailed(String s) {

            }

            @Override
            public void onNetworkImported(MeshNetwork meshNetwork) {

            }

            @Override
            public void onNetworkImportFailed(String s) {

            }

            @Override
            public void sendProvisioningPdu(UnprovisionedMeshNode unprovisionedMeshNode, byte[] bytes) {

            }

            @Override
            public void onMeshPduCreated(byte[] bytes) {

            }

            @Override
            public int getMtu() {
                return 0;
            }
        });
        mMeshManagerApi.setProvisioningStatusCallbacks(new MeshProvisioningStatusCallbacks() {
            @Override
            public void onProvisioningStateChanged(UnprovisionedMeshNode unprovisionedMeshNode, ProvisioningState.States states, byte[] bytes) {

            }

            @Override
            public void onProvisioningFailed(UnprovisionedMeshNode unprovisionedMeshNode, ProvisioningState.States states, byte[] bytes) {

            }

            @Override
            public void onProvisioningCompleted(ProvisionedMeshNode provisionedMeshNode, ProvisioningState.States states, byte[] bytes) {

            }
        });
        mMeshManagerApi.setMeshStatusCallbacks(new MeshStatusCallbacks() {
            @Override
            public void onTransactionFailed(int i, boolean b) {

            }

            @Override
            public void onUnknownPduReceived(int i, byte[] bytes) {

            }

            @Override
            public void onBlockAcknowledgementProcessed(int i, @NonNull ControlMessage controlMessage) {

            }

            @Override
            public void onBlockAcknowledgementReceived(int i, @NonNull ControlMessage controlMessage) {

            }

            @Override
            public void onMeshMessageProcessed(int i, @NonNull MeshMessage meshMessage) {

            }

            @Override
            public void onMeshMessageReceived(int i, @NonNull MeshMessage meshMessage) {

            }

            @Override
            public void onMessageDecryptionFailed(String s, String s1) {

            }
        });
        mMeshManagerApi.loadMeshNetwork();
    }

    public void test(int position) {
        BluetoothDevice bluetoothDevice = deviceList.get(position);
        byte[] bytes = uuidList.get(position);
        StringBuffer buffer = new StringBuffer();
        for (int i=0; i < 16; i++){
            String s = String.format("%02x", bytes[i] & 0xFF);
            buffer.append(s);
            if (i == 3 || i == 5 || i == 7 || i == 9){
                buffer.append("-");
            }
        }
        Log.i(TAG, "test: " + buffer);
        UUID uuid = UUID.fromString(buffer.toString());
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            mMeshManagerApi.identifyNode(uuid);
            mMeshManagerApi.startProvisioning(new UnprovisionedMeshNode(uuid));
        }catch (Exception e){
            Log.e(TAG, "test: " +e.getMessage());
            e.printStackTrace();
        }

//        final GenericOnOffSet genericOnOffSet = new GenericOnOffSet(appKey, state,
//                new Random().nextInt(), transitionSteps, transitionStepResolution, delay);
//        mMeshManagerApi.createMeshPdu(address, genericOnOffSet);
    }

    private void intBtn() {
        Button start = findViewById(R.id.start);
        start.setOnClickListener(this);
        Button stop = findViewById(R.id.stop);
        start.setOnClickListener(this);
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
        //获取系统蓝牙适配器管理类
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 询问打开蓝牙
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
        adapter.setOnClickListener(new MyBluetoothAdapter.OnClickListener() {
            @Override
            public void onClick(int position) {
                test(position);
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start:
                startScan();
                break;
            case R.id.stop:
                stopScan();
                break;
        }
    }


    class BluetoothBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //蓝牙搜索
            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice scanDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.e("TAG", scanDevice.getAddress());
                if (ActivityCompat.checkSelfPermission(Bluetooth2Activity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(Bluetooth2Activity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                }
                if (scanDevice == null || scanDevice.getName() == null) {
                    return;
                }

                int btType = scanDevice.getType();
                if (btType == BluetoothDevice.DEVICE_TYPE_LE || btType == BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
//                    return;
                }
                if (scanDevice.getUuids() == null) {
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
                //将搜索到的蓝牙设备加入列表
//                deviceList.add(scanDevice);

                short rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
//                rssiList.add(rssi);
                adapter.setData(deviceList);
            }

        }
    }

    public void startScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Bluetooth2Activity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
        }
//        stopScan();
//        mBluetoothAdapter.startDiscovery();
        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        scanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if (result.getScanRecord().getServiceData() != null & result.getScanRecord().getServiceUuids() != null) {

                    byte[] uuid = result.getScanRecord().getServiceData().get(result.getScanRecord().getServiceUuids().get(0));
                    if ((uuid != null ? uuid.length : 0) >= 16) {
                        BluetoothDevice scanDevice = result.getDevice();
                        if (deviceList.isEmpty()) {
                            deviceList.add(scanDevice);
                            uuidList.add(uuid);
                        } else {
                            for (int i = 0; i < deviceList.size(); i++) {
                                if (scanDevice.equals(deviceList.get(i))) {
                                    deviceList.set(i, deviceList.get(i));
                                    uuidList.set(i, uuid);
                                    break;
                                }
                                if (i == deviceList.size() - 1) {
                                    deviceList.add(scanDevice);
                                    uuidList.add(uuid);
                                }
                            }

                        }
                        adapter.setData(deviceList);
                    }
                }
            }
        });
    }

    /**
     * 蓝牙配对并连接
     */
    public void bondAndConnect(BluetoothDevice mCurDevice) {


        //取消搜索
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Bluetooth2Activity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
        }

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        if (mCurDevice == null) {
            Log.i(TAG, "bondAndConnect: 远程蓝牙设备为空！");
            return;
        }

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mCurDevice.getAddress());
        if (device == null) {
            return;
        }
        bluetoothGatt = device.connectGatt(getBaseContext(), false, mGattCallback);

        //当前蓝牙设备未配对，则先进行配对
//        if (mCurDevice.getBondState() == BluetoothDevice.BOND_NONE) {
//            Log.d(TAG, "create bond to " + mCurDevice.getName());
//
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    //配对
//                    Method method;
//                    try {
//                        method = BluetoothDevice.class.getMethod("createBond");
//                        method.invoke(mCurDevice);
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                    Log.e(getPackageName(), "开始配对");
//                }
//            }).start();
//
//
////            boolean nRet= true;
//////                    BluetoothUtil.createBond(mCurDevice);
////            if(!nRet){
////                Log.i(TAG, "bondAndConnect: createBond fail！");
////                return;
////            }
////            mFlag=0;
////            while(mFlag==0){
////                SystemClock.sleep(250);
////            }
////            if(mFlag==-1){
////                Log.i(TAG, "bondAndConnect: "+mCurDevice.getName()+"的蓝牙配对失败");
////                return;
////            }
//        }

//        if(mCurDevice.getBondState()==BluetoothDevice.BOND_BONDED){
//            try {
//                //创建Socket
//                BluetoothSocket socket = mCurDevice.createRfcommSocketToServiceRecord(GlobalDef.BT_UUID);
//                //连接蓝牙服务套接字
//                socket.connect();
//                mThread=new SocketThread(socket);
//                mThread.start();
//                Log.i(TAG, "bondAndConnect: 成功与【"+mCurDevice.getName()+"】建立连接");
//            } catch (IOException e) {
//                Log.d(TAG,"socket connect fail");
//                Log.i(TAG, "bondAndConnect: 连接【"+mCurDevice.getName()+"】失败");
//                e.printStackTrace();
//            }
//        }
    }


    public void stopScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }


}