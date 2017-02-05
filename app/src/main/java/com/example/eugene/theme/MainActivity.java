package com.example.eugene.theme;

import android.animation.Animator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity implements View.OnClickListener, View.OnTouchListener, AsyncConnectBTdevice.BlueThread, AsyncConnectBTdevice.BlueReconnect {

    private static MainActivity ins;

    public static MainActivity  getInstace(){
        return ins;
    }

    private static final String START_DEVICE_RECONNECT = "com.example.eugene.action.START_DEVICE_RECONNECT";
    private static final String ACE_LOG = "ACE_Activity";
    private static final String SAVED_TEXT = "MAC";
    private static final String BTN_DEFAULT = "btnDefault";
    private static final String BTN_PRESSED = "btnPressed";
    private static final String DEFAULT_COLOR= "#C6C7C7";

    Vibrator v;
    Animation animAlpha;

    private List<ImageButton> imageButtons;
    private static final int[] BUTTON_IDS = {
            R.id.btnFLUp, R.id.btnFLDown, R.id.btnFRUp, R.id.btnFRDown, R.id.btnFAllUp, R.id.btnFAllDown,
            R.id.btnRLUp, R.id.btnRLDown, R.id.btnRRUp, R.id.btnRRDown, R.id.btnRAllUp, R.id.btnRAllDown,
            R.id.btnAllUp, R.id.btnAllDown
    };

    /*ImageButton btnFLUp, btnFLDown, btnFRUp, btnFRDown, btnFAllUp, btnFAllDown;
    ImageButton btnRLUp, btnRLDown, btnRRUp, btnRRDown, btnRAllUp, btnRAllDown;
    ImageButton btnAllUp, btnAllDown;*/
    ImageButton btnConfig;

    BroadcastReceiver broadcastReceiver;
    BluetoothAdapter bluetoothAdapter;
    ThreadConnected myThreadConnected;

    SharedPreferences sPref;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        v = (Vibrator) this.getSystemService(this.VIBRATOR_SERVICE);

        // проверяем поддержку устройством Bluetooth
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this, "BLUETOOTH NOT support", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Состояние и конфиг
        btnConfig = (ImageButton) findViewById(R.id.btnConfig);
        btnConfig.setOnClickListener(this);

        // register animation
        animAlpha = AnimationUtils.loadAnimation(this, R.anim.anim_alpha);

        imageButtons = new ArrayList<ImageButton>();
        for (int id : BUTTON_IDS){
            ImageButton imageButton = (ImageButton) findViewById(id);
            imageButton.setOnTouchListener(this);
            imageButtons.add(imageButton);
        }

        /*// Кнопки для передней оси
        btnFLUp = (ImageButton) findViewById(R.id.btnFLUp);
        btnFLDown = (ImageButton) findViewById(R.id.btnFLDown);
        btnFRUp = (ImageButton) findViewById(R.id.btnFRUp);
        btnFRDown = (ImageButton) findViewById(R.id.btnFRDown);
        btnFAllUp = (ImageButton) findViewById(R.id.btnFAllUp);
        btnFAllDown = (ImageButton) findViewById(R.id.btnFAllDown);
        // Кнопки для задней оси
        btnRLUp = (ImageButton) findViewById(R.id.btnRLUp);
        btnRLDown = (ImageButton) findViewById(R.id.btnRLDown);
        btnRRUp = (ImageButton) findViewById(R.id.btnRRUp);
        btnRRDown = (ImageButton) findViewById(R.id.btnRRDown);
        btnRAllUp = (ImageButton) findViewById(R.id.btnRAllUp);
        btnRAllDown = (ImageButton) findViewById(R.id.btnRAllDown);
        // Кнопки для всей оси
        btnAllUp = (ImageButton) findViewById(R.id.btnAllUp);
        btnAllDown = (ImageButton) findViewById(R.id.btnAllDown);*/

        /*// Создаем обработчик для передней оси кнопок
        btnFLUp.setOnTouchListener(this);
        btnFLDown.setOnTouchListener(this);
        btnFRUp.setOnTouchListener(this);
        btnFRDown.setOnTouchListener(this);
        btnFAllUp.setOnTouchListener(this);
        btnFAllDown.setOnTouchListener(this);
        // Создаем обработчик для задней оси кнопок
        btnRLUp.setOnTouchListener(this);
        btnRLDown.setOnTouchListener(this);
        btnRRUp.setOnTouchListener(this);
        btnRRDown.setOnTouchListener(this);
        btnRAllUp.setOnTouchListener(this);
        btnRAllDown.setOnTouchListener(this);
        // Создаем обработчик для всей оси
        btnAllUp.setOnTouchListener(this);
        btnAllDown.setOnTouchListener(this);*/

        /* Регистрируем ресивер */
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        broadcastReceiver = new BroadRecStartBlue();
        registerReceiver(broadcastReceiver, intentFilter);

        /* Регистрируем состояние Bluetooth модуля */
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter2.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter2.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter2.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter2.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(broadcastReceiver, filter2);

        //
        ins = this;
        changeAllBtnColor();

        if(myThreadConnected == null) {
            if (bluetoothAdapter.isEnabled()) {
                // Считываем сохраненный MAC адрес устройства
                SharedPreferences sPref = getSharedPreferences("MacFile", MODE_PRIVATE);
                String savedText = sPref.getString(SAVED_TEXT, "");
                // Проверяем, есть ли в памяти MAC адрес
                if(!savedText.equals("")){
                    BluetoothDevice bD = bluetoothAdapter.getRemoteDevice(savedText);

                    //Запускаем ресивер
                    Intent intent = new Intent(START_DEVICE_RECONNECT);
                    intent.putExtra("bluetoothDevice", bD);
                    sendBroadcast(intent);
                }
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            // Запускаем окно с настройками
            case R.id.btnConfig:
                Intent intent = new Intent(this, ConfigsActivity.class);
                startActivity(intent);
                break;
            /*case R.id.btnAllUp:
                view.startAnimation(animAlpha);
                Log.d(ACE_LOG, "Start anim");*/
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int id = view.getId();
        if(myThreadConnected!=null) {
            switch (motionEvent.getAction()){
                // Удерживаем кнопку и отправляем команды
                case MotionEvent.ACTION_DOWN:
                    switch (id){
                        // Открываем клапана для передней оси
                        case R.id.btnFLUp:
                            byte[] bytesToSend = "A".getBytes();
                            myThreadConnected.write(bytesToSend);
                            imageButtons.get(0).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                        case R.id.btnFLDown:
                            byte[] bytesToSend1 = "B".getBytes();
                            myThreadConnected.write(bytesToSend1);
                            imageButtons.get(1).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                        case R.id.btnFRUp:
                            byte[] bytesToSend2 = "C".getBytes();
                            myThreadConnected.write(bytesToSend2);
                            imageButtons.get(2).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                        case R.id.btnFRDown:
                            byte[] bytesToSend3 = "D".getBytes();
                            myThreadConnected.write(bytesToSend3);
                            imageButtons.get(3).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                        case R.id.btnFAllUp:
                            byte[] bytesToSend4 = "E".getBytes();
                            myThreadConnected.write(bytesToSend4);
                            imageButtons.get(4).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                        case R.id.btnFAllDown:
                            byte[] bytesToSend5 = "F".getBytes();
                            myThreadConnected.write(bytesToSend5);
                            imageButtons.get(5).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                        // Открываем клапана для задней оси
                        case R.id.btnRLUp:
                            byte[] bytesToSend6 = "Z".getBytes();
                            myThreadConnected.write(bytesToSend6);
                            imageButtons.get(6).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                        case R.id.btnRLDown:
                            byte[] bytesToSend7 = "Y".getBytes();
                            myThreadConnected.write(bytesToSend7);
                            imageButtons.get(7).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                        case R.id.btnRRUp:
                            byte[] bytesToSend8 = "X".getBytes();
                            myThreadConnected.write(bytesToSend8);
                            imageButtons.get(8).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                        case R.id.btnRRDown:
                            byte[] bytesToSend9 = "W".getBytes();
                            myThreadConnected.write(bytesToSend9);
                            imageButtons.get(9).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                        case R.id.btnRAllUp:
                            byte[] bytesToSend10 = "V".getBytes();
                            myThreadConnected.write(bytesToSend10);
                            imageButtons.get(10).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                        case R.id.btnRAllDown:
                            byte[] bytesToSend11 = "U".getBytes();
                            myThreadConnected.write(bytesToSend11);
                            imageButtons.get(11).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                        // Открываем клапана для всей оси
                        case R.id.btnAllUp:
                            byte[] bytesToSend12 = "K".getBytes();
                            myThreadConnected.write(bytesToSend12);
                            imageButtons.get(12).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                        case R.id.btnAllDown:
                            byte[] bytesToSend13 = "L".getBytes();
                            myThreadConnected.write(bytesToSend13);
                            imageButtons.get(13).setColorFilter(Color.parseColor(getBtnPressedColor()));
                            break;
                    }
                    v.vibrate(200);
                    break;
                case MotionEvent.ACTION_UP:
                    switch (id){
                        // Закрываем клапана для передней оси
                        case R.id.btnFLUp:
                            byte[] bytesToSend = "a".getBytes();
                            myThreadConnected.write(bytesToSend);
                            imageButtons.get(0).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                        case R.id.btnFLDown:
                            byte[] bytesToSend1 = "b".getBytes();
                            myThreadConnected.write(bytesToSend1);
                            imageButtons.get(1).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                        case R.id.btnFRUp:
                            byte[] bytesToSend2 = "c".getBytes();
                            myThreadConnected.write(bytesToSend2);
                            imageButtons.get(2).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                        case R.id.btnFRDown:
                            byte[] bytesToSend3 = "d".getBytes();
                            myThreadConnected.write(bytesToSend3);
                            imageButtons.get(3).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                        case R.id.btnFAllUp:
                            byte[] bytesToSend4 = "e".getBytes();
                            myThreadConnected.write(bytesToSend4);
                            imageButtons.get(4).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                        case R.id.btnFAllDown:
                            byte[] bytesToSend5 = "f".getBytes();
                            myThreadConnected.write(bytesToSend5);
                            imageButtons.get(5).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                        // Закрываем клапана для задней оси
                        case R.id.btnRLUp:
                            byte[] bytesToSend6 = "z".getBytes();
                            myThreadConnected.write(bytesToSend6);
                            imageButtons.get(6).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                        case R.id.btnRLDown:
                            byte[] bytesToSend7 = "y".getBytes();
                            myThreadConnected.write(bytesToSend7);
                            imageButtons.get(7).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                        case R.id.btnRRUp:
                            byte[] bytesToSend8 = "x".getBytes();
                            myThreadConnected.write(bytesToSend8);
                            imageButtons.get(8).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                        case R.id.btnRRDown:
                            byte[] bytesToSend9 = "w".getBytes();
                            myThreadConnected.write(bytesToSend9);
                            imageButtons.get(9).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                        case R.id.btnRAllUp:
                            byte[] bytesToSend10 = "v".getBytes();
                            myThreadConnected.write(bytesToSend10);
                            imageButtons.get(10).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                        case R.id.btnRAllDown:
                            byte[] bytesToSend11 = "u".getBytes();
                            myThreadConnected.write(bytesToSend11);
                            imageButtons.get(11).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                        // Закрываем клапана для всей оси
                        case R.id.btnAllUp:
                            byte[] bytesToSend12 = "k".getBytes();
                            myThreadConnected.write(bytesToSend12);
                            imageButtons.get(12).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                        case R.id.btnAllDown:
                            byte[] bytesToSend13 = "l".getBytes();
                            myThreadConnected.write(bytesToSend13);
                            imageButtons.get(13).setColorFilter(Color.parseColor(getBtnDefaultColor()));
                            break;
                    }
                    break;
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        changeAllBtnColor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // дерегистрируем (выключаем) BroadcastReceiver
        try{
            unregisterReceiver(broadcastReceiver);
        }catch (Exception e){

        }

    }

    // Обновляем статус о подключении
    public void updateStatusDraw(final String t) {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                if (t.equals("1")){
                    btnConfig.setColorFilter(getResources().getColor(R.color.colorConnect));
                }
                if (t.equals("0")){
                    btnConfig.setColorFilter(getResources().getColor(R.color.colorDisconnect));
                    myThreadConnected = null;
                }
            }
        });
    }

    // Ловим наш поток для передачи команд
    @Override
    public void getBluetoothWrite(ThreadConnected myThreadConnected1) {
        myThreadConnected = myThreadConnected1;
        Log.d(ACE_LOG, "getBlue");
    }

    // Меняем цвет кнопок
    public void changeAllBtnColor(){
        // Считываем сохраненный цвет кнопки
        sPref = getSharedPreferences("MacFile", MODE_PRIVATE);
        String savedText = sPref.getString(BTN_DEFAULT, "");
        if(!savedText.equals("")) {
            Log.d(ACE_LOG, savedText);
            for (int i = 0; i < imageButtons.size(); i++){
                imageButtons.get(i).setColorFilter(Color.parseColor(savedText));
            }

        }else{
            for (int i = 0; i < imageButtons.size(); i++){
                imageButtons.get(0).setColorFilter(Color.parseColor(DEFAULT_COLOR));
            }
        }
    }

    // Получаем цвет обычной кнопки
    public String getBtnDefaultColor(){
        sPref = getSharedPreferences("MacFile", MODE_PRIVATE);
        String savedText = sPref.getString(BTN_DEFAULT, "");
        if(!savedText.equals("")) {
            return savedText;
        }else{
            return "#3F51B5";
        }
    }

    // Получаем цвет нажатой кнопки
    public String getBtnPressedColor(){
        sPref = getSharedPreferences("MacFile", MODE_PRIVATE);
        String savedText = sPref.getString(BTN_PRESSED, "");
        if(!savedText.equals("")) {
            return savedText;
        }else{
            return "#3F51B5";
        }
    }

    @Override
    public void blueReconnect(boolean status) {
        Log.d(ACE_LOG, "blueReconnect");
        if(myThreadConnected == null) {
            Log.d(ACE_LOG, "blueReconnect true");
            if (status) {
                if (bluetoothAdapter.isEnabled()) {
                    // Считываем сохраненный MAC адрес устройства
                    SharedPreferences sPref = getSharedPreferences("MacFile", MODE_PRIVATE);
                    String savedText = sPref.getString(SAVED_TEXT, "");
                    // Проверяем, есть ли в памяти MAC адрес
                    if(!savedText.equals("")){
                        BluetoothDevice bD = bluetoothAdapter.getRemoteDevice(savedText);

                        //Запускаем ресивер
                        Intent intent = new Intent(START_DEVICE_RECONNECT);
                        intent.putExtra("bluetoothDevice", bD);
                        sendBroadcast(intent);
                    }
                }
            }
        }
    }
}
