package com.mediconnect;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private ListView deviceList;
    private TextView statusText;
    private ArrayAdapter<String> deviceAdapter;
    private ArrayList<BluetoothDevice> pairedDevicesList = new ArrayList<>();
    private BluetoothSocket btSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_PERMISSION_BT = 2;
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP UUID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = findViewById(R.id.deviceList);
        statusText = findViewById(R.id.statusText);
        Button connectBtn = findViewById(R.id.connectBtn);

        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceList.setAdapter(deviceAdapter);

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth आपके device में नहीं है!", Toast.LENGTH_LONG).show();
            finish();
        }

        connectBtn.setOnClickListener(v -> showPairedDevices());

        deviceList.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = pairedDevicesList.get(position);
            connectToDevice(device);
        });
    }

    private void showPairedDevices() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSION_BT);
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        deviceAdapter.clear();
        pairedDevicesList.clear();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceAdapter.add(device.getName() + "\n" + device.getAddress());
                pairedDevicesList.add(device);
            }
            statusText.setText("Paired Devices:");
        } else {
            statusText.setText("कोई भी paired device नहीं मिला!");
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            try {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothAdapter.cancelDiscovery();
                btSocket.connect();

                runOnUiThread(() ->
                        statusText.setText("Connected to: " + device.getName()));

                outputStream = btSocket.getOutputStream();
                inputStream = btSocket.getInputStream();

                listenForData();

            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "कनेक्शन विफल रहा!", Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    private void listenForData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    runOnUiThread(() ->
                            Toast.makeText(this, "Data: " + incomingMessage, Toast.LENGTH_SHORT).show());
                } catch (IOException e) {
                    break;
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (btSocket != null) btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPairedDevices();
            } else {
                Toast.makeText(this, "Bluetooth permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
