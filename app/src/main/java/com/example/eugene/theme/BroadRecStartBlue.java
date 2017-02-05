package com.example.eugene.theme;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by eugene on 12/3/16.
 */

public class BroadRecStartBlue extends BroadcastReceiver {

    static final String ACE_LOG = "ACE_Broadcast";
    private static final String BROADCAST_ACTION = "com.example.eugene.action.START_BLUE";
    private static final String START_DEVICE_CONNECT = "com.example.eugene.action.START_DEVICE_CONNECT";
    private static final String START_DEVICE_RECONNECT = "com.example.eugene.action.START_DEVICE_RECONNECT";

    Intent intentService;

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Log.i(ACE_LOG, "Broadcast received: " + action);

        intentService = new Intent(context, ServiceBluetooth.class);
        // Поиск устройства по MAC адресу
        BluetoothDevice bD = intent.getParcelableExtra("bluetoothDevice");
        intentService.putExtra("bluetoothDevice", bD);

        // Запускаем сервис
        if(action.equals(START_DEVICE_CONNECT)){
            // Говорим сервису, что это первая настройка
            intentService.putExtra("firstInit", true);
            context.startService(intentService);
            Log.i(ACE_LOG, String.valueOf(bD));
        }

        // Перезапускаем сервис
        if(action.equals(START_DEVICE_RECONNECT)){
            context.startService(intentService);
            Log.i(ACE_LOG, String.valueOf(bD));
        }

        // Проверка состояния подключения
        switch (action){
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                Log.d(ACE_LOG, "Connected");

                try {
                    // Меняем статус
                    MainActivity  .getInstace().updateStatusDraw("1");
                    Toast.makeText(context, "Соединение установлено", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {

                }

                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                Log.d(ACE_LOG, "Disconnected");

                try {
                    // Меняем статус
                    MainActivity  .getInstace().updateStatusDraw("0");
                    Toast.makeText(context, "Соединение с устройством отсутствует", Toast.LENGTH_SHORT).show();
                    context.startService(intentService);
                } catch (Exception e) {

                }

                break;
        }

        if(action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

            int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

            switch(mode){
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                    Log.d(ACE_LOG, "SCAN_MODE_CONNECTABLE_DISCOVERABLE");
                    break;
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                    Log.d(ACE_LOG, "SCAN_MODE_CONNECTABLE");
                    break;
                case BluetoothAdapter.SCAN_MODE_NONE:
                    Log.d(ACE_LOG, "SCAN_MODE_NONE");
                    break;
            }
        }

        // Состояние Bluetooth модуля
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch(state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.d(ACE_LOG, "STATE_OFF");
                    Toast.makeText(context, "Bluetooth модуль отключен", Toast.LENGTH_SHORT).show();
                    try {
                        MainActivity  .getInstace().updateStatusDraw("0");
                    } catch (Exception e) {

                    }
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.d(ACE_LOG, "STATE_TURNING_OFF");

                    try{
                        context.stopService(intent);
                    }catch (Exception e){}

                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.d(ACE_LOG, "STATE_ON");

                    context.startService(intentService);
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d(ACE_LOG, "STATE_TURNING_ON");
                    break;
            }

        }

    }

}
