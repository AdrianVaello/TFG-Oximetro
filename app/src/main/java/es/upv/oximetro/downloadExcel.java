package es.upv.oximetro;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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

public class downloadExcel extends AppCompatActivity {

    ImageView bt_descargar_excel;
    ImageView bt_volver_atras;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_excel);

        bt_descargar_excel= findViewById(R.id.bt_descargar_excel);
        bt_descargar_excel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarDatosExcel();
            }
        });
        bt_volver_atras= findViewById(R.id.bt_volver_atras);
        bt_volver_atras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(downloadExcel.this, //*OperationActivity.class);
                        MainActivity.class);
                startActivity(intent);
            }
        });

    }


    public void guardarDatosExcel(){
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

        //CAMBIAR EL NOMBRE
        File file= new File(getExternalFilesDir(null), "Usuario2.xls");
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