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
    private NrfMeshRepository nrfMeshRepository;
    private boolean mIsScanning = false;
    private Runnable runnable;

    private ArrayList<ScanResult> resultList = new ArrayList<>();
    private ArrayList<ExtendedBluetoothDevice> mDevices = new ArrayList<>();
    private Set<String> meshProvisioningAddress = new HashSet<String>();
    private final ScanCallback mScanCallbacks = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, @NonNull final ScanResult result) {
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

        }
    };
    private ExtendedBluetoothDevice device;

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
    }

    private void intBtn() {
        Button start = findViewById(R.id.start);
        start.setOnClickListener(this);
        Button stop = findViewById(R.id.stop);
        stop.setOnClickListener(this);
        Button send = findViewById(R.id.send);
        send.setOnClickListener(this);
    }

    private void initRecycleView() {
        RecyclerView recyclerView = findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyBluetoothAdapter();
        adapter.setOnClickListener(new MyBluetoothAdapter.OnClickListener() {
            @Override
            public void onClick(int position) {
                bondAndConnect(position);
            }
        });
        recyclerView.setAdapter(adapter);

    }

    private void feasyTest() {
        mDevices.clear();
        checkPermission();
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
                break;
            case R.id.stop:
                stopScan();
                break;
            case R.id.send:
                send();
                break;
        }
    }


    /**
     * 蓝牙配对并连接
     */
    @SuppressLint("RestrictedApi")
    public void bondAndConnect(int position) {
        device = mDevices.get(position);
        Log.i(TAG, "onClick: ");
        if (mIsScanning) {
            stopScan();
        }
        //取消搜索
        checkPermission();
        nrfMeshRepository.getBleMeshManager().isProvisioning = true;
        nrfMeshRepository.connect(getBaseContext(), device, false);

        nrfMeshRepository.isDeviceReady().observe(this, deviceReady -> {
            if (nrfMeshRepository.getBleMeshManager().isDeviceReady()) {
                final boolean isComplete = nrfMeshRepository.isProvisioningComplete();
                if (isComplete) {

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

    private void send() {
        nrfMeshRepository.onDataSent(device.getDevice(), nrfMeshRepository.getMtu(), new byte[]{1,2,3,4});
    }
}