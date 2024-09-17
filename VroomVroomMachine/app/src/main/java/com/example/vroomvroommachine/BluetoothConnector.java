package com.example.vroomvroommachine;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnector extends Thread {
    private final BluetoothSocket bluetoothSocket;
    public static Handler handler;

    @SuppressLint("MissingPermission")
    public BluetoothConnector(BluetoothDevice device, UUID uuid, Handler handler) {
        BluetoothSocket temp = null;
        BluetoothConnector.handler = handler;

        try {
            temp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.e("SOCKET_BEEPBOOP", "It appears that we have failed to create the socket");
        }
        bluetoothSocket = temp;
    }

    @SuppressLint("MissingPermission")
    public void run() {
        try {
            bluetoothSocket.connect();
        } catch (IOException e) {
            Log.e("SOCKET_BEEPBOOP", "It appears that we have failed to connect to the Bluetooth device");
            try {
                bluetoothSocket.close();
            } catch (IOException ex) {
                Log.e("SOCKET_BEEPBOOP", "It appears that we have even failed to close the socket");
            }
        }
    }

    public void cancel() {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            Log.e("SOCKET_BEEPBOOP", "It appears that we have even failed to close the socket");
        }
    }

    public BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }
}
