package com.example.vroomvroommachine;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothConnected {
    private final BluetoothSocket bluetoothSocket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private String value;

    public BluetoothConnected(BluetoothSocket bs) throws IOException {
        bluetoothSocket = bs;
        InputStream tempInputStream = null;
        OutputStream tempOutputStream = null;

        try {
            tempInputStream = bs.getInputStream();
        } catch (IOException e) {
            Log.e("INPUT_STREAM_BEEPBOOP", "It appears that something went from while creating the input stream");
        }

        try {
            tempOutputStream = bs.getOutputStream();
        } catch (IOException e) {
            Log.e("OUTPUT_STREAM_BEEPBOOP", "It appears that something went from while creating the output stream");
        }

        inputStream = tempInputStream;
        outputStream = tempOutputStream;
    }

    public void run() {
        byte[] buffer = new byte[1024];
        int bytes = 0;
        int numReadings = 0;

        while (numReadings < 1) {
            try {
                buffer[bytes] = (byte) inputStream.read();
                String message = "";
                if (buffer[bytes] == '\n') {
                    message = new String (buffer, 0, bytes);
                    bytes = 0;
                    numReadings++;
                } else {
                    bytes++;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void cancel() {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            Log.e("problems everywhere", "socket won't close");
        }
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}
