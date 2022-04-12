package es.upv.oximetro;

import static android.os.Environment.getExternalStorageDirectory;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
//import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;

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

        // cambio unas propiedades del sistema para utilizar la librería poi más pequeña
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl");


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
        XSSFWorkbook workbook = new XSSFWorkbook();

        XSSFSheet sheet= workbook.createSheet(nombrePaciente);


        CellStyle cellStyle= workbook.createCellStyle();
        cellStyle.setFillForegroundColor((short) R.color.colorTextoBlanco);

        //TITULOS DEL EXCEL
        Cell cell= null;
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

        for (int i= 1; i<Utilities.datosPulsioximetro.size(); i++){
            //Log.d(TAG, "++++dentro for" );
            //VALORES DE LAS CASILLAS
            //Cell=0 -> SP02 Cell=1 -> PR Cell=2 -> RR Cell=3 -> PI
            row= sheet.createRow(i);
            cell= row.createCell(0);
            cell.setCellValue(Utilities.datosPulsioximetro.get(i).get("Sp02"));


            cell = row.createCell(1);
            cell.setCellValue(Utilities.datosPulsioximetro.get(i).get("Pr"));

            cell = row.createCell(2);
            cell.setCellValue(Utilities.datosPulsioximetro.get(i).get("Rr"));

            cell = row.createCell(3);
            cell.setCellValue(Utilities.datosPulsioximetro.get(i).get("Pi"));
        }

        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String strDate = sdf.format(c.getTime());

        File file= new File(getExternalFilesDir(null), nombrePaciente+"_"+strDate+".xlsx");
        FileOutputStream outputStream= null;


        try {
            outputStream =  new FileOutputStream(file.getAbsolutePath());
            workbook.write(outputStream);
            outputStream.flush();
            outputStream.close();
            Toast.makeText(this, "Los datos se han guardado correctamente", Toast.LENGTH_SHORT).show();

        }catch(IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error111", Toast.LENGTH_SHORT).show();

        }
    }
}
