/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.example.blescan;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.feasycom.feasymesh.library.ble.BleManager;
import com.feasycom.feasymesh.library.ble.callback.DataReceivedCallback;
import com.feasycom.feasymesh.library.ble.callback.DataSentCallback;

import java.util.UUID;



public class BleMeshManager extends LoggableBleManager<BleMeshManagerCallbacks>{

    private static final String TAG = "BleMeshManager";
    private static final int MTU_SIZE_DEFAULT = 23;
    private static final int MTU_SIZE_MAX = 517;

    /**
     * Mesh provisioning service UUID
     */
    public final static UUID MESH_PROVISIONING_UUID = UUID.fromString("00001827-0000-1000-8000-00805F9B34FB");
    /**
     * Mesh provisioning data in characteristic UUID
     */
    private final static UUID MESH_PROVISIONING_DATA_IN = UUID.fromString("00002ADB-0000-1000-8000-00805F9B34FB");
    /**
     * Mesh provisioning data out characteristic UUID
     */
    private final static UUID MESH_PROVISIONING_DATA_OUT = UUID.fromString("00002ADC-0000-1000-8000-00805F9B34FB");



    /**
     * Mesh provisioning service UUID
     */
    public final static UUID MESH_PROXY_UUID = UUID.fromString("00001828-0000-1000-8000-00805F9B34FB");

    /**
     * Mesh provisioning data in characteristic UUID
     */
    private final static UUID MESH_PROXY_DATA_IN = UUID.fromString("00002ADD-0000-1000-8000-00805F9B34FB");

    /**
     * Mesh provisioning data out characteristic UUID
     */
    private final static UUID MESH_PROXY_DATA_OUT = UUID.fromString("00002ADE-0000-1000-8000-00805F9B34FB");

    private final static UUID FEASY_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private final static UUID FSC_NOTIFICATION = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");

    private final static UUID FSC_WRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic mMeshProvisioningDataInCharacteristic;
    private BluetoothGattCharacteristic mMeshProvisioningDataOutCharacteristic;
    public BluetoothGattCharacteristic mMeshProxyDataInCharacteristic;
    private BluetoothGattCharacteristic mMeshProxyDataOutCharacteristic;
    private BluetoothGattCharacteristic mFeasyCharacteristic;


    private boolean isProvisioningComplete;
    private boolean mIsDeviceReady;
    private boolean mNodeReset;

    public boolean isProvisioning = false;
    /**
     * BluetoothGatt callbacks for connection/disconnection, service discovery, receiving notifications, etc.
     */
    private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {

        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService meshProxyService = gatt.getService(MESH_PROXY_UUID);

            final BluetoothGattService feasyServce = gatt.getService(FEASY_SERVICE);
            mFeasyCharacteristic = feasyServce.getCharacteristic(FSC_NOTIFICATION);

            boolean find_mesh_proxy_service = false;
            boolean find_mesh_provisioning_service = false;


            if (isProvisioning){
                if (meshProxyService != null) {
                    isProvisioningComplete = true;
                    mMeshProxyDataInCharacteristic = meshProxyService.getCharacteristic(MESH_PROXY_DATA_IN);
                    mMeshProxyDataOutCharacteristic = meshProxyService.getCharacteristic(MESH_PROXY_DATA_OUT);

                    find_mesh_proxy_service = mMeshProxyDataInCharacteristic != null &&
                            mMeshProxyDataOutCharacteristic != null &&
                            hasNotifyProperty(mMeshProxyDataOutCharacteristic) &&
                            hasWriteNoResponseProperty(mMeshProxyDataInCharacteristic);
                }
                final BluetoothGattService meshProvisioningService = gatt.getService(MESH_PROVISIONING_UUID);
                if (meshProvisioningService != null) {
                    isProvisioningComplete = false;
                    mMeshProvisioningDataInCharacteristic = meshProvisioningService.getCharacteristic(MESH_PROVISIONING_DATA_IN);
                    mMeshProvisioningDataOutCharacteristic = meshProvisioningService.getCharacteristic(MESH_PROVISIONING_DATA_OUT);

                    find_mesh_provisioning_service = mMeshProvisioningDataInCharacteristic != null &&
                            mMeshProvisioningDataOutCharacteristic != null &&
                            hasNotifyProperty(mMeshProvisioningDataOutCharacteristic) &&
                            hasWriteNoResponseProperty(mMeshProvisioningDataInCharacteristic);
                }
                return find_mesh_proxy_service || find_mesh_provisioning_service;
            }else {
                if (meshProxyService != null) {
                    isProvisioningComplete = true;
                    mMeshProxyDataInCharacteristic = meshProxyService.getCharacteristic(MESH_PROXY_DATA_IN);
                    mMeshProxyDataOutCharacteristic = meshProxyService.getCharacteristic(MESH_PROXY_DATA_OUT);
                    return mMeshProxyDataInCharacteristic != null &&
                            mMeshProxyDataOutCharacteristic != null &&
                            hasNotifyProperty(mMeshProxyDataOutCharacteristic) &&
                            hasWriteNoResponseProperty(mMeshProxyDataInCharacteristic);
                }
            }
            return false;
        }

