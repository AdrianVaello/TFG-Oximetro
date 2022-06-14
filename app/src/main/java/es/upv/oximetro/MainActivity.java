package es.upv.oximetro;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import es.upv.fastble.BleManager;
import es.upv.fastble.callback.BleGattCallback;
import es.upv.fastble.callback.BleScanCallback;
import es.upv.fastble.data.BleDevice;
import es.upv.fastble.exception.BleException;
import es.upv.oximetro.adapter.DeviceAdapter;
import es.upv.oximetro.comm.ObserverManager;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Variables estaticas
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    public static final String KEY_DATA = "key_data";

    // Variables para la interfaz
    private Button btn_scan;
    private ImageView img_loading;
    private Animation operatingAnim;
    private DeviceAdapter mDeviceAdapter;
    private ProgressDialog progressDialog;
    TextView textoVacio;
    ListView listView_device;
    FloatingActionButton mAddFab, mPacientesFab, mAyudaFab;
    TextView pacientesText, ayudaText, noHayDispositivosList;

    Boolean isAllFabsVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);

        //Comenzamos escaneando dispositivos
        checkPermissionsAndConnect();

        // Se ocultan todos los floating action buttons
        mPacientesFab.setVisibility(View.GONE);
        mAyudaFab.setVisibility(View.GONE);
        pacientesText.setVisibility(View.GONE);
        ayudaText.setVisibility(View.GONE);

        isAllFabsVisible = false;
        // Se añade el evento para hacer click en el fab principal
        mAddFab.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!isAllFabsVisible) {
                            // Se muestran todos los fabs
                            mPacientesFab.show();
                            mAyudaFab.show();
                            pacientesText.setVisibility(View.VISIBLE);
                            ayudaText.setVisibility(View.VISIBLE);

                            isAllFabsVisible = true;
                        } else {
                            // Se ocultan todos los fabs
                            mPacientesFab.hide();
                            mAyudaFab.hide();
                            pacientesText.setVisibility(View.GONE);
                            ayudaText.setVisibility(View.GONE);

                            isAllFabsVisible = false;
                        }
                    }
                });

        // Se añade el click al boton de ayuda
        mAyudaFab.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this,ActivityAyuda.class);
                        startActivity(intent);
                    }
                });

        // Se añade el click al boton de pacientes
        mPacientesFab.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this, ListaUsuarios.class);
                        startActivity(intent);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        showConnectedDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
    }

    /* -------------------------------------
    Función para el click del boton de escaneo
    Params: vista de la pantalla
    ---------------------------------------*/
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                if (btn_scan.getText().equals(getString(R.string.start_scan))) {
                    checkPermissionsAndConnect();
                } else if (btn_scan.getText().equals(getString(R.string.stop_scan))) {
                    BleManager.getInstance().cancelScan();
                }
                break;
        }
    }

    /* -------------------------------------
    Función para inicializar las variables de la interfaz a los objetos
    ---------------------------------------*/
    private void initView() {

        textoVacio= findViewById(R.id.tvNoDispositivosEncontrados);

        btn_scan = (Button) findViewById(R.id.btn_scan);
        btn_scan.setText(getString(R.string.start_scan));
        btn_scan.setOnClickListener(this);

        img_loading = (ImageView) findViewById(R.id.img_loading);
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        operatingAnim.setInterpolator(new LinearInterpolator());
        progressDialog = new ProgressDialog(this);

        mAddFab = findViewById(R.id.add_fab);
        mPacientesFab = findViewById(R.id.pacientes_fab);
        mAyudaFab = findViewById(R.id.ayuda_fab);
        pacientesText = findViewById(R.id.pacientes_action_text);
        ayudaText = findViewById(R.id.ayuda_action_text);

        mDeviceAdapter = new DeviceAdapter(this);
        mDeviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onConnect(BleDevice bleDevice) {
                if (!BleManager.getInstance().isConnected(bleDevice)) {
                    BleManager.getInstance().cancelScan();
                    connect(bleDevice);
                }
            }

            @Override
            public void onDisConnect(final BleDevice bleDevice) {
                if (BleManager.getInstance().isConnected(bleDevice)) {
                    BleManager.getInstance().disconnect(bleDevice);
                }
            }

            @Override    // Entramos en el dispositivo
            public void onDetail(BleDevice bleDevice) {
                goShowDataActivity(bleDevice);
            }
        });

        listView_device = (ListView) findViewById(R.id.list_device);
        listView_device.setAdapter(mDeviceAdapter);
        //Añadimos texto si esta vacia la lista de dispositivos
        textoVacio.setText(R.string.no_se_han_encontrado_dispositivos);
        listView_device.setEmptyView(textoVacio);
        noHayDispositivosList = findViewById(R.id.noHayDispositivosList);
      }

    /* -------------------------------------
    Función para ir a la pagina de mostrar datos
    Params: dispositivo BLE
    ---------------------------------------*/
    private void goShowDataActivity(BleDevice bleDevice) {
        if (BleManager.getInstance().isConnected(bleDevice)) {
            Intent intent = new Intent(MainActivity.this, ShowDataActivity.class);
            intent.putExtra(KEY_DATA, bleDevice);
            startActivity(intent);
        }
    }

    /* -------------------------------------
    Función para mostrar los dispositivos conectados
    ---------------------------------------*/
    private void showConnectedDevice() {
        List<BleDevice> deviceList = BleManager.getInstance().getAllConnectedDevice();
        mDeviceAdapter.clearConnectedDevice();
        for (BleDevice bleDevice : deviceList) {
            mDeviceAdapter.addDevice(bleDevice);
        }
        mDeviceAdapter.notifyDataSetChanged();
    }

    /* -------------------------------------
    Función que controla que hacer cuando se inicia el escaneo
    ---------------------------------------*/
    private void startScan() {
            BleManager.getInstance().scan(new BleScanCallback() {
                @Override
                public void onScanStarted(boolean success) {
                    mDeviceAdapter.clearScanDevice();
                    mDeviceAdapter.notifyDataSetChanged();
                    img_loading.startAnimation(operatingAnim);
                    img_loading.setVisibility(View.VISIBLE);
                    btn_scan.setText(getString(R.string.stop_scan));
                }

                @Override
                public void onLeScan(BleDevice bleDevice) {
                    super.onLeScan(bleDevice);
                }

                @Override
                public void onScanning(BleDevice bleDevice) {
                    if (bleDevice!=null && "HJ-Narigmed".equals(bleDevice.getName())) {

                        mDeviceAdapter.addDevice(bleDevice);
                        mDeviceAdapter.notifyDataSetChanged();
                    }
                    if(listView_device.getAdapter() != null){
                        //De esta manera sabes si tu RecyclerView está vacío
                        if(listView_device.getCount() != 0) {
                            //Aquí muestras el mensaje
                            noHayDispositivosList.setVisibility(View.GONE);
                        }else{
                            noHayDispositivosList.setVisibility(View.VISIBLE);
                        }

                    }
                }

                @Override
                public void onScanFinished(List<BleDevice> scanResultList) {
                    img_loading.clearAnimation();
                    img_loading.setVisibility(View.INVISIBLE);
                    btn_scan.setText(getString(R.string.start_scan));
                }
            });
        }

    /* -------------------------------------
    Función para conectar conel dispositivo
    Params: Dispositivo BLE
    ---------------------------------------*/
    private void connect(final BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                progressDialog.show();
            }

            // Si la conexion falla
            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                btn_scan.setText(getString(R.string.start_scan));
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, getString(R.string.connect_fail), Toast.LENGTH_LONG).show();

            }

            // Si la conexion es correcta
            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                progressDialog.dismiss();
                mDeviceAdapter.addDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
                goShowDataActivity(bleDevice);

            }

            // Cuando se desconecta del dispositivo
            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                progressDialog.dismiss();

                mDeviceAdapter.removeDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();

                if (isActiveDisConnected) {
                    Toast.makeText(MainActivity.this, getString(R.string.active_disconnected), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.disconnected), Toast.LENGTH_LONG).show();
                    //Log.d(TAG, "Antes de quidrar" + Utilities.datosPulsioximetro);
                    Intent intent = new Intent(MainActivity.this, DownloadExcel.class);
                    startActivity(intent);
                    ObserverManager.getInstance().notifyObserver(bleDevice);
                }
            }
        });
    }

    /*private void readRssi(BleDevice bleDevice) {
        BleManager.getInstance().readRssi(bleDevice, new BleRssiCallback() {
            @Override
            public void onRssiFailure(BleException exception) {
                Log.i(TAG, "onRssiFailure" + exception.toString());
            }

            @Override
            public void onRssiSuccess(int rssi) {
                Log.i(TAG, "onRssiSuccess: " + rssi);
            }
        });
    }

    private void setMtu(BleDevice bleDevice, int mtu) {
        BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {
                Log.i(TAG, "onsetMTUFailure" + exception.toString());
            }

            @Override
            public void onMtuChanged(int mtu) {
                Log.i(TAG, "onMtuChanged: " + mtu);
            }
        });
    }*/

    /* -------------------------------------
    Función para pedir los permisos necesarios
    ---------------------------------------*/
    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
        }
    }

    /* -------------------------------------
    Función que comprueba si los permisos estan correctos
    ---------------------------------------*/
    private void checkPermissionsAndConnect() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    /* -------------------------------------
    Función para pedir el permiso de ubicacion (si no se ha aceptado antes) y iniciar el primer escaneo
    ---------------------------------------*/
    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    startScan();
                }
                break;
        }
    }

    /* -------------------------------------
    Función para comprobar si la ubicacion esta activado
    ---------------------------------------*/
    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    /* -------------------------------------
    Función que empieza a escanear  si la el permiso de ubicacion esta activado
    ---------------------------------------*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                //setScanRule();
                startScan();
            }
        }
    }
}
