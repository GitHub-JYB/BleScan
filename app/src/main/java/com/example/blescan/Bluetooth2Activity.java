package com.example.blescan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
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
import androidx.collection.ArraySet;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.feasycom.feasymesh.library.MeshBeacon;
import com.feasycom.feasymesh.library.MeshManagerApi;
import com.feasycom.feasymesh.library.MeshManagerCallbacks;
import com.feasycom.feasymesh.library.MeshNetwork;
import com.feasycom.feasymesh.library.MeshProvisioningStatusCallbacks;
import com.feasycom.feasymesh.library.MeshStatusCallbacks;
import com.feasycom.feasymesh.library.UnprovisionedBeacon;
import com.feasycom.feasymesh.library.provisionerstates.ProvisioningState;
import com.feasycom.feasymesh.library.provisionerstates.UnprovisionedMeshNode;
import com.feasycom.feasymesh.library.transport.ControlMessage;
import com.feasycom.feasymesh.library.transport.Element;
import com.feasycom.feasymesh.library.transport.MeshMessage;
import com.feasycom.feasymesh.library.transport.ProvisionedMeshNode;
import com.feasycom.feasymesh.library.utils.CompanyIdentifiers;
import com.feasycom.feasymesh.library.utils.MeshParserUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class Bluetooth2Activity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "test";
    private ArrayList<String> permissionList = new ArrayList<String>();
    private MyBluetoothAdapter adapter;

    private HandlerThread handlerThread;
    private Handler handler;

    private MeshManagerApi mMeshManagerApi;


    private String mNetworkId;
    private BleMeshManager mBleMeshManager;
    private MeshNetwork network;
    private ProvisionedMeshNode phoneNode;
    private Runnable runnable;
    private NrfMeshRepository nrfMeshRepository;

    private void showList(ScanResult result) {
        if (result.getScanRecord().getServiceData() != null & result.getScanRecord().getServiceUuids() != null) {

            byte[] uuid = result.getScanRecord().getServiceData().get(result.getScanRecord().getServiceUuids().get(0));
            if ((uuid != null ? uuid.length : 0) >= 16) {
                BluetoothDevice scanDevice = result.getDevice();
//                if (deviceList.isEmpty()) {
//                    deviceList.add(scanDevice);
//                    uuidList.add(uuid);
//                } else {
//                    for (int i = 0; i < deviceList.size(); i++) {
//                        if (scanDevice.equals(deviceList.get(i))) {
//                            deviceList.set(i, deviceList.get(i));
//                            uuidList.set(i, uuid);
//                            break;
//                        }
//                        if (i == deviceList.size() - 1) {
//                            deviceList.add(scanDevice);
//                            uuidList.add(uuid);
//                        }
//                    }
//
//                }
//                adapter.setData(deviceList);
            }
        }
    }

    private ArrayList<ScanResult> resultList = new ArrayList<>();
    private ArrayList<ExtendedBluetoothDevice> mDevices = new ArrayList<>();
    private final ScanCallback mScanCallbacks = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, @NonNull final ScanResult result) {

            final ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord != null) {
                if (scanRecord.getBytes() != null) {
                    final byte[] beaconData = mMeshManagerApi.getMeshBeaconData(scanRecord.getBytes());
                    if (beaconData != null) {
                        if (mMeshManagerApi.getMeshBeacon(beaconData) != null) {
                            ExtendedBluetoothDevice device = new ExtendedBluetoothDevice(result, mMeshManagerApi.getMeshBeacon(beaconData));
                            if (mDevices.isEmpty()) {
                                mDevices.add(device);
                            } else {
                                for (int i = 0; i < mDevices.size(); i++) {
                                    if (device.getAddress().equals(mDevices.get(i).getAddress())) {
                                        if (device.getName() == null) {
                                            device.setName(mDevices.get(i).getName());
                                        }
                                        mDevices.set(i, device);
                                        break;
                                    }
                                    if (i == mDevices.size() - 1) {
                                        mDevices.add(device);
                                    }

                                }

                            }
                            adapter.setData(mDevices);
                        }
                    }
                }
            }


            // 连接的设备
//            ExtendedBluetoothDevice device = mDevices.get(0);
//            stopScan();
//            nrfMeshRepository.connect(getBaseContext(), device, true);


