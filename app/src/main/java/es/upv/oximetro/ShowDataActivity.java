package es.upv.oximetro;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import androidx.appcompat.app.AppCompatActivity;
import es.upv.fastble.BleManager;
import es.upv.fastble.callback.BleNotifyCallback;
import es.upv.fastble.data.BleDevice;
import es.upv.fastble.exception.BleException;
import static java.lang.String.valueOf;
import static java.util.Arrays.copyOfRange;

public class ShowDataActivity extends AppCompatActivity {

   private static final String TAG = MainActivity.class.getSimpleName();
   public static final String KEY_DATA = "key_data";

   private BleDevice bleDevice;
   private BluetoothGattService bluetoothGattService;
   private BluetoothGattCharacteristic characteristic;

   private TextView tv_spo2, tv_pr, tv_rr, tv_pi, tv_Cargando_Pvi, tv_PmaxPmin, tv_Cisura, tv_Area;
   private LineChart chart;

   private long time;
   private String fileName;
   private FileOutputStream f1, f2;

   public ArrayList<Double> datosGrafica = new ArrayList<Double>();

   TextView tvPVi;

   Long tiempoInicial;
   Long tiempoFinal;

   int cicloMuestras = 4000;

   public Double PVI = 0.0;

   public boolean primeraVezCalculoPVi;

   YAxis yAxis;
   String tipoVaso;

   //************************************
   Double maximoCisura = Double.NEGATIVE_INFINITY;
   Double area=0.0;

   //Variables globales para representar la gráfica
   //Se mostrarán los últimos 5 segundos (SHOW_TIME)
   //El refresco de la gráfica se realiza con un barrido de izq. a der.
   //similar a los electrocardiogramas antiguos
   //La gráfica se representa en dos partes, la izq. y la der.
   //(entriesLeft y entriesRight)

   public static final int SHOW_TIME = 5000;   // Tiempo a mostrar en la gráfica (mseg)
   public static final int SAMPLE_PERIOD = 22; // Cada cuantos mseg llega una muestra
   public static final int SAMPLES_IN_GRAPH = SHOW_TIME / SAMPLE_PERIOD; // # de muestras en la gráfica (227)

