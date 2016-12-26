package com.example.eugene.theme;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import me.priyesh.chroma.ChromaDialog;
import me.priyesh.chroma.ColorMode;
import me.priyesh.chroma.ColorSelectListener;

import static android.R.layout.simple_list_item_1;

/**
 * Created by eugene on 11/30/16.
 */

public class ConfigsActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String ACE_LOG = "ACE_ConfigActivity";

    private static final String START_DEVICE_CONNECT = "com.example.eugene.action.START_DEVICE_CONNECT";
    private static final String BROADCAST_ACTION = "com.example.eugene.action.BROADCAST";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String BTN_DEFAULT = "btnDefault";
    private static final String BTN_PRESSED = "btnPressed";

    BluetoothAdapter bluetoothAdapter;
    ArrayAdapter<String> pairedDeviceAdapter;
    ArrayList<String> pairedDeviceArrayList;
    ListView deviceList;
    FrameLayout frame;
    Button btnSave;
    BroadcastReceiver br;
    SharedPreferences sPref;

    public TextView textInfo;
    ImageButton btnDefault, btnPressed;
    TextView tvDefault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        textInfo = (TextView) findViewById(R.id.textInfo);
        tvDefault = (TextView) findViewById(R.id.tvDefault);
        deviceList = (ListView) findViewById(R.id.deviceList);
        frame = (FrameLayout) findViewById(R.id.frame);
        btnDefault = (ImageButton) findViewById(R.id.btnDefault);
        btnPressed = (ImageButton) findViewById(R.id.btnPressed);

        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);
        btnDefault.setOnClickListener(this);
        btnPressed.setOnClickListener(this);

        // Проверка на поддержку bluetooth модуля
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Ваше устройство не поддерживает Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Загружаем цвета кнопок, если они были выбраны
        btnDefault.setColorFilter(Color.parseColor(getBtnColor(BTN_DEFAULT)));
        btnPressed.setColorFilter(Color.parseColor(getBtnColor(BTN_PRESSED)));

    }

    @Override
    protected void onStart() {// Запрос на включение Bluetooth
        super.onStart();

        // Регистрируем ресивер
        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);
        br = new BroadRecStartBlue();
        registerReceiver(br, intentFilter);

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        setup();
    }

    // Создание списка сопряжённых Bluetooth-устройств
    private void setup() {

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) { // Если есть сопряжённые устройства

            pairedDeviceArrayList = new ArrayList<>();

            for (BluetoothDevice device : pairedDevices) { // Добавляем сопряжённые устройства - Имя + MAC-адресс
                pairedDeviceArrayList.add(device.getName() + "\n" + device.getAddress());
            }

            pairedDeviceAdapter = new ArrayAdapter<>(this, simple_list_item_1, pairedDeviceArrayList);
            deviceList.setAdapter(pairedDeviceAdapter);

            deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() { // Клик по нужному устройству

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    //deviceList.setVisibility(View.GONE); // После клика скрываем список

                    String  itemValue = (String) deviceList.getItemAtPosition(position);
                    String MAC = itemValue.substring(itemValue.length() - 17); // Вычленяем MAC-адрес

                    BluetoothDevice bD = bluetoothAdapter.getRemoteDevice(MAC);

                    //Запускаем ресивер и передаем MAC адрес устройства
                    Intent intent = new Intent(START_DEVICE_CONNECT);
                    intent.putExtra("bluetoothDevice", bD);
                    sendBroadcast(intent);
                }
            });

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){ // Если разрешили включить Bluetooth, тогда void setup()

            if(resultCode == Activity.RESULT_OK) {
                setup();
            }

            else { // Если не разрешили, закрываем текущее окно

                Toast.makeText(this, "BlueTooth не включён", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // дерегистрируем (выключаем) BroadcastReceiver
        unregisterReceiver(br);
        Log.d(ACE_LOG, "Destroy");
    }

    @Override
    protected void onResume() {
        super.onPostResume();
        Log.d(ACE_LOG, "Resume");
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            // Сохраняем изменения
            case R.id.btnSave:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
            case R.id.btnDefault:
                changeAndSaveBtnColor(btnDefault, BTN_DEFAULT);
                break;
            case R.id.btnPressed:
                changeAndSaveBtnColor(btnPressed, BTN_PRESSED);
                break;
        }
    }

    /* Меняем цвет кнопки и сохраняем в памяти телефона */
    public void changeAndSaveBtnColor(final ImageButton ib, final String btnType){
        final String[] color = new String[1];
        new ChromaDialog.Builder()
                .initialColor(Color.parseColor(getBtnColor(btnType))) // Загружаем предыдущий цвет
                .colorMode(ColorMode.RGB)
                .onColorSelected(new ColorSelectListener() {
                    @Override
                    public void onColorSelected(@ColorInt int i) {
                        color[0] = String.format("#%06X", 0xFFFFFF & i);

                        // Записываем цвет кнопки в память телефона
                        sPref = getSharedPreferences("MacFile", MODE_PRIVATE);
                        SharedPreferences.Editor ed = sPref.edit();
                        Log.d(ACE_LOG, color[0]);
                        ed.putString(btnType, color[0]);
                        ed.commit();

                        // Меняем цвет кнопки
                        ib.setColorFilter(Color.parseColor(color[0]));
                    }
                })
                .create()
                .show(getSupportFragmentManager(), "ChromaDialog");
    }

    /* Получить цвет кнопки */
    public String getBtnColor(String btnType){
        // Считываем сохраненный цвет кнопки
        sPref = getSharedPreferences("MacFile", MODE_PRIVATE);
        String savedText = sPref.getString(btnType, "");
        if(!savedText.equals("")) {
            Log.d(ACE_LOG, savedText);
            return savedText;
        }else{
            return "#C6C7C7";
        }
    }

}
