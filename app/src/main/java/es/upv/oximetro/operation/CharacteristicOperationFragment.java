package es.upv.oximetro.operation;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import es.upv.oximetro.R;
import es.upv.fastble.BleManager;
import es.upv.fastble.callback.BleIndicateCallback;
import es.upv.fastble.callback.BleNotifyCallback;
import es.upv.fastble.callback.BleReadCallback;
import es.upv.fastble.callback.BleWriteCallback;
import es.upv.fastble.data.BleDevice;
import es.upv.fastble.exception.BleException;
import es.upv.fastble.utils.HexUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.copyOfRange;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CharacteristicOperationFragment extends Fragment {

   public static final int PROPERTY_READ = 1;
   public static final int PROPERTY_WRITE = 2;
   public static final int PROPERTY_WRITE_NO_RESPONSE = 3;
   public static final int PROPERTY_NOTIFY = 4;
   public static final int PROPERTY_INDICATE = 5;

   private LinearLayout layout_container;
   private List<String> childList = new ArrayList<>();

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View v = inflater.inflate(R.layout.fragment_characteric_operation, null);
      initView(v);
      return v;
   }

   private void initView(View v) {
      layout_container = (LinearLayout) v.findViewById(R.id.layout_container);
   }

/*   public void showData_viejo() {
      final BleDevice bleDevice = ((OperationActivity) getActivity()).getBleDevice();
      final BluetoothGattCharacteristic characteristic = ((OperationActivity) getActivity()).getCharacteristic();
      final int charaProp = ((OperationActivity) getActivity()).getCharaProp();
      String child = characteristic.getUuid().toString() + String.valueOf(charaProp);

      for (int i = 0; i < layout_container.getChildCount(); i++) {
         layout_container.getChildAt(i).setVisibility(View.GONE);
      }
      if (childList.contains(child)) {
         layout_container.findViewWithTag(bleDevice.getKey() + characteristic.getUuid().toString() + charaProp).setVisibility(View.VISIBLE);
      } else {
         childList.add(child);

         View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation, null);
         view.setTag(bleDevice.getKey() + characteristic.getUuid().toString() + charaProp);
         LinearLayout layout_add = (LinearLayout) view.findViewById(R.id.layout_add);
         final TextView txt_title = (TextView) view.findViewById(R.id.txt_title);
         txt_title.setText(String.valueOf(characteristic.getUuid().toString() + getActivity().getString(R.string.data_changed)));
         final TextView txt = (TextView) view.findViewById(R.id.txt);
         txt.setMovementMethod(ScrollingMovementMethod.getInstance());

         switch (charaProp) {
            case PROPERTY_READ: {
               View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
               Button btn = (Button) view_add.findViewById(R.id.btn);
               btn.setText(getActivity().getString(R.string.read));
               btn.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {
                     BleManager.getInstance().read(
                             bleDevice,
                             characteristic.getService().getUuid().toString(),
                             characteristic.getUuid().toString(),
                             new BleReadCallback() {

                                @Override
                                public void onReadSuccess(final byte[] data) {
                                   runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                         addText(txt, HexUtil.formatHexString(data, true));
                                      }
                                   });
                                }

                                @Override
                                public void onReadFailure(final BleException exception) {
                                   runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                         addText(txt, exception.toString());
                                      }
                                   });
                                }
                             });
                  }
               });
               layout_add.addView(view_add);
            }
            break;

            case PROPERTY_WRITE: {
               View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_et, null);
               final EditText et = (EditText) view_add.findViewById(R.id.et);
               Button btn = (Button) view_add.findViewById(R.id.btn);
               btn.setText(getActivity().getString(R.string.write));
               btn.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {
                     String hex = et.getText().toString();
                     if (TextUtils.isEmpty(hex)) {
                        return;
                     }
                     BleManager.getInstance().write(
                             bleDevice,
                             characteristic.getService().getUuid().toString(),
                             characteristic.getUuid().toString(),
                             HexUtil.hexStringToBytes(hex),
                             new BleWriteCallback() {

                                @Override
                                public void onWriteSuccess(final int current, final int total, final byte[] justWrite) {
                                   runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                         addText(txt, "write success, current: " + current
                                                 + " total: " + total
                                                 + " justWrite: " + HexUtil.formatHexString(justWrite, true));
                                      }
                                   });
                                }

                                @Override
                                public void onWriteFailure(final BleException exception) {
                                   runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                         addText(txt, exception.toString());
                                      }
                                   });
                                }
                             });
                  }
               });
               layout_add.addView(view_add);
            }
            break;

            case PROPERTY_WRITE_NO_RESPONSE: {
               View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_et, null);
               final EditText et = (EditText) view_add.findViewById(R.id.et);
               Button btn = (Button) view_add.findViewById(R.id.btn);
               btn.setText(getActivity().getString(R.string.write));
               btn.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {
                     String hex = et.getText().toString();
                     if (TextUtils.isEmpty(hex)) {
                        return;
                     }
                     BleManager.getInstance().write(
                             bleDevice,
                             characteristic.getService().getUuid().toString(),
                             characteristic.getUuid().toString(),
                             HexUtil.hexStringToBytes(hex),
                             new BleWriteCallback() {

                                @Override
                                public void onWriteSuccess(final int current, final int total, final byte[] justWrite) {
                                   runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                         addText(txt, "write success, current: " + current
                                                 + " total: " + total
                                                 + " justWrite: " + HexUtil.formatHexString(justWrite, true));
                                      }
                                   });
                                }

                                @Override
                                public void onWriteFailure(final BleException exception) {
                                   runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                         addText(txt, exception.toString());
                                      }
                                   });
                                }
                             });
                  }
               });
               layout_add.addView(view_add);
            }
            break;


            case PROPERTY_NOTIFY: {
               View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
               final Button btn = (Button) view_add.findViewById(R.id.btn);
               btn.setText(getActivity().getString(R.string.open_notification));
               btn.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {
                     if (btn.getText().toString().equals(getActivity().getString(R.string.open_notification))) {
                        btn.setText(getActivity().getString(R.string.close_notification));
                        BleManager.getInstance().notify(
                                bleDevice,
                                characteristic.getService().getUuid().toString(),
                                characteristic.getUuid().toString(),
                                new BleNotifyCallback() {

                                   @Override
                                   public void onNotifySuccess() {
                                      runOnUiThread(new Runnable() {
                                         @Override
                                         public void run() {
                                            addText(txt, "notify success");
                                         }
                                      });
                                   }

                                   @Override
                                   public void onNotifyFailure(final BleException exception) {
                                      runOnUiThread(new Runnable() {
                                         @Override
                                         public void run() {
                                            addText(txt, exception.toString());
                                         }
                                      });
                                   }

                                   @Override
                                   public void onCharacteristicChanged(byte[] data) {
                                      writeFrames(data);
                                      runOnUiThread(new Runnable() {
                                         @Override
                                         public void run() {
                                            addText(txt, HexUtil.formatHexString(characteristic.getValue(), true));
                                            Log.d("LLL", System.currentTimeMillis() + "; " + HexUtil.formatHexString(characteristic.getValue(), true));
                                         }
                                      });
                                   }
                                });
                     } else {
                        btn.setText(getActivity().getString(R.string.open_notification));
                        BleManager.getInstance().stopNotify(
                                bleDevice,
                                characteristic.getService().getUuid().toString(),
                                characteristic.getUuid().toString());
                     }
                  }
               });
               layout_add.addView(view_add);
            }
            break;

           case PROPERTY_INDICATE: {
               View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
               final Button btn = (Button) view_add.findViewById(R.id.btn);
               btn.setText(getActivity().getString(R.string.open_notification));
               btn.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {
                     if (btn.getText().toString().equals(getActivity().getString(R.string.open_notification))) {
                        btn.setText(getActivity().getString(R.string.close_notification));
                        BleManager.getInstance().indicate(
                                bleDevice,
                                characteristic.getService().getUuid().toString(),
                                characteristic.getUuid().toString(),
                                new BleIndicateCallback() {

                                   @Override
                                   public void onIndicateSuccess() {
                                      runOnUiThread(new Runnable() {
                                         @Override
                                         public void run() {
                                            addText(txt, "indicate success");
                                         }
                                      });
                                   }

                                   @Override
                                   public void onIndicateFailure(final BleException exception) {
                                      runOnUiThread(new Runnable() {
                                         @Override
                                         public void run() {
                                            addText(txt, exception.toString());
                                         }
                                      });
                                   }

                                   @Override
                                   public void onCharacteristicChanged(byte[] data) {
                                      runOnUiThread(new Runnable() {
                                         @Override
                                         public void run() {
                                            addText(txt, HexUtil.formatHexString(characteristic.getValue(), true));
                                         }
                                      });
                                   }
                                });
                     } else {
                        btn.setText(getActivity().getString(R.string.open_notification));
                        BleManager.getInstance().stopIndicate(
                                bleDevice,
                                characteristic.getService().getUuid().toString(),
                                characteristic.getUuid().toString());
                     }
                  }
               });
               layout_add.addView(view_add);
            }
            break;
         }

         layout_container.addView(view);
      }
   }*/



   //DONE: Quitar botón NOTIFY y enviar la notificación directamente
 /////////////////////////////
   public void showData() {
      final BleDevice bleDevice = ((OperationActivity) getActivity()).getBleDevice();
      final BluetoothGattCharacteristic characteristic = ((OperationActivity) getActivity()).getCharacteristic();
      final int charaProp = ((OperationActivity) getActivity()).getCharaProp();
      String child = characteristic.getUuid().toString() + String.valueOf(charaProp);
      for (int i = 0; i < layout_container.getChildCount(); i++) {
         layout_container.getChildAt(i).setVisibility(View.GONE);
      }
      if (childList.contains(child)) {
         layout_container.findViewWithTag(bleDevice.getKey() + characteristic.getUuid().toString() + charaProp).setVisibility(View.VISIBLE);
      } else {
         childList.add(child);
         View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation, null);
         view.setTag(bleDevice.getKey() + characteristic.getUuid().toString() + charaProp);
         LinearLayout layout_add = (LinearLayout) view.findViewById(R.id.layout_add);
         final TextView txt_title = (TextView) view.findViewById(R.id.txt_title);
         txt_title.setText(String.valueOf(characteristic.getUuid().toString() + getActivity().getString(R.string.data_changed)));
         final TextView txt = (TextView) view.findViewById(R.id.txt);
         txt.setMovementMethod(ScrollingMovementMethod.getInstance());

         BleManager.getInstance().notify(
              bleDevice,
              characteristic.getService().getUuid().toString(),
              characteristic.getUuid().toString(),
              new BleNotifyCallback() {

                 @Override
                 public void onNotifySuccess() {
                    runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                          addText(txt, "notify success");
                       }
                    });
                 }

                 @Override
                 public void onNotifyFailure(final BleException exception) {
                    runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                          addText(txt, exception.toString());
                       }
                    });
                 }

                 @Override
                 public void onCharacteristicChanged(byte[] data) {
                    writeFrames(data);
                    runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                          addText(txt, HexUtil.formatHexString(characteristic.getValue(), true));
                          Log.d("LLL", System.currentTimeMillis() + "; " + HexUtil.formatHexString(characteristic.getValue(), true));
                       }
                    });
                 }
              });
         layout_container.addView(view);
   }}


   private void runOnUiThread(Runnable runnable) {
      if (isAdded() && getActivity() != null)
         getActivity().runOnUiThread(runnable);
   }

   private void addText(TextView textView, String content) {
      textView.append(content);
      textView.append("\n");
      int offset = textView.getLineCount() * textView.getLineHeight();
      if (offset > textView.getHeight()) {
         textView.scrollTo(0, offset - textView.getHeight());
      }
   }

   //DONE: Escribir datos en fichero CSV
   private long time;
   private String fileName;
   private FileOutputStream f1, f2;

   void writeFrames(byte[] data) {
      time = System.currentTimeMillis();
      writeSingleFrame(data);
      if (data.length > data[1]) {   //data[1] is length of frame
         writeSingleFrame(copyOfRange(data,data[1],data.length));
      }
   }

   void writeSingleFrame(byte[] data) {
      String s = "" + time;
      try {
         if (f1 == null || f2 == null) {
            if (fileName == null) {
               SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
               String currentDate = dateFormat.format(new Date());
               fileName = Environment.getExternalStorageDirectory() + "/"+currentDate;
            }
            f1 = new FileOutputStream(fileName+" raw.csv");
            f1.write("TIME, D1, D2, D3\n".getBytes());
            f2 = new FileOutputStream(fileName+" data.csv");
            f2.write("TIME, SpO2, PR/min, RR/min, PI, ??? \n".getBytes());
         }
         if ((data[1] == 6) && (data[2] == -128/*0x80*/)) {
            int unsignedbyte = data[5] & 0xff;
            s += ", " + data[3] + ", " + data[4] + ", " + unsignedbyte+"\n";
            f1.write(s.getBytes());
         } else if (data[1] == 11 && (data[2] == -127/*0x81*/)) {
            int spO2 = data[3] & 0xff;
            int pr = data[4] & 0xff;
            int rr = data[6];
            int pi = (data[7] & 0xff) + (data[8] & 0xff)*256;
            int unk = (data[9] & 0xff) + (data[10] & 0xff)*256;
            s += ", " + spO2 + ", " + pr + ", " + rr + ", " + pi + ", " + unk +"\n";
            f2.write(s.getBytes());
         } else {
            Log.e("LLL", "ERROR: Trama desconocida");
         }

      } catch (FileNotFoundException e) {
         Log.e("LLL", "ERROR: Abriendo fichero " + e.toString());
      } catch (IOException e) {
         Log.e("LLL", "ERROR: Escribiendo fichero " + e.toString());
      }
   }

   @Override
   public void onStop() {
      try {
         if (f1!=null) f1.close();
         if (f2!=null) f2.close();
      } catch (IOException e) {
         Log.e("LLL", "ERROR: Cerrando fichero " + e.toString());
      }
      super.onStop();
   }
}