//            try {
//                if (mFilterUuid.equals(BleMeshManager.MESH_PROVISIONING_UUID)) {
//                    if (result.getScanRecord() != null){
//                        if (result.getScanRecord().getServiceUuids() != null){
//                            if(result.getScanRecord().getServiceUuids().get(0).toString().equals(BleMeshManager.MESH_PROVISIONING_UUID.toString())){
//                                if (!meshProvisioningAddress.contains(result.getDevice().getAddress())){
//                                    meshProvisioningAddress.add(result.getDevice().getAddress());
//                                }
//                                updateScannerLiveData(result);
//                            }
//                        }else {
//                            if (meshProvisioningAddress.contains(result.getDevice().getAddress())){
//                                updateScannerLiveData(result);
//                            }
//                        }
//                    }
//                }
//                if (mFilterUuid.equals(BleMeshManager.MESH_PROXY_UUID)) {
//                final byte[] serviceData = Utils.getServiceData(result, BleMeshManager.MESH_PROXY_UUID);
//                if (serviceData == null) {
//                    return;
//                }
//
//                if (mMeshManagerApi != null) {
//                    if (mMeshManagerApi.isAdvertisingWithNetworkIdentity(serviceData)) {
//                        if (mMeshManagerApi.networkIdMatches(mNetworkId, serviceData)) {
//                            Log.i(TAG, "onScanResult: " + mNetworkId);
//                            updateScannerLiveData(result);
//                        }
//                    } else if (mMeshManagerApi.isAdvertisedWithNodeIdentity(serviceData)) {
//                        if (checkIfNodeIdentityMatches(serviceData)) {
//                            updateScannerLiveData(result);
//                        }
//                    }
//                }
//                }
//            } catch (Exception ex) {
//                Log.i(TAG, "Error1: " + ex.getMessage());
//            }
        }

        @Override
        public void onBatchScanResults(@NonNull final List<ScanResult> results) {
            // Batch scan is disabled (report delay = 0)
        }

        @Override
        public void onScanFailed(final int errorCode) {
//            mScannerStateLiveData.scanningStopped();

        }
    };
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        intBtn();
        initRecycleView();
        handlerThread = new HandlerThread("test");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        mMeshManagerApi = new MeshManagerApi(this);

        mBleMeshManager = new BleMeshManager(this);
//        mBleMeshManager.setGattCallbacks(new BleMeshManagerCallbacks() {
//            @Override
//            public void onDataReceived(BluetoothDevice bluetoothDevice, int mtu, byte[] pdu) {
//                Log.i(TAG, "onDataReceived: ");
//            }
//
//            @Override
//            public void onDataSent(BluetoothDevice device, int mtu, byte[] pdu) {
//                Log.i(TAG, "onDataSent: ");
//            }
//
//            @Override
//            public void onDeviceConnecting(@NonNull BluetoothDevice bluetoothDevice) {
//                Log.i(TAG, "onDeviceConnecting: ");
//            }
//
//            @Override
//            public void onDeviceConnected(@NonNull BluetoothDevice bluetoothDevice) {
//                Log.i(TAG, "onDeviceConnected: ");
//            }
//
//            @Override
//            public void onDeviceDisconnecting(@NonNull BluetoothDevice bluetoothDevice) {
//                Log.i(TAG, "onDeviceDisconnecting: ");
//            }
//
//            @Override
//            public void onDeviceDisconnected(@NonNull BluetoothDevice bluetoothDevice) {
//                Log.i(TAG, "onDeviceDisconnected: ");
//
//            }
//
//            @Override
//            public void onLinkLossOccurred(@NonNull BluetoothDevice bluetoothDevice) {
//                Log.i(TAG, "onLinkLossOccurred: ");
//            }
//
//            @Override
//            public void onServicesDiscovered(@NonNull BluetoothDevice bluetoothDevice, boolean b) {
//                Log.i(TAG, "onServicesDiscovered: ");
//            }
//
//            @Override
//            public void onDeviceReady(@NonNull BluetoothDevice bluetoothDevice) {
//                Log.i(TAG, "onDeviceReady: ");
//                checkPermission();
////                if (bluetoothDevice.getUuids() != null) {
////                    mMeshManagerApi.identifyNode(bluetoothDevice.getUuids());
////                }
////                    UnprovisionedBeacon beacon = (UnprovisionedBeacon)bluetoothDevice.getBeacon();
////                    if (beacon != null){
////                        mMeshManagerApi.identifyNode(beacon.getUuid());
////                    }else {
////                        final byte[] serviceData = Utils.getServiceData(bluetoothDevice).getScanResult(), BleMeshManager.MESH_PROVISIONING_UUID);
////                        if (serviceData != null) {
////                            final UUID uuid = mMeshManagerApi.getDeviceUuid(serviceData);
////                            mMeshManagerApi.identifyNode(uuid);
////                        }
////                    }
//            }
//
//            @Override
//            public void onBondingRequired(@NonNull BluetoothDevice bluetoothDevice) {
//                Log.i(TAG, "onBondingRequired: ");
//            }
//
//            @Override
//            public void onBonded(@NonNull BluetoothDevice bluetoothDevice) {
//                Log.i(TAG, "onBonded: ");
//            }
//
//            @Override
//            public void onBondingFailed(@NonNull BluetoothDevice bluetoothDevice) {
//                Log.i(TAG, "onBondingFailed: ");
//            }
//
//            @Override
//            public void onError(@NonNull BluetoothDevice bluetoothDevice, @NonNull String s, int i) {
//                Log.i(TAG, "onError: ");
//            }
//
//            @Override
//            public void onDeviceNotSupported(@NonNull BluetoothDevice bluetoothDevice) {
//                Log.i(TAG, "onDeviceNotSupported: ");
//            }
//
//            @Override
//            public void onAddress(String s) {
//                Log.i(TAG, "onAddress: ");
//            }
//
//            @Override
//            public void onIvIndex(int i, boolean b) {
//                Log.i(TAG, "onIvIndex: ");
//            }
//        });
        nrfMeshRepository = new NrfMeshRepository(this, mMeshManagerApi, mBleMeshManager);
