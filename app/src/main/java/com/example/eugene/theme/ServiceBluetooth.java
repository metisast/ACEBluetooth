package com.example.eugene.theme;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class ServiceBluetooth extends Service {

    static final String ACE_LOG = "ACE_Service";

    BluetoothDevice MAC, bD;
    private BluetoothAdapter bluetoothAdapter;
    SharedPreferences sPref;
    private static final String SAVED_TEXT = "MAC";

    public ServiceBluetooth() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(ACE_LOG, "SERVICE CREATED");

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Проверяем первое это ли подключение
        boolean status = intent.getBooleanExtra("firstInit", false);
        if(status){
            // Забираем имя устройства
            MAC = intent.getParcelableExtra("bluetoothDevice");
            bD = bluetoothAdapter.getRemoteDevice(String.valueOf(MAC));

            // Записываем MAC адрес устройства в память телефона
            sPref = getSharedPreferences("MacFile", MODE_PRIVATE);
            SharedPreferences.Editor ed = sPref.edit();
            ed.putString(SAVED_TEXT, String.valueOf(MAC));
            ed.commit();

            // Запускаем поток на соединение
            AsyncConnectBTdevice async = new AsyncConnectBTdevice(bD, MainActivity.getInstace(), MainActivity.getInstace(), MainActivity.getInstace());
            async.execute();
        }else{
            // Считываем сохраненный MAC адрес устройства
            sPref = getSharedPreferences("MacFile", MODE_PRIVATE);
            String savedText = sPref.getString(SAVED_TEXT, "");
            if(!savedText.equals("")){
                bD = bluetoothAdapter.getRemoteDevice(savedText);

                // Запускаем поток на соединение
                AsyncConnectBTdevice async = new AsyncConnectBTdevice(bD, MainActivity.getInstace(), MainActivity.getInstace(), MainActivity.getInstace());
                async.execute();
            }
        }

        // Отменяем перезапуск сервиса после закрытия приложения
        return START_NOT_STICKY;
    }
}
