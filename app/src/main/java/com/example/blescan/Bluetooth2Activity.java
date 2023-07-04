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

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanRecord;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import android.bluetooth.le.BluetoothLeScanner;
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

import com.feasycom.feasymesh.library.ApplicationKey;
import com.feasycom.feasymesh.library.MeshBeacon;
import com.feasycom.feasymesh.library.MeshManagerApi;
import com.feasycom.feasymesh.library.MeshManagerCallbacks;
import com.feasycom.feasymesh.library.MeshNetwork;
import com.feasycom.feasymesh.library.MeshProvisioningStatusCallbacks;
import com.feasycom.feasymesh.library.MeshStatusCallbacks;
import com.feasycom.feasymesh.library.Provisioner;
import com.feasycom.feasymesh.library.UnprovisionedBeacon;
import com.feasycom.feasymesh.library.provisionerstates.ProvisioningCapabilities;
import com.feasycom.feasymesh.library.provisionerstates.ProvisioningFailedState;
import com.feasycom.feasymesh.library.provisionerstates.ProvisioningState;
import com.feasycom.feasymesh.library.provisionerstates.UnprovisionedMeshNode;
import com.feasycom.feasymesh.library.transport.ControlMessage;
import com.feasycom.feasymesh.library.transport.Element;
import com.feasycom.feasymesh.library.transport.MeshMessage;
import com.feasycom.feasymesh.library.transport.ProvisionedMeshNode;
import com.feasycom.feasymesh.library.utils.AuthenticationOOBMethods;
import com.feasycom.feasymesh.library.utils.CompanyIdentifiers;
import com.feasycom.feasymesh.library.utils.MeshParserUtils;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class Bluetooth2Activity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "test";
    private ArrayList<String> permissionList = new ArrayList<String>();
    private MyBluetoothAdapter adapter;

    private HandlerThread handlerThread;
    private Handler handler;

    private ProvisionedMeshNode phoneNode;
    private NrfMeshRepository nrfMeshRepository;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private ArrayList<byte[]> uuidList = new ArrayList<>();
    private boolean mIsScanning = false;
    private Runnable runnable;

    private void showList(ScanResult result) {
        if (result.getScanRecord().getServiceData() != null & result.getScanRecord().getServiceUuids() != null) {

            byte[] uuid = result.getScanRecord().getServiceData().get(result.getScanRecord().getServiceUuids().get(0));
            if ((uuid != null ? uuid.length : 0) >= 16) {
                BluetoothDevice scanDevice = result.getDevice();
                if (deviceList.isEmpty()) {
                    deviceList.add(scanDevice);
                    uuidList.add(uuid);
                    resultList.add(result);
                } else {
                    for (int i = 0; i < deviceList.size(); i++) {
                        if (scanDevice.equals(deviceList.get(i))) {
                            deviceList.set(i, deviceList.get(i));
                            uuidList.set(i, uuid);
                            resultList.set(i, result);

                            break;
                        }
                        if (i == deviceList.size() - 1) {
                            deviceList.add(scanDevice);
                            uuidList.add(uuid);
                            resultList.add(result);
                        }
                    }

                }
//                adapter.setData(deviceList);
            }
        }
    }

    private ArrayList<ScanResult> resultList = new ArrayList<>();
    private ArrayList<ExtendedBluetoothDevice> mDevices = new ArrayList<>();
    private Set<String> meshProvisioningAddress = new HashSet<String>();
    private final ScanCallback mScanCallbacks = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, @NonNull final ScanResult result) {
//            showList(result);
//            final ScanRecord scanRecord = result.getScanRecord();
//            if (scanRecord != null) {
//                if (scanRecord.getBytes() != null) {
//                    final byte[] beaconData = mMeshManagerApi.getMeshBeaconData(scanRecord.getBytes());
//                    if (beaconData != null) {
//                        if (mMeshManagerApi.getMeshBeacon(beaconData) != null) {
//                            ExtendedBluetoothDevice device = new ExtendedBluetoothDevice(result, mMeshManagerApi.getMeshBeacon(beaconData));
//                            if (mDevices.isEmpty()) {
//                                mDevices.add(device);
//                            } else {
//                                for (int i = 0; i < mDevices.size(); i++) {
//                                    if (device.getAddress().equals(mDevices.get(i).getAddress())) {
//                                        if (device.getName() == null) {
//                                            device.setName(mDevices.get(i).getName());
//                                        }
//                                        mDevices.set(i, device);
//                                        break;
//                                    }
//                                    if (i == mDevices.size() - 1) {
//                                        mDevices.add(device);
//                                    }
//
//                                }
//
//                            }
//                            adapter.setData(mDevices);
//                        }
//                    }
//                }
//            }


            // 连接的设备
//            ExtendedBluetoothDevice device = mDevices.get(0);
//            stopScan();
//            nrfMeshRepository.connect(getBaseContext(), device, true);
            try {
                if (result.getScanRecord() != null) {
                    if (result.getScanRecord().getServiceUuids() != null) {
                        if (result.getScanRecord().getServiceUuids().get(0).toString().equals(BleMeshManager.MESH_PROVISIONING_UUID.toString())) {
                            if (!meshProvisioningAddress.contains(result.getDevice().getAddress())) {
                                meshProvisioningAddress.add(result.getDevice().getAddress());
                            }
                            updateScannerLiveData(result);
                        }
                    } else {
                        if (meshProvisioningAddress.contains(result.getDevice().getAddress())) {
                            updateScannerLiveData(result);
                        }
                    }
                }
            } catch (
                    Exception ex) {
                Log.i(TAG, "Error1: " + ex.getMessage());
            }
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

    private void updateScannerLiveData(final ScanResult result) {
        checkPermission();
       /* if (result.getDevice() .getName()== null && result.getScanRecord().getDeviceName() == null){
            return;
        }*/
        final ScanRecord scanRecord = result.getScanRecord();
        // Log.e(TAG, "updateScannerLiveData: " + MeshParserUtils.bytesToHex(scanRecord.getBytes(), false) );
        if (scanRecord != null) {
            if (scanRecord.getBytes() != null) {
                final byte[] beaconData = nrfMeshRepository.getMeshManagerApi().getMeshBeaconData(scanRecord.getBytes());
                if (beaconData != null) {
                    ExtendedBluetoothDevice device;

                    MeshBeacon beacon = nrfMeshRepository.getMeshManagerApi().getMeshBeacon(beaconData);
                    final int index = indexOf(result);
                    if (index == -1) {
                        device = new ExtendedBluetoothDevice(result, beacon);
                        // Update RSSI and name
                        device.setRssi(result.getRssi());
                        if (result.getDevice().getName() == null) {
                            if (result.getScanRecord().getDeviceName() != null) {
                                device.setName(result.getScanRecord().getDeviceName());
                            }
                        } else {
                            device.setName(result.getDevice().getName());
                        }
                        mDevices.add(device);
                        adapter.setData(mDevices, index);
                    } else {
                        device = mDevices.get(index);
                        // Update RSSI and name
                        device.setRssi(result.getRssi());
                        if (device.getName() == null) {
                            if (result.getDevice().getName() != null) {
                                device.setName(result.getDevice().getName());
                                mDevices.set(index, device);
                                adapter.setData(mDevices, index);
                            } else if (result.getScanRecord().getDeviceName() != null) {
                                device.setName(result.getScanRecord().getDeviceName());
                                mDevices.set(index, device);
                                adapter.setData(mDevices, index);
                            }
                        }
                    }
                }
            }
        }
    }

    private int indexOf(ScanResult result) {
        int i = 0;
        for (final ExtendedBluetoothDevice device : mDevices) {
            if (device.matches(result))
                return i;
            i++;
        }
        return -1;
    }

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
        nrfMeshRepository = new NrfMeshRepository(new MeshManagerApi(this), new BleMeshManager(this));

//        mMeshManagerApi = new MeshManagerApi(this);
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
//
//        mBleMeshManager = new BleMeshManager(this);
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
    }

    private void intBtn() {
        Button start = findViewById(R.id.start);
        start.setOnClickListener(this);
        Button stop = findViewById(R.id.stop);
        stop.setOnClickListener(this);
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

    private void feasyTest() {

        checkPermission();

//        network = mMeshManagerApi.getMeshNetwork();
//        if (network != null) {
//            if (!network.getNetKeys().isEmpty()) {
//                mNetworkId = mMeshManagerApi.generateNetworkId(network.getNetKeys().get(0).getKey());
//            }
//            for (ProvisionedMeshNode node : network.getNodes()) {
//                if (!node.getUuid().equalsIgnoreCase(network.getSelectedProvisioner().getProvisionerUuid())) {
//
//                } else {
//                    phoneNode = node;
//
//                }
//            }
//        }
//
//        Log.i(TAG, "feasyTest: " + phoneNode.getNodeName());
//        Log.i(TAG, "feasyTest: " + phoneNode.getUnicastAddress());
//        if (phoneNode.getElements() != null && !phoneNode.getElements().isEmpty()) {
//            if (phoneNode.getCompanyIdentifier() != null) {
//                Log.i(TAG, "feasyTest: " + CompanyIdentifiers.getCompanyName(phoneNode.getCompanyIdentifier().shortValue()));
//            }
//            Log.i(TAG, "feasyTest: " + phoneNode.getElements().size());
//            Log.i(TAG, "feasyTest: " + phoneNode.getNumberOfElements());
//            int i = 0;
//            for (Element element : phoneNode.getElements().values()) {
//                i += element.getMeshModels().size();
//            }
//            Log.i(TAG, "feasyTest: " + i);
//
//        }
        if (mIsScanning) {
            return;
        }
        mIsScanning = true;
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                // Refresh the devices list every second
                .setReportDelay(0)
                // Hardware filtering has some issues on selected devices
                .setUseHardwareFilteringIfSupported(true)
                // Samsung S6 and S6 Edge report equal value of RSSI for all devices. In this app we ignore the RSSI.
                /*.setUseHardwareBatchingIfSupported(false)*/
                .build();
        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
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
    @SuppressLint("RestrictedApi")
    public void bondAndConnect(int position) {
        ExtendedBluetoothDevice device = mDevices.get(position);
        Log.i(TAG, "onClick: ");
//        nrfMeshRepository.disconnect();
        if (mIsScanning) {
            stopScan();
        }
        //取消搜索

//        nrfMeshRepository.disconnect();
        checkPermission();
        nrfMeshRepository.getBleMeshManager().isProvisioning = true;
        nrfMeshRepository.connect(getBaseContext(), device, false);
//        nrfMeshRepository.getMeshNetworkLiveData().resetSelectedAppKey();

        nrfMeshRepository.isDeviceReady().observe(this, deviceReady -> {
            if (nrfMeshRepository.getBleMeshManager().isDeviceReady()) {
                final boolean isComplete = nrfMeshRepository.isProvisioningComplete();
                if (isComplete) {
                    nrfMeshRepository.getProvisioningState().observe(this, provisioningStateLiveData -> {
                        if (provisioningStateLiveData != null) {
                            final ProvisionerProgress provisionerProgress = provisioningStateLiveData.getProvisionerProgress();
                            if (provisionerProgress != null) {
                                final ProvisionerStates state = provisionerProgress.getState();
                                switch (state) {
                                    case PROVISIONING_FAILED:
                                        Log.i(TAG, "DIALOG_FRAGMENT_PROVISIONING_FAILED");
                                        break;
                                    case PROVISIONING_AUTHENTICATION_STATIC_OOB_WAITING:
                                    case PROVISIONING_AUTHENTICATION_OUTPUT_OOB_WAITING:
                                    case PROVISIONING_AUTHENTICATION_INPUT_OOB_WAITING:
                                        Log.i(TAG, "DIALOG_FRAGMENT_AUTH_INPUT_TAG");

                                        break;
                                    case PROVISIONING_AUTHENTICATION_INPUT_ENTERED:
                                        Log.i(TAG, "DIALOG_FRAGMENT_AUTH_INPUT_TAG 2");

                                        break;
                                    case PROVISIONING_COMPLETE:
                                    case NETWORK_TRANSMIT_STATUS_RECEIVED:
                                        Log.i(TAG, "DIALOG_FRAGMENT_CONFIGURATION_STATUS");

                                        break;
                                    case PROVISIONER_UNASSIGNED:
                                        Log.i(TAG, "DIALOG_FRAGMENT_CONFIGURATION_STATUS 2");
                                        break;
                                    default:
                                        break;
                                }

                            }
                        }
                    });
                    Log.i(TAG, "bondAndConnect: complete");
                } else {
//                    setupProvisionerStateObservers(provisioningStatusContainer);
                    UnprovisionedMeshNode node = nrfMeshRepository.getUnprovisionedMeshNode().getValue();
                    if (node == null) {
                        device.setName(nrfMeshRepository.getMeshNetworkLiveData().getNodeName());
                        nrfMeshRepository.identifyNode(device);
                    }
                }
            }
        });

        nrfMeshRepository.getMeshNetworkLiveData().observe(this, meshNetworkLiveData -> {
            final ApplicationKey applicationKey = meshNetworkLiveData.getSelectedAppKey();
            Log.e("TAG", "onCreate: -----------------------" + meshNetworkLiveData.getMeshNetwork().getUnicastAddress());
        });

        nrfMeshRepository.getUnprovisionedMeshNode().observe(this, meshNode -> {
            if (meshNode != null) {
                final ProvisioningCapabilities capabilities = meshNode.getProvisioningCapabilities();
                if (capabilities != null) {
                    final MeshNetwork network = nrfMeshRepository.getMeshNetworkLiveData().getMeshNetwork();
                    if (network != null) {
                        try {
                            final int elementCount = capabilities.getNumberOfElements();
                            final Provisioner provisioner = network.getSelectedProvisioner();
                            final int unicast = network.nextAvailableUnicastAddress(elementCount, provisioner);
                            network.assignUnicastAddress(unicast);
                        } catch (IllegalArgumentException ex) {
                            Log.i(TAG, ex.getMessage());
                        }
                        UnprovisionedMeshNode node = nrfMeshRepository.getUnprovisionedMeshNode().getValue();
                        if (node != null) {
                            if (node.getProvisioningCapabilities() != null) {
                                if (node.getProvisioningCapabilities().getAvailableOOBTypes().size() == 1 &&
                                        node.getProvisioningCapabilities().getAvailableOOBTypes().get(0) == AuthenticationOOBMethods.NO_OOB_AUTHENTICATION) {
                                    try {
                                        node.setNodeName(nrfMeshRepository.getMeshNetworkLiveData().getNodeName());
                                        nrfMeshRepository.getMeshManagerApi().startProvisioning(node);
                                    } catch (IllegalArgumentException ex) {
                                        Log.i(TAG, ex.getMessage());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

    }

    private void stopScan() {
        checkPermission();
        handler.removeCallbacks(runnable);
        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        Log.e(TAG, "stopScan: 停止扫描");
        scanner.stopScan(mScanCallbacks);
        mIsScanning = false;
    }


}