package es.upv.oximetro;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DownloadExcel extends AppCompatActivity {

    private static final String TAG = "1";
    ImageView bt_descargar_excel;
    ImageView bt_volver_atras;
    TextView nombrePaciente;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_excel);
        Log.d(TAG, "----" +Utilities.datosPulsioximetro);

        bt_descargar_excel= findViewById(R.id.bt_descargar_excel);
        bt_descargar_excel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nombrePaciente= findViewById(R.id.nombrePaciente);
                if(nombrePaciente.getText().toString().equals(" ") || nombrePaciente.getText().toString().isEmpty()){
                    Toast.makeText(DownloadExcel.this, "Rellena el nombre del paciente primero.", Toast.LENGTH_SHORT).show();
                }else{
                    guardarDatosExcel(nombrePaciente.getText().toString());
                }

            }
        });
        bt_volver_atras= findViewById(R.id.bt_volver_atras);
        bt_volver_atras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DownloadExcel.this, //*OperationActivity.class);
                        MainActivity.class);
                startActivity(intent);
            }
        });

    }


    public void guardarDatosExcel(String nombrePaciente){
        Workbook workbook = new HSSFWorkbook();
        Cell cell= null;
        CellStyle cellStyle= workbook.createCellStyle();
        cellStyle.setFillForegroundColor(HSSFColor.WHITE.index);
        cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        Sheet sheet= null;
        sheet= workbook.createSheet("Usuario 1");

        //TITULOS DEL EXCEL
        Row row= null;
        row= sheet.createRow(0);
        cell= row.createCell(0);
        cell.setCellValue("SpO2");
        cell.setCellStyle(cellStyle);

        sheet.createRow(1);
        cell= row.createCell(1);
        cell.setCellValue("PR/min");
        cell.setCellStyle(cellStyle);

        sheet.createRow(2);
        cell= row.createCell(2);
        cell.setCellValue("RR/min");
        cell.setCellStyle(cellStyle);

        sheet.createRow(3);
        cell= row.createCell(3);
        cell.setCellValue("PI");
        cell.setCellStyle(cellStyle);

        //VALORES DE LAS CASILLAS
        row= sheet.createRow(1);
        cell= row.createCell(0);
        cell.setCellValue("spo2");

        cell = row.createCell(1);
        cell.setCellValue("pr");


        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = sdf.format(c.getTime());

        File file= new File(getExternalFilesDir(null), nombrePaciente+"_"+strDate+".xls");
        FileOutputStream outputStream= null;

        try {
            outputStream =  new FileOutputStream(file);
            workbook.write(outputStream);
            Toast.makeText(this, "Los datos se han guardado correctamente", Toast.LENGTH_SHORT).show();

        }catch(IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();

        }
    }
}
