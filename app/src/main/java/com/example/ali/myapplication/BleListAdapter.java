package com.example.ali.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.polidea.rxandroidble2.RxBleDevice;

import java.util.ArrayList;

/**
 * Created by ali on 4/19/2018.
 */

public class BleListAdapter extends BaseAdapter {
    private ArrayList<RxBleDevice> devices;
    private Listener listener;

    public BleListAdapter(ArrayList<RxBleDevice> devices) {
        this.devices = devices;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        RxBleDevice device = devices.get(position);
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bluetooth_device, parent, false);

        TextView nameText = v.findViewById(R.id.txt_name);
        TextView macText = v.findViewById(R.id.txt_mac_address);

        if (device.getName() != null) {
            nameText.setText(device.getName());
        }
        if (device.getMacAddress() != null) {
            macText.setText(device.getMacAddress());
        }

        Button connectButton = v.findViewById(R.id.btn_connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onConnect(devices.get(position));
                }
            }
        });

        return v;
    }

    public void setDevices(ArrayList<RxBleDevice> devices) {
        this.devices = devices;
        notifyDataSetChanged();
    }

    public ArrayList<RxBleDevice> getDevices() {
        return devices;
    }

    public interface Listener {
        public void onConnect(RxBleDevice rxBleDevice);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
}
