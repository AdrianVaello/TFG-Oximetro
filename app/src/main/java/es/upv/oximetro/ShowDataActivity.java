package es.upv.oximetro;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.inputmethodservice.Keyboard;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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

   public ArrayList<Double> datosGrafica= new ArrayList<Double>();
   public ArrayList<Double> datosMayorMenor= new ArrayList<Double>();

   TextView tvPVi;

   Long tiempoInicial;
   Long tiempoFinal;

   int cicloMuestras=4000;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_show_data);
      initView();
      prepareDeviceServiceCharact();

      tiempoInicial=System.currentTimeMillis();

   }



   //Variables globales para representar la gráfica
   //Se mostrarán los últimos 5 segundos (SHOW_TIME)
   //El refresco de la gráfica se realiza con un barrido de izq. a der.
   //similar a los electrocardiogramas antiguos
   //La gráfica se representa en dos partes, la izq. y la der.
   //(entriesLeft y entriesRight)

   public static final int SHOW_TIME = 5000;   // Tiempo a mostrar en la gráfica (mseg)
   public static final int SAMPLE_PERIOD = 22; // Cada cuantos mseg llega una muestra
   public static final int SAMPLES_IN_GRAPH = SHOW_TIME / SAMPLE_PERIOD; // # de muestras en la gráfiva (227)

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

   private void newRawData(float value, long time) {
      //Llega un nuevo dato a visualizar
      long relativeTime = time % SHOW_TIME; //Se representa el módulo 4000 del tiempo
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
      tvPVi=findViewById(R.id.tvPVi);
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
                    Log.d(TAG, "Valores: "+ Arrays.toString(data));
                    //Log.d(TAG, "Valores :"+System.currentTimeMillis() + "; " + HexUtil.formatHexString(characteristic.getValue(), true));
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
            float numeroDecimal = (~data[3]) + 1;
            Log.d("Byte data", ",,,,ddata "+ Arrays.toString(data) + " size "+data.length);
            tiempoFinal=System.currentTimeMillis();
            if(data[3] !=0){
                  datosGrafica.add((double) numeroDecimal);

               if(tiempoInicial+cicloMuestras<=tiempoFinal){
                  calcularPVi(datosGrafica);
                  tiempoInicial=System.currentTimeMillis();
                  //datosGrafica.clear();
               }
            }

            //****************************************************************
            // Hay que probar de pasar el byte[3] que está en complemento a 2
            // a decimal para enviarlo a la gráfica
            //

            //****************************************************************

            //Datos para mostrar la grafica
            //newRawData(data[3], System.currentTimeMillis());

            //*******************************************************
            // con numeroDecimal (sin complemento a 2)
            //*******************************************************
            newRawData(numeroDecimal, System.currentTimeMillis());
            //*******************************************************

         } else if (data[1] == 11 && (data[2] == -127/*0x81*/)) {
            //Log.d("Dato chart", ",,,,data2 "+data[8]);
            int spO2 = data[3] & 0xff;

            //***********************************************************************************
            //********************* OJO POSIBLE ERROR EN PR *************************************
            //***********************************************************************************
            // según el github de Jesús Tomás.....
            // PR/min – Pulsaciones por minuto. Se codifica en dos bytes.
            // Para obtener el valor hay que multiplicar el 2º byte por 256 y sumarle el primero.
            // Valores típicos entre 60 y 100.
            //***********************************************************************************
            // Corrección:
            //int pr = (data[4] & 0xff) + (data[5] & 0xff) * 256;
            //***********************************************************************************
            int pr = data[4] & 0xff;

            int rr = data[6];

            //***************************************************************************************
            //********************* OJO POSIBLE ERROR EN PI *****************************************
            //***************************************************************************************
            // según el github de Jesús Tomás.....
            // PI (%) - El índice de perfusión: es la proporción entre el flujo de sangre no pulsátil
            // y el pulsátil a través del lecho capilar periférico. Se codifica en dos bytes.
            // Para obtener el valor hay que multiplicar el 2º byte por 256 y sumarle el primero
            // y luego dividir ente 1000. Valores observados 7.00% a 14.00%
            //***************************************************************************************
            // Corrección:
            //int pi = ((data[7] & 0xff) + (data[8] & 0xff) * 256) / 1000;
            //***************************************************************************************
            int pi = (data[7] & 0xff) + (data[8] & 0xff) * 256;

            //******************************************************
            // VALOR IRRELEVANTE DE unk, SE PUEDE AHORRAR EL CÁLCULO
            //******************************************************
            int unk = (data[9] & 0xff) + (data[10] & 0xff) * 256;

            s += ", " + spO2 + ", " + pr + ", " + rr + ", " + pi + ", " + unk + "\n";
            //Log.d("Dato chart", ",,,,Data1 "+data[1]+",,,,Data2 "+data[2]+",,,,Data3 "+data[3]+",,,,Data4 "+data[4]+",,,,Data5 "+data[5]);
            //Log.d("Dato chart", "77pi "+pi);

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

   public void calcularPVi(ArrayList<Double> datos){
      Log.d("Dato chart", "////An"+datos);

      //ArrayList<Double> arrayPVmax= new ArrayList<Double>();
      //ArrayList<Double> arrayPVmin= new ArrayList<Double>();
      ArrayList<Double> arrayAmplitudes= new ArrayList<Double>();
      //Double amplitud;

      Double maxAmp=Double.NEGATIVE_INFINITY;
      Double minAmp=Double.POSITIVE_INFINITY;

      //Double maximoRelativo;
      //Double minimoRelativo;
      //Double amplitudMax;
      //Double amplitudMin;

      Queue<Double> cola = new LinkedList<Double>();
      //Queue<Double> colaAux= new LinkedList<Double>();

      // *******IMPORTANTE: comprobar tamaño óptimo de la ventana deslizante
      int tamanyoVentana=12;

      //ArrayList<Double> ventanaCalculo= new ArrayList<>();
      //ArrayList<Double> ventanaCalculoAux= new ArrayList<>();
      Boolean subiendo=false;
      // int contadorPrimeraVez=0;
      Boolean contadorPrimeraVez=true;
      Boolean contadorInicializarTendencia=true;

      //Double valorSuma=0.0;
      Double valorSumaAux=0.0;

      /*for (int i=0; i<datos.size();i++){

         cola.add(datos.get(i));
         int posicion=0;

        // Log.d("Dato chart", "//// Ventana"+ ventana);
         if(i>=tamanyoVentana){

            List ventana = new ArrayList(cola);

            for (int j=0; j<ventana.size();j++){
               ventanaCalculo.add((Double) ventana.get(j));
               //Log.d("Dato chart", "//// Ventana1"+ ventanaCalculo);
            }
            if(contadorPrimeraVez==0){
               if(ventanaCalculo.get(0) > ventanaCalculo.get(ventanaCalculo.size()-1)){
                  //Posible bajando=false
                  subiendo=false;


               }else if(ventanaCalculo.get(0) < ventanaCalculo.get(ventanaCalculo.size()-1)){
                  subiendo=true;
               }

               contadorPrimeraVez++;
            }else{
               for (int k=0;k<ventanaCalculo.size();k++){
                  if(ventanaCalculo.get(0) > ventanaCalculo.get(ventanaCalculo.size()-1)){

                     //subiendo=false;
                     if(k!=ventanaCalculo.size()-1){
                        if(ventanaCalculo.get(k)<ventanaCalculo.get(k+1) && !subiendo){
                           //esta bajando pero cn altibajos
                           if(ventanaCalculo.get(k)<minimoRelativo){
                              minimoRelativo=ventanaCalculo.get(k);
                              posicion=k;
                              Log.d("Dato chart", "//// i= "+i+" posicion=" +posicion);
                              for(int l=i+posicion;l<i+posicion+tamanyoVentana;l++){
                                 colaAux.add(datos.get(l));

                                 if(l>=i+posicion+tamanyoVentana){
                                    Log.d("Dato chart", "//// COla aux= "+colaAux);
                                    List ventanaAux = new ArrayList(colaAux);
                                    Log.d("Dato chart", "//// ventanaAux= "+ventanaAux);
                                    for (int j=0; j<ventanaAux.size();j++){
                                       ventanaCalculoAux.add((Double) ventanaAux.get(j));
                                    }
                                    Log.d("Dato chart", "//// ventanaCalculoAux= "+ventanaCalculoAux);
                                    if(l!=i+posicion+9) {
                                       for (int n = 0; n < ventanaCalculoAux.size(); n++) {
                                          if (ventanaCalculoAux.get(n) > ventanaCalculoAux.get(n + 1)) {
                                             max = maximoRelativo;
                                          } else if (ventanaCalculoAux.get(n) < ventanaCalculoAux.get(n + 1)) {
                                             min = minimoRelativo;
                                          }
                                       }
                                    }
                                 }

                              }
                           }
                           //subiendo=false;
                           //Log.d("Dato chart", "//// Bajando pero con altibajos");
                        }else if(ventanaCalculo.get(k) > ventanaCalculo.get(k+1) && !subiendo){
                           //esta bajando correctamente
                           //Log.d("Dato chart", "//// Bajando correctamente");
                           subiendo=false;
                        }
                     }


                  }else if(ventanaCalculo.get(0) < ventanaCalculo.get(ventanaCalculo.size()-1)){
                     //subiendo=true;


                     if(k!=ventanaCalculo.size()-1){
                        if(ventanaCalculo.get(k)>ventanaCalculo.get(k+1) && subiendo){
                           //Esta subiendo pero ha tenido un pequeño altibajo
                           //subiendo=true;
                           //Log.d("Dato chart", "//// Subiendo pero con altibajos");
                           if(ventanaCalculo.get(k)>maximoRelativo){
                              maximoRelativo=ventanaCalculo.get(k);
                              posicion=k;
                           }

                           for(int l=i+posicion;l<i+posicion+tamanyoVentana;l++){
                              colaAux.add(datos.get(l));
                              if(l>=i+posicion+tamanyoVentana){
                                 List ventanaAux = new ArrayList(colaAux);
                                 Log.d("Dato chart", "//// ventanaAux= "+ventanaAux);
                                 for (int j=0; j<ventanaAux.size();j++){
                                    ventanaCalculoAux.add((Double) ventanaAux.get(j));
                                 }
                                 Log.d("Dato chart", "//// ventanaCalculoAux= "+ventanaCalculoAux);
                                 if(l!=i+posicion+9){
                                    for(int n=0;n<ventanaCalculoAux.size();n++){
                                       if(ventanaCalculoAux.get(n)>ventanaCalculoAux.get(n+1)){
                                          max=maximoRelativo;
                                       }else if(ventanaCalculoAux.get(n)<ventanaCalculoAux.get(n+1)){
                                          min=minimoRelativo;
                                       }
                                    }
                                 }
                              }

                           }


                        }else if(ventanaCalculo.get(k)<ventanaCalculo.get(k+1) && subiendo){
                           //Sube correctamente
                           //Log.d("Dato chart", "//// Subiendo correctamente");
                           subiendo=true;

                        }
                     }
                  }
               }
            }
            cola.remove();
         }

         Log.d("Dato chart", "//// Valores PVI Subiendo="+subiendo+ " maximo="+max+" minimo="+min);
      }*/


      // obtengo los datos leídos
      for (int i=0; i<datos.size();i++){
         Log.d("Dato chart", "////Inicio for");

         // voy añadiendo el dato a la cola (FIFO)
         cola.add(datos.get(i));

         // empiezo a rellenar la ventana y calcular valores cuando tengo (tamaño datos leidos)==(tamaño ventana)
         if(i>=tamanyoVentana-1){
            ArrayList<Double> ventana = new ArrayList(cola);
            // ordeno la ventana
            Collections.sort(ventana);
            // obtengo max y min de la ventana
            Double maximoRelativo=ventana.get(ventana.size()-1);
            Double minimoRelativo=ventana.get(0);
            // actualizo minAmp y maxAmp
            if(minimoRelativo<minAmp){
               minAmp=minimoRelativo;
            }
            if(maximoRelativo>maxAmp){
               maxAmp=maximoRelativo;
            }
            // calculo la suma de los datos de la ventana
            Double valorSuma=sumaVentana(ventana);

            // si es la primera ventana que tengo inicializo valorSumaAux
            if(contadorPrimeraVez){
               valorSumaAux=valorSuma;
               contadorPrimeraVez=false;
            }else{
               //Log.d("Dato chart", "//// Ventana: " + ventana +" Suma: "+valorSuma +" Suma aux:" + valorSumaAux );

               // como ya tengo dos sumas de ventanas compruebo la dirección de la curva la primera vez
               if(contadorInicializarTendencia){
                  if (valorSuma>valorSumaAux) {
                     subiendo=true;
                  } else {
                     subiendo=false;
                  }
                  contadorInicializarTendencia=false;
               }

               // calculo la amplitud cuando cambia la tendencia
               if (valorSuma>valorSumaAux && !subiendo) {

                  //*******************************************************************************************************************
                  // comprobar si hay que introducir el valor absoluto de la resta -> Math.abs(), para que no haya amplitudes negativas
                  Log.d("Dato chart", "//// maxAmp-minAmp: " + (maxAmp-minAmp) + " abs(maxAmp-minAmp): " + Math.abs(maxAmp-minAmp));
                  //*******************************************************************************************************************

                  arrayAmplitudes.add(maxAmp-minAmp);
                  maxAmp=Double.NEGATIVE_INFINITY;
                  minAmp=Double.POSITIVE_INFINITY;
                  subiendo=true;
               }
               if (valorSuma<valorSumaAux) {
                  subiendo=false;
               }

               // actualizo valorSumaAux con la suma actual
               valorSumaAux=valorSuma;

               /*if(valorSuma>valorSumaAux &&!subiendo){
                  valorSumaAux=valorSuma;

                  arrayAmplitudes.add(maxAmp-minAmp);
                  maxAmp=Double.NEGATIVE_INFINITY;
                  minAmp=Double.POSITIVE_INFINITY;
                  subiendo=true;

               }else if(valorSuma>valorSumaAux &&subiendo){
                  valorSumaAux=valorSuma;
                 // subiendo=true;
               }else {
                  valorSumaAux=valorSuma;
                  subiendo=false;
               }*/

            }

            Log.d("Dato chart", "//// " + " Max: "+maximoRelativo + " Min: "+minimoRelativo +
                    " MaxAMp "+maxAmp+ " MinAmp "+minAmp+ " Amplitud "+arrayAmplitudes +" Subiendo: " +subiendo);

            // elimino primer elemento de la cola
            cola.remove();
         }
      }

      // ordeno array de amplitudes obtenidas
      Collections.sort(arrayAmplitudes);
      // obtengo la amplitud máxima y mínima
      Double amplitudMax=arrayAmplitudes.get(arrayAmplitudes.size()-1);
      Double amplitudMin=arrayAmplitudes.get(0);

      //                PImax - PImin
      // obtengo PVI = --------------- x 100
      //                    PImax
      Double PVI= ((amplitudMax-amplitudMin)/amplitudMax)*100;
      // añado resultado a la app
      anadirTextoPVi(String.format("%.2f", PVI));

      Log.d("Dato chart", "//// PVI= " +PVI + "ArrayA: "+ arrayAmplitudes+ " max: " +amplitudMax+ " min: "+ amplitudMin);

      /*if(arrayAmplitudes.size()!=0){
         PvmaxMin=arrayAmplitudes.get(0);
         PvmaxMax=arrayAmplitudes.get(0);

         for (int j=0;j<arrayAmplitudes.size();j++){
            if(arrayAmplitudes.get(j)>PvmaxMax){
               PvmaxMax=arrayAmplitudes.get(j);
            }
            if(arrayAmplitudes.get(j)<PvmaxMin){
               PvmaxMin=arrayAmplitudes.get(j);
            }
         }
      }

      Log.d("Dato chart", "////Max"+PvmaxMax+"Min "+PvmaxMin);
      double Pvi=  ((float) (PvmaxMax - PvmaxMin) / PvmaxMax) * 100;
      Log.d("Dato chart", "//// PVi"+Pvi);
      anadirTextoPVi(String.format("%.2f", Pvi));*/

      // limpio los datos de la gràfica
      datosGrafica.clear();
   }

   public void anadirTextoPVi(String texto){
      tvPVi.setText(texto);
   }

   private Double sumaVentana(ArrayList<Double> ventana){
      Double suma=0.0;
      for (int i=0;i<ventana.size();i++){
         suma+=ventana.get(i);
      }
      return suma;
   }

}
