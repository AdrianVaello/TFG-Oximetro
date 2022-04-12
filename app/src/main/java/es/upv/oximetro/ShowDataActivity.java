package es.upv.oximetro;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.inputmethodservice.Keyboard;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import androidx.appcompat.app.AppCompatActivity;
import es.upv.fastble.BleManager;
import es.upv.fastble.callback.BleNotifyCallback;
import es.upv.fastble.data.BleDevice;
import es.upv.fastble.exception.BleException;
import es.upv.fastble.utils.HexUtil;
import es.upv.oximetro.operation.CharacteristicOperationFragment;
import es.upv.oximetro.operation.OperationActivity;

import static java.lang.String.valueOf;
import static java.util.Arrays.copyOfRange;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

//DONE: Añadir botón para grabar datos
//TODO: Añadir ToolBar

public class ShowDataActivity extends AppCompatActivity {

   private static final String TAG = MainActivity.class.getSimpleName();
   public static final String KEY_DATA = "key_data";

   private BleDevice bleDevice;
   private BluetoothGattService bluetoothGattService;
   private BluetoothGattCharacteristic characteristic;

   private TextView tv_spo2, tv_pr, tv_rr, tv_pi;
   private LineChart chart;

   //DONE: Escribir datos en fichero CSV
   private long time;
   private String fileName;
   private FileOutputStream f1, f2;




   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_show_data);
      initView();
      prepareDeviceServiceCharact();

   }



   //Variables globales para representar la gráfica
   //Se mostrarán los últimos 5 segundos (SHOW_TIME)
   //El refresco de la gráfica se realiza con un barrido de izq. a der.
   //similar a los electrocardiogramas antiguos
   //La gráfica se representa en dos partes, la izq. y la der.
   //(entriesLeft y entriesRight)

   public static final int SHOW_TIME = 5000;   // Tiempo a mostrar en la gráfica (mseg)
   public static final int SAMPLE_PERIOD = 22; // Cada cuantos mseg llega una muestra
   public static final int SAMPLES_IN_GRAPH = SHOW_TIME / SAMPLE_PERIOD;
   // # de muestras en la gráfiva (227)
   List<Entry> entriesLeft = new ArrayList<>();
   List<Entry> entriesRight = new ArrayList<>();
   long startTime = System.currentTimeMillis();

   private void showChart() {
      LineDataSet rightDataSet = new LineDataSet(entriesRight, "");
      rightDataSet.setDrawCircles(false);
      rightDataSet.setDrawValues(false);
      rightDataSet.setLineWidth(2);
      rightDataSet.setColor(R.color.colorTexto);
      LineDataSet leftDataSet = new LineDataSet(entriesLeft, "");
      leftDataSet.setDrawCircles(false);
      leftDataSet.setDrawValues(false);
      leftDataSet.setLineWidth(3);
      leftDataSet.setColor(R.color.colorTexto);
      LineData lineData = new LineData(leftDataSet, rightDataSet);
      chart.setData(lineData);
      chart.invalidate(); // refresh
      chart.setBorderColor(R.color.colorTextoBlanco);
   }

   private void newRawData(int value, long time) {
      //Llega un nuevo dato a visualizar
      long relativeTime = time % SHOW_TIME; //Se representa el módulo 5000 del tiempo
      //Cuando nos salimos por la derecha ...
      //(Lo sabemos porque el módulo del tiempo < que el último añadido)
      if (entriesLeft.size() > 0 &&
              relativeTime < entriesLeft.get(entriesLeft.size() - 1).getX()) {
         //Intercambiamos parte derecha y parte izquierda
         List<Entry> temp = entriesRight;
         entriesRight = entriesLeft;
         entriesLeft = temp;
      }
      //El nuevo dato se añade al final de la parte izq
      entriesLeft.add(new Entry(relativeTime, value));
      //TODO: Eliminar SAMPLES_IN_GRAPH y dejar un espacio constante de tiempo entre la parte izq. y der.
      //Eliminamos muestras (normalmente de la parte rerecha)
      //de forma que el número total de muestras se mantenga constante en SAMPLES_IN_GRAPH
      while (entriesLeft.size() + entriesRight.size() > SAMPLES_IN_GRAPH) {
         if (entriesRight.size() > 0) {
            entriesRight.remove(0);
         } else {
            entriesLeft.remove(0);
         }
      }
      showChart();
   }

   private void initView() {
      /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);
      btn_scan = (Button) findViewById(R.id.btn_scan);
      btn_scan.setText(getString(R.string.start_scan));
      btn_scan.setOnClickListener(this);*/
      tv_spo2 = (TextView) findViewById(R.id.tv_spo2);
      tv_pr = (TextView) findViewById(R.id.tv_pr);
      tv_rr = (TextView) findViewById(R.id.tv_rr);
      tv_pi = (TextView) findViewById(R.id.tv_pi);
      //GRÁFICA
      chart = (LineChart) findViewById(R.id.chart_heart);
      XAxis xAxis = chart.getXAxis();
      xAxis.setAxisMinimum(0);
      xAxis.setAxisMaximum(5000);
      xAxis.setDrawLabels(false);
      chart.getAxisLeft().setDrawLabels(false);
      chart.getAxisLeft().setAxisMinimum(-50);
      chart.getAxisLeft().setAxisMaximum(50);
      chart.getAxisLeft().setEnabled(false);
      chart.getAxisRight().setEnabled(false);
      chart.getLegend().setEnabled(false);
      chart.getDescription().setEnabled(false);
      chart.setDrawBorders(false);
   }

   private void prepareDeviceServiceCharact() {
      //Se inicializan las variables: bleDevice, bluetoothGattService y characteristic
      //a las que nos hemos de conectar para que nos envie los datos
      bleDevice = getIntent().getParcelableExtra(KEY_DATA);
      if (bleDevice == null)
         finish();
      BluetoothGatt gatt = BleManager.getInstance().getBluetoothGatt(bleDevice);
      for (BluetoothGattService service : gatt.getServices()) {
         if (service.getUuid().toString().equals("0000fff0-0000-1000-8000-00805f9b34fb")) {
            bluetoothGattService = service;
            for (BluetoothGattCharacteristic charact : service.getCharacteristics())
               if (charact.getUuid().toString().equals("0000fff1-0000-1000-8000-00805f9b34fb"))
                  characteristic = charact;
         }
      }
   }

   @Override
   public void onResume() {
      super.onResume();
      showData();
   }

   public void showData() {
      final int charaProp = CharacteristicOperationFragment.PROPERTY_NOTIFY;
      //tv_log.setMovementMethod(ScrollingMovementMethod.getInstance());
      BleManager.getInstance().notify(
              bleDevice,
              characteristic.getService().getUuid().toString(),
              characteristic.getUuid().toString(),
              new BleNotifyCallback() {
                 @Override
                 public void onNotifySuccess() {
                 }

                 @Override
                 public void onNotifyFailure(final BleException exception) {
                    Log.e(TAG, exception.toString());
                 }

                 @Override
                 public void onCharacteristicChanged(byte[] data) {
                    //Log.d(TAG, System.currentTimeMillis() + "; " + HexUtil.formatHexString(characteristic.getValue(), true));
                   writeFrames(data);
                 }
              });
   }

   void writeFrames(byte[] data) {
      time = System.currentTimeMillis();
      writeSingleFrame(data);
      if (data.length > data[1]) {   //data[1] is length of frame
         writeSingleFrame(copyOfRange(data, data[1], data.length));
      }
   }

   void writeSingleFrame(byte[] data) {
      String s = "" + time;
      try {
         if ((data[1] == 6) && (data[2] == -128/*0x80*/)) {
            int unsignedbyte = data[5] & 0xff;
            s += ", " + data[3] + ", " + data[4] + ", " + unsignedbyte + "\n";
            if (fileName!=null){
               f1.write(s.getBytes());
            }
            Log.d("Dato chart", ",,,,"+String.valueOf(data[3]));
            newRawData(data[3], System.currentTimeMillis());
         } else if (data[1] == 11 && (data[2] == -127/*0x81*/)) {
            int spO2 = data[3] & 0xff;
            int pr = data[4] & 0xff;
            int rr = data[6];
            int pi = (data[7] & 0xff) + (data[8] & 0xff) * 256;
            int unk = (data[9] & 0xff) + (data[10] & 0xff) * 256;
            s += ", " + spO2 + ", " + pr + ", " + rr + ", " + pi + ", " + unk + "\n";

            if(spO2!=0 || pr!=0 || rr!=0 || pi!=0) {
               HashMap<String, Integer> hashDatos = new HashMap<String, Integer>();

               hashDatos.put("Sp02", spO2);
               hashDatos.put("Pr", pr);
               hashDatos.put("Rr", rr);
               hashDatos.put("Pi", pi);

               Utilities.datosPulsioximetro.add(hashDatos);
               //Log.d(TAG, "////////////Lista" + Utilities.datosPulsioximetro);
            }

            tv_spo2.setText(valueOf(spO2));
            tv_pr.setText(valueOf(pr));
            tv_rr.setText(valueOf(rr));
            //           double d = pi/1000;
            tv_pi.setText(String.format("%.2f", (0.0 + pi) / 1000));
            if (fileName!=null) f2.write(s.getBytes());
         } else {
            Log.e(TAG, "ERROR: Trama desconocida");
         }
      } catch (FileNotFoundException e) {
         Log.e(TAG, "ERROR: Abriendo fichero " + e.toString());
      } catch (IOException e) {
         Log.e(TAG, "ERROR: Escribiendo fichero " + e.toString());
      }
   }

   @Override
   public void onStop() {
      try {
         if (f1 != null) f1.close();
         if (f2 != null) f2.close();
      } catch (IOException e) {
         Log.e(TAG, "ERROR: Cerrando fichero " + e.toString());
      }
      super.onStop();
   }

   @Override
   protected void onDestroy() {
      super.onDestroy();
      BleManager.getInstance().disconnectAllDevice();
      BleManager.getInstance().destroy();
   }

}