//        mMeshManagerApi.setMeshManagerCallbacks(new MeshManagerCallbacks() {
//            @Override
//            public void onNetworkLoaded(MeshNetwork meshNetwork) {
//                Log.i(TAG, "onNetworkLoaded: ");
//            }
//
//            @Override
//            public void onNetworkUpdated(MeshNetwork meshNetwork) {
//                Log.i(TAG, "onNetworkUpdated: ");
//            }
//
//            @Override
//            public void onNetworkLoadFailed(String s) {
//                Log.i(TAG, "onNetworkLoadFailed: ");
//            }
//
//            @Override
//            public void onNetworkImported(MeshNetwork meshNetwork) {
//                Log.i(TAG, "onNetworkImported: ");
//            }
//
//            @Override
//            public void onNetworkImportFailed(String s) {
//                Log.i(TAG, "onNetworkImportFailed: ");
//            }
//
//            @Override
//            public void sendProvisioningPdu(UnprovisionedMeshNode unprovisionedMeshNode, byte[] bytes) {
//                Log.i(TAG, "sendProvisioningPdu: ");
//            }
//
//            @Override
//            public void onMeshPduCreated(byte[] bytes) {
//                Log.i(TAG, "onMeshPduCreated: ");
//            }
//
//            @Override
//            public int getMtu() {
//                return 0;
//            }
//        });
//        mMeshManagerApi.setProvisioningStatusCallbacks(new MeshProvisioningStatusCallbacks() {
//            @Override
//            public void onProvisioningStateChanged(UnprovisionedMeshNode unprovisionedMeshNode, ProvisioningState.States states, byte[] bytes) {
//                Log.i(TAG, "onProvisioningStateChanged: ");
//                mMeshManagerApi.startProvisioningWithStaticOOB(unprovisionedMeshNode);
//            }
//
//            @Override
//            public void onProvisioningFailed(UnprovisionedMeshNode unprovisionedMeshNode, ProvisioningState.States states, byte[] bytes) {
//                Log.i(TAG, "onProvisioningFailed: ");
//            }
//
//            @Override
//            public void onProvisioningCompleted(ProvisionedMeshNode provisionedMeshNode, ProvisioningState.States states, byte[] bytes) {
//                Log.i(TAG, "onProvisioningCompleted: ");
//
//            }
//        });
//        mMeshManagerApi.setMeshStatusCallbacks(new MeshStatusCallbacks() {
//            @Override
//            public void onTransactionFailed(int i, boolean b) {
//                Log.i(TAG, "onTransactionFailed: ");
//            }
//
//            @Override
//            public void onUnknownPduReceived(int i, byte[] bytes) {
//                Log.i(TAG, "onUnknownPduReceived: ");
//            }
//
//            @Override
//            public void onBlockAcknowledgementProcessed(int i, @NonNull ControlMessage controlMessage) {
//                Log.i(TAG, "onBlockAcknowledgementProcessed: ");
//            }
//
//            @Override
//            public void onBlockAcknowledgementReceived(int i, @NonNull ControlMessage controlMessage) {
//                Log.i(TAG, "onBlockAcknowledgementReceived: ");
//            }
//
//            @Override
//            public void onMeshMessageProcessed(int i, @NonNull MeshMessage meshMessage) {
//                Log.i(TAG, "onMeshMessageProcessed: ");
//            }
//
//            @Override
//            public void onMeshMessageReceived(int i, @NonNull MeshMessage meshMessage) {
//                Log.i(TAG, "onMeshMessageReceived: ");
//            }
//
//            @Override
//            public void onMessageDecryptionFailed(String s, String s1) {
//                Log.i(TAG, "onMessageDecryptionFailed: ");
//            }
//        });
//        mMeshManagerApi.loadMeshNetwork();
    }

    private void intBtn() {
        Button start = findViewById(R.id.start);
        start.setOnClickListener(this);
        Button stop = findViewById(R.id.stop);
        start.setOnClickListener(this);
    }

    private void initRecycleView() {
        RecyclerView recyclerView = findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyBluetoothAdapter();
        adapter.setOnClickListener(new MyBluetoothAdapter.OnClickListener() {
            @Override
            public void onClick(int position) {
//                test(position);
                bondAndConnect(position);
            }
        });
        recyclerView.setAdapter(adapter);

    }

    @SuppressLint("RestrictedApi")
    private void feasyTest() {


        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                // Refresh the devices list every second
                .setReportDelay(0)
                // Hardware filtering has some issues on selected devices
//                .setUseHardwareFilteringIfSupported(true)
                // Samsung S6 and S6 Edge report equal value of RSSI for all devices. In this app we ignore the RSSI.
                /*.setUseHardwareBatchingIfSupported(false)*/
                .build();

        checkPermission();

//        mMeshManagerApi = nrfMeshRepository.getMeshManagerApi();
//        mBleMeshManager = nrfMeshRepository.getBleMeshManager();
//        MeshNetworkLiveData meshNetworkLiveData = nrfMeshRepository.getMeshNetworkLiveData();
        network = nrfMeshRepository.getMeshManagerApi().getMeshNetwork();
        if (network != null) {
            if (!network.getNetKeys().isEmpty()) {
                mNetworkId = nrfMeshRepository.getMeshManagerApi().generateNetworkId(network.getNetKeys().get(0).getKey());
            }
            for (ProvisionedMeshNode node : network.getNodes()) {
                if (!node.getUuid().equalsIgnoreCase(network.getSelectedProvisioner().getProvisionerUuid())) {

                } else {
                    phoneNode = node;

                }
            }
        }

        Log.i(TAG, "feasyTest: " + phoneNode.getNodeName());
        Log.i(TAG, "feasyTest: " + phoneNode.getUnicastAddress());
        if (phoneNode.getElements() != null && !phoneNode.getElements().isEmpty()) {
            if (phoneNode.getCompanyIdentifier() != null) {
                Log.i(TAG, "feasyTest: " + CompanyIdentifiers.getCompanyName(phoneNode.getCompanyIdentifier().shortValue()));
            }
            Log.i(TAG, "feasyTest: " + phoneNode.getElements().size());
            Log.i(TAG, "feasyTest: " + phoneNode.getNumberOfElements());
            int i = 0;
            for (Element element : phoneNode.getElements().values()) {
                i += element.getMeshModels().size();
            }
            Log.i(TAG, "feasyTest: " + i);

        }


        scanner.startScan(null, settings, mScanCallbacks);
        runnable = new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        };
        handler.postDelayed(runnable, 20000);

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
                feasyTest();