   List<Entry> entriesLeft = new ArrayList<>();
   List<Entry> entriesRight = new ArrayList<>();
   long startTime = System.currentTimeMillis();

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_show_data);
      initView();
      prepareDeviceServiceCharact();

      tiempoInicial = System.currentTimeMillis();
      primeraVezCalculoPVi = true;
      tv_Cargando_Pvi.setText(R.string.cargando_datos_pvi);
   }

   private void showChart() {

      LineDataSet rightDataSet = new LineDataSet(entriesRight, "");
      rightDataSet.setDrawCircles(false);
      rightDataSet.setDrawValues(false);
      rightDataSet.setLineWidth(2);
      rightDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
      rightDataSet.setColor(R.color.colorTexto);
      LineDataSet leftDataSet = new LineDataSet(entriesLeft, "Spo2 (mmHg)");
      leftDataSet.setDrawCircles(false);
      leftDataSet.setDrawValues(false);
      leftDataSet.setLineWidth(3f);
      leftDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
      leftDataSet.setColor(R.color.colorTexto);
      LineData lineData = new LineData(leftDataSet, rightDataSet);
      chart.setData(lineData);
      chart.invalidate(); // refresh
      chart.setBorderColor(R.color.colorTextoBlanco);
   }

   private void newRawData(float value, long time) {
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
      tv_spo2 = (TextView) findViewById(R.id.tv_spo2);
      tv_pr = (TextView) findViewById(R.id.tv_pr);
      tv_rr = (TextView) findViewById(R.id.tv_rr);
      tv_pi = (TextView) findViewById(R.id.tv_pi);
      tv_Cargando_Pvi = (TextView) findViewById(R.id.tvCargandoPVi);
      tvPVi = (TextView) findViewById(R.id.tvPVi);
      tv_PmaxPmin = (TextView) findViewById(R.id.tv_PmaxPmin);

      tv_Cisura = (TextView) findViewById(R.id.textViewCisura);
      tv_Area= findViewById(R.id.textViewArea);

      //GRÁFICA
      chart = (LineChart) findViewById(R.id.chart_heart);
      XAxis xAxis = chart.getXAxis();
      xAxis.setAxisMinimum(0);
      xAxis.setAxisMaximum(5000);
      xAxis.setDrawLabels(true);
      yAxis = chart.getAxisLeft();
      yAxis.setAxisMaximum(100);
      yAxis.setAxisMinimum(0);
      yAxis.setDrawLabels(true);

      chart.getAxisRight().setEnabled(false);
      //chart.getLegend().setEnabled(true);
      //chart.getLegend().setExtra(ColorTemplate.VORDIPLOM_COLORS, new String[] { "Set1", "Set2", "Set3", "Set4", "Set5" });

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
      // final int charaProp = CharacteristicOperationFragment.PROPERTY_NOTIFY;
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
            if (fileName != null){
               f1.write(s.getBytes());
            }

            //Se pasa de complemento a 2 a decimal
            float numeroDecimal = (~data[3]) + 1;

            tiempoFinal = System.currentTimeMillis();
            if (!primeraVezCalculoPVi) {
               datosGrafica.add((double) numeroDecimal);

               if (tiempoInicial + cicloMuestras <= tiempoFinal) {
                  calcularPVi(datosGrafica);
                  tiempoInicial = System.currentTimeMillis();
                  tv_Cargando_Pvi.setText("");

               }

            } else if (tiempoInicial + cicloMuestras <= tiempoFinal) {
               tiempoInicial = System.currentTimeMillis();
               primeraVezCalculoPVi = false;

            }

            float datoChart = Normalization(numeroDecimal, -49, 49, 0, 100);
            newRawData(datoChart, System.currentTimeMillis());

         } else if (data[1] == 11 && (data[2] == -127/*0x81*/)) {
            //Log.d("Dato chart", ",,,,data2 "+data[8]);
            int spO2 = data[3] & 0xff;

            int pr = (data[4] & 0xff) + (data[5] & 0xff) * 256;

            //int pr = data[4] & 0xff;

            int rr = data[6];
            int pi = (data[7] & 0xff) + (data[8] & 0xff) * 256;

            int unk = (data[9] & 0xff) + (data[10] & 0xff) * 256;

            s += ", " + spO2 + ", " + pr + ", " + rr + ", " + pi + ", " + unk + "\n";

            if (spO2!=0 || pr!=0 || rr!=0 || pi!=0) {
               HashMap<String, Double> hashDatos = new HashMap<String, Double>();

               hashDatos.put("Sp02", (double) spO2);
               hashDatos.put("Pr", (double) pr);
               hashDatos.put("Rr", (double) rr);
               hashDatos.put("Pi", (double) pi);
               hashDatos.put("PVi", PVI);
               hashDatos.put("Area", area);
               if (tipoVaso!=null){
                  if (tipoVaso.equals("Vasodilatacion")){
                     hashDatos.put("Cisura",0.0 );
                  }else{
                     hashDatos.put("Cisura",1.0 );
                  }
               }

               Utilities.datosPulsioximetro.add(hashDatos);
            }

            tv_spo2.setText(valueOf(spO2));
            tv_pr.setText(valueOf(pr));
            tv_rr.setText(valueOf(rr));
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

   @SuppressLint("DefaultLocale")
   public void calcularPVi(ArrayList<Double> datos) {
      //Log.d("Dato chart", "////An"+datos);

      ArrayList<Double> arrayAmplitudes = new ArrayList<Double>();

      Double maxAmp = Double.NEGATIVE_INFINITY;
      Double minAmp = Double.POSITIVE_INFINITY;

      Queue<Double> cola = new LinkedList<Double>();

      int tamanyoVentana = 15;

      Boolean subiendo = false;

      Boolean contadorPrimeraVez = true;
      Boolean contadorInicializarTendencia = true;

      Double valorSumaAux = 0.0;

      Double cisuraMax = 0.0;

      //************************************************************************
      // para calcular el área deberíamos guardar los datos que vamos a utilizar
      // del primer punto hasta que encontremos un minAmp que pasará a ser
      // el primer punto de la siguiente área
      //************************************************************************
      ArrayList<Double> arrayPuntosArea = new ArrayList<Double>();

      double tamanyoBase =  ((double) cicloMuestras / datos.size()) / 1000.0;
      Log.d("Area", "---------------------- "+tamanyoBase + " Size "+datos.size());
      // obtengo los datos leídos
      for (int i = 0; i < datos.size(); i++) {

         //************************************************
         // vamos rellenando el array para calcular el área
         arrayPuntosArea.add(datos.get(i));

         // voy añadiendo el dato a la cola (FIFO)
         cola.add(datos.get(i));

         // empiezo a rellenar la ventana y calcular valores cuando tengo (tamaño datos leidos)==(tamaño ventana)
         if (i >= tamanyoVentana - 1) {
            ArrayList<Double> ventana = new ArrayList(cola);
            ArrayList<Double> ventanaCisura = new ArrayList<Double>(ventana);
            // ordeno la ventana
            Collections.sort(ventana);
            // obtengo max y min de la ventana
            Double maximoRelativo = ventana.get(ventana.size() - 1);
            Double minimoRelativo = ventana.get(0);
            // actualizo minAmp y maxAmp
            if (minimoRelativo < minAmp) {
               minAmp = minimoRelativo;
            }
            if (maximoRelativo > maxAmp) {
               maxAmp = maximoRelativo;
            }
            // calculo la suma de los datos de la ventana
            Double valorSuma = sumaVentana(ventana);

            // si es la primera ventana que tengo inicializo valorSumaAux
            if (contadorPrimeraVez) {
               valorSumaAux = valorSuma;
               contadorPrimeraVez = false;
            } else {
               //Log.d("Dato chart", "//// Ventana: " + ventana +" Suma: "+valorSuma +" Suma aux:" + valorSumaAux );

               // como ya tengo dos sumas de ventanas compruebo la dirección de la curva la primera vez
               if (contadorInicializarTendencia) {
                  if (valorSuma > valorSumaAux) {
                     subiendo = true;
                  } else {
                     subiendo = false;
                  }
                  contadorInicializarTendencia = false;
               }

               // calculo la amplitud cuando cambia la tendencia
               if (valorSuma > valorSumaAux && !subiendo) {

                  //Log.d("Dato chart", "//// maxAmp-minAmp: " + (maxAmp-minAmp) + " abs(maxAmp-minAmp): " + Math.abs(maxAmp-minAmp));
                  //*******************************************************************************************************************
                  //Log.d("Dato chart", "Maximo tiempo: "+tiempoEncontrarMax+ " Minimo tiempo: "+tiempoEncontrarMin);

                  double area = calcularArea(arrayPuntosArea, tamanyoBase);
                  Log.d("Area", "Area = "+area);

                  //
                  // hacemos con area lo que tengamos que hacer
                  Log.d("Area", "Area de " + arrayPuntosArea.get(0) + " a "
                          + arrayPuntosArea.get(arrayPuntosArea.size() - 1)
                          + " = " + area);

                  arrayPuntosArea.clear();

                  arrayAmplitudes.add(Math.abs(maxAmp - minAmp));
                  anadirTextoPmaxPmin(String.valueOf(Math.abs(maxAmp - minAmp)));
                  maxAmp = Double.NEGATIVE_INFINITY;
                  minAmp = Double.POSITIVE_INFINITY;

                  subiendo = true;

                  // empieza otra subgráfica
                  maximoCisura = Double.NEGATIVE_INFINITY;
               }

               if (valorSuma < valorSumaAux) {
                  subiendo = false;

               }

               //*************************************************
               // mientras está bajando calculo valor cisura
               //*************************************************
               if (!subiendo) {
                  cisuraMax = calcularValorCisura(ventanaCisura);
                  Log.d("Dato chart", "//// cisuraMax: " + cisuraMax + " MaxAmp: " + maxAmp + " MinAmp: " + minAmp);

                  if (cisuraMax != Double.NEGATIVE_INFINITY) {
                     calcularVasos(maxAmp, minAmp, cisuraMax);
                  }
               }

               // actualizo valorSumaAux con la suma actual
               valorSumaAux = valorSuma;
            }

            /*Log.d("Dato chart", "//// " + " Max: "+maximoRelativo + " Min: "+minimoRelativo +
                    " MaxAMp "+maxAmp+ " MinAmp "+minAmp+ " Amplitud "+arrayAmplitudes +" Subiendo: " +subiendo);*/

            // elimino primer elemento de la cola
            cola.remove();
         }
      }

      // ordeno array de amplitudes obtenidas
      Collections.sort(arrayAmplitudes);
      // obtengo la amplitud máxima y mínima
      if (arrayAmplitudes.size() != 0) {
         Double amplitudMax = arrayAmplitudes.get(arrayAmplitudes.size() - 1);
         Double amplitudMin = arrayAmplitudes.get(0);

         //                PImax - PImin
         // obtengo PVI = --------------- x 100
         //                    PImax

         PVI = ((amplitudMax - amplitudMin) / amplitudMax) * 100;
      }

      // añado resultado a la app
      anadirTextoPVi(String.format("%.1f", PVI));

      //Log.d("Dato chart", "//// PVI= " +PVI + "ArrayA: "+ arrayAmplitudes+ " max: " +amplitudMax+ " min: "+ amplitudMin);

      // limpio los datos de la gràfica
      datosGrafica.clear();


      //maximoCisura = Double.NEGATIVE_INFINITY;
   }

   public void anadirTextoPVi(String texto) {
      tvPVi.setText(texto);
   }

   public void anadirTextoPmaxPmin(String texto) {
      tv_PmaxPmin.setText("Amplitud de PI = " + texto);
   }

   private Double sumaVentana(ArrayList<Double> ventana) {
      Double suma = 0.0;
      for (int i = 0; i < ventana.size(); i++){
         suma += ventana.get(i);
      }
      return suma;
   }

   public float Normalization(double v, double Min, double Max,
                              double newMin, double newMax) {
      return (float) ((v - Min) / (Max - Min) * (newMax - newMin) + newMin);
   }

   public Double calcularValorCisura(ArrayList<Double> ventana) {

      for (int i = 1; i < ventana.size(); i++) {
         if (ventana.get(i) > ventana.get(i - 1)) {
            maximoCisura = ventana.get(i);
         }
      }

      return maximoCisura;
   }

   public void calcularVasos(double max,double min, double cisura) {
      double difCisuraMax = Math.abs(max - cisura);
      double difCisuraMin = Math.abs(min - cisura);

      if (difCisuraMax > difCisuraMin) {
         Log.d("Dato chart", "Vasodilatación  Max: " + max + " Min: " + min + " " + cisura);
         tipoVaso="Vasodilatación";
         tv_Cisura.setText(tipoVaso);
      } else {
         Log.d("Dato chart", "Vasocontricción  Max: " + max + " Min: " + min + " " + cisura);
         tipoVaso="Vasocontricción";
         tv_Cisura.setText(tipoVaso);
      }
   }

   public double calcularArea( ArrayList<Double> datos, double base) {
      area = 0.0;
      // habría que calcular el tamaño del rectángulo (Tiempo / N)

      //double base = 0.001;

      double primerPunto = datos.get(0);
      double ultimoPunto = datos.get(datos.size() - 1);
      double puntoBase = (primerPunto > ultimoPunto) ? ultimoPunto : primerPunto;
      double difBase = Math.abs(primerPunto - ultimoPunto);

      for (int i = 0; i < datos.size(); i++) {
         if (datos.get(i) > puntoBase) {
            area += base * (datos.get(i) + difBase);
         } else {
            area += base * datos.get(i);
         }
      }
      tv_Area.setText(String.format("%.2f", area));
      return area;
   }

}
