package com.example.blescan;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyBluetoothAdapter extends RecyclerView.Adapter {
    private ArrayList<ExtendedBluetoothDevice> deviceList;
    private OnClickListener onClickListener;

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new MyBluetoothViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((MyBluetoothViewHolder) holder).address.setText("Address: " + deviceList.get(position).getAddress());
        if (ActivityCompat.checkSelfPermission(holder.itemView.getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return;
        }}
//        ((MyBluetoothViewHolder) holder).type.setText("Type: " + deviceList.get(position).getType());
        ((MyBluetoothViewHolder) holder).name.setText("Name:    " + deviceList.get(position).getName());

    }

    @Override
    public int getItemCount() {
        return deviceList == null? 0: deviceList.size();
    }


    public void setData(ArrayList<ExtendedBluetoothDevice> deviceList, int index) {
        this.deviceList = deviceList;
        if (index == -1){
            notifyDataSetChanged();
        }else {
            notifyItemChanged(index);
        }
    }

    class MyBluetoothViewHolder extends RecyclerView.ViewHolder {

        private TextView address, type, name;

        public MyBluetoothViewHolder(@NonNull View itemView) {
            super(itemView);
            address = (TextView)itemView.findViewById(R.id.address);
            type = (TextView)itemView.findViewById(R.id.type);
            name = (TextView)itemView.findViewById(R.id.name);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onClickListener != null){
                        onClickListener.onClick(getAdapterPosition());
                    }
                }
            });
        }


    }

    public void setOnClickListener(OnClickListener onClickListener){
        this.onClickListener = onClickListener;
    }

    public interface OnClickListener {
        void onClick(int position);
    }
}