//                startScan();
                break;
            case R.id.stop:
                stopScan();
                break;
        }
    }


    /**
     * 蓝牙配对并连接
     */
    public void bondAndConnect(int position) {
//        stopScan();
        //取消搜索

//        nrfMeshRepository.disconnect();
        checkPermission();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                nrfMeshRepository.connect(getBaseContext(), mDevices.get(position), false);
            }
        }, 0);

//        nrfMeshRepository.onDeviceReady(mDevices.get(position).getDevice());
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                nrfMeshRepository.identifyNode(mDevices.get(position));
//            }
//        }, 4000);
        if (nrfMeshRepository.isProvisioningComplete()) {
        }
        Log.i(TAG, "bondAndConnect: " + nrfMeshRepository.isProvisioningComplete());
        if (nrfMeshRepository.getBleMeshManager().isDeviceReady()) {
        }
        Log.i(TAG, "bondAndConnect: " + nrfMeshRepository.getBleMeshManager().isDeviceReady());

        Log.i(TAG, "bondAndConnect: " + "wait300");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "bondAndConnect: " + nrfMeshRepository.isProvisioningComplete());
                Log.i(TAG, "bondAndConnect: " + nrfMeshRepository.getBleMeshManager().isDeviceReady());

            }
        }, 5000);
//            nrfMeshRepository.disconnect();
//            nrfMeshRepository.identifyNode(mDevices.get(position));


    }

    public void stopScan() {
        checkPermission();
        handler.removeCallbacksAndMessages(null);
        scanner.stopScan(mScanCallbacks);
    }


}