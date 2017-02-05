package com.example.eugene.theme;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class AsyncConnectBTdevice extends AsyncTask<Void, String, String>{

    private final String ACE_LOG = "ACE_Async";

    BluetoothDevice bluetoothDevice;
    ThreadConnected myThreadConnected;
    private BluetoothSocket bluetoothSocket = null;
    private BlueThread blueThread;
    private BlueReconnect blueReconnect;
    Boolean status = false;

    final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB"; // UUID устройства
    private UUID myUUID;

    public AsyncConnectBTdevice(BluetoothDevice device, final BlueThread _blueThread, final BlueReconnect blueReconect) {
        bluetoothDevice = device;
        blueThread = _blueThread;
        this.blueReconnect = blueReconect;

        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(myUUID);
        }

        catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(ACE_LOG, String.valueOf(bluetoothSocket));
    }

    @Override
    protected String doInBackground(Void... voids) {
        boolean success = false;
        String result = null;

        publishProgress("True");

        try {
            bluetoothSocket.connect();
            success = true;
            status = true;
        }

        catch (IOException e) {
            e.printStackTrace();
            result = "Нет подключения, проверьте Bluetooth-устройство!";
            status = false;

            try {
                bluetoothSocket.close();
                blueReconnect.blueReconnect(true);
            }

            catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        if(success) {  // Если законнектились, запускаем поток приёма и отправки данных
            result = "Соединение установлено";
            myThreadConnected = new ThreadConnected(bluetoothSocket);
            myThreadConnected.start(); // запуск потока приёма и отправки данных
        }

        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        // Передаем идентификатор потока в Activity
        if(status){
            /* Передаем наш новый лист */
            if(blueThread!=null)
                blueThread.getBluetoothWrite(myThreadConnected);
                blueReconnect.blueReconnect(false);
        } else {
            blueReconnect.blueReconnect(true);
        }

        Log.d(ACE_LOG, result);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(ACE_LOG, "Соединеняю с устройством ");
        Toast.makeText(MainActivity.getInstace(), "Соединеняю с устройством", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    public interface BlueThread{
        public void getBluetoothWrite(ThreadConnected myThreadConnected);
    }

    public interface BlueReconnect{
        public void blueReconnect(boolean status);
    }
}