        @Override
        protected void initialize() {
            Log.e(TAG, "initialize: ");
            requestMtu(MTU_SIZE_MAX).enqueue();
            // This callback will be called each time a notification is received.
            final DataReceivedCallback onDataReceived = (device, data) ->
                    mCallbacks.onDataReceived(device, getMaximumPacketSize(), data.getValue());

            // Set the notification callback and enable notification on Data In characteristic.
            final BluetoothGattCharacteristic characteristic = isProvisioningComplete ?
                    mMeshProxyDataOutCharacteristic : mMeshProvisioningDataOutCharacteristic;
            setNotificationCallback(characteristic).with(onDataReceived);
            // setNotificationCallback(mFeasyCharacteristic).with(onDataReceived);

            enableNotifications(characteristic).enqueue();
            enableNotifications(mFeasyCharacteristic).enqueue();
        }

        @Override
        protected void onDeviceDisconnected() {
            //We reset the MTU to 23 upon disconnection
            overrideMtu(MTU_SIZE_DEFAULT);
            mIsDeviceReady = false;
            isProvisioningComplete = false;
            isProvisioning = false;
            mMeshProvisioningDataInCharacteristic = null;
            mMeshProvisioningDataOutCharacteristic = null;
            mMeshProxyDataInCharacteristic = null;
            mMeshProxyDataOutCharacteristic = null;
        }

        @Override
        protected void onDeviceReady() {
            Log.e(TAG, "onDeviceReady: -----------------------------------------" );
            mIsDeviceReady = true;
            super.onDeviceReady();
        }
    };

    public BleMeshManager(@NonNull final Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return mGattCallback;
    }

    @Override
    protected boolean shouldClearCacheWhenDisconnected() {
        // This is to make sure that Android will discover the services as the the mesh node will
        // change the provisioning service to a proxy service.
        final boolean result = !isProvisioningComplete ||  mNodeReset;
        mNodeReset = false;
        return result;
    }

    /**
     * After calling this method the device cache will be cleared upon next disconnection.
     */
    public void setClearCacheRequired() {
        mNodeReset = true;
    }

    /**
     * Sends the mesh pdu.
     * <p>
     * The function will chunk the pdu to fit in to the mtu size supported by the node.
     *
     * @param pdu mesh pdu.
     */
    public void sendPdu(final byte[] pdu) {
        if (!mIsDeviceReady)
            return;

        // This callback will be called each time the data were sent.
        final DataSentCallback callback = (device, data) ->
                mCallbacks.onDataSent(device, getMaximumPacketSize(), data.getValue());

        // Write the right characteristic.
        final BluetoothGattCharacteristic characteristic = isProvisioningComplete ?
                mMeshProxyDataInCharacteristic : mMeshProvisioningDataInCharacteristic;
        writeCharacteristic(characteristic, pdu)
                .split()
                .with(callback)
                .enqueue();
    }

    public int getMaximumPacketSize() {
        return super.getMtu() - 3;
    }

    public boolean isProvisioningComplete() {
        return isProvisioningComplete;
    }

    public boolean isDeviceReady() {
        return mIsDeviceReady;
    }

    private boolean hasWriteNoResponseProperty(@NonNull final BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0;
    }

    private boolean hasNotifyProperty(@NonNull final BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }


    public void refresh(){
        if (mMeshProxyDataInCharacteristic == null){
            disconnect().enqueue();
        }else {
            isProvisioningComplete = true;
            // This callback will be called each time a notification is received.
            final DataReceivedCallback onDataReceived = (device, data) ->
                    mCallbacks.onDataReceived(device, getMaximumPacketSize(), data.getValue());
            // Set the notification callback and enable notification on Data In characteristic.
            final BluetoothGattCharacteristic characteristic = isProvisioningComplete ?
                    mMeshProxyDataOutCharacteristic : mMeshProvisioningDataOutCharacteristic;
            setNotificationCallback(characteristic).with(onDataReceived);
            Log.e(TAG, "refresh: " + characteristic.getUuid().toString() );
            enableNotifications(characteristic).enqueue();
        }
    }
}
