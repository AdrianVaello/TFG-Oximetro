package es.upv.oximetro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DownloadExcel extends AppCompatActivity {

    private static final String TAG = "1";

    // Variables de la interfaz
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
        nombrePaciente= findViewById(R.id.nombrePaciente);
        // Se añade el click al boton de descargar datos
        bt_descargar_excel= findViewById(R.id.bt_descargar_excel);
        bt_descargar_excel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nombrePaciente.getText().toString().equals(" ") || nombrePaciente.getText().toString().isEmpty()){
                    Toast.makeText(DownloadExcel.this, "Rellena el nombre del paciente primero.", Toast.LENGTH_SHORT).show();
                }else{
                    guardarDatosExcel(nombrePaciente.getText().toString());
                }
            }
        });

        // Se añade el click al boton para volver atras
        bt_volver_atras= findViewById(R.id.bt_volver_atras);
        bt_volver_atras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DownloadExcel.this,MainActivity.class);
                startActivity(intent);
            }
        });

    }
    /* -------------------------------------
    Función para guardar los datos en Excel
    Params: Nombre del paciente
    ---------------------------------------*/
    public void guardarDatosExcel(String nombrePaciente){
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet= workbook.createSheet(nombrePaciente);

        CellStyle cellStyle= workbook.createCellStyle();
        cellStyle.setFillForegroundColor((short) R.color.colorTextoBlanco);

        //Titulos de las casillas del excel
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
        cell.setCellValue("PI (%)");
        cell.setCellStyle(cellStyle);

        sheet.createRow(4);
        cell= row.createCell(4);
        cell.setCellValue("PVi (%)");
        cell.setCellStyle(cellStyle);

        sheet.createRow(5);
        cell= row.createCell(5);
        cell.setCellValue("Area bajo curva");
        cell.setCellStyle(cellStyle);

        sheet.createRow(6);
        cell= row.createCell(6);
        cell.setCellValue("Valor cisura dicrótica");
        cell.setCellStyle(cellStyle);

        for (int i= 1; i<Utilities.datosPulsioximetro.size(); i++){
            // Valores de las casillas
            row= sheet.createRow(i);
            cell= row.createCell(0);
            cell.setCellValue(Utilities.datosPulsioximetro.get(i).get("Sp02"));

            cell = row.createCell(1);
            cell.setCellValue(Utilities.datosPulsioximetro.get(i).get("Pr"));

            cell = row.createCell(2);
            cell.setCellValue(Utilities.datosPulsioximetro.get(i).get("Rr"));

            cell = row.createCell(3);
            cell.setCellValue(Utilities.datosPulsioximetro.get(i).get("Pi"));

            cell = row.createCell(4);
            cell.setCellValue(Utilities.datosPulsioximetro.get(i).get("PVi"));

            cell = row.createCell(5);
            cell.setCellValue(Utilities.datosPulsioximetro.get(i).get("Area"));

            cell = row.createCell(6);
            if(Utilities.datosPulsioximetro.get(i).get("Cisura")!=null){
                if(Utilities.datosPulsioximetro.get(i).get("Cisura")==0.0){
                    cell.setCellValue("Vasodilatación");
                }else{
                    cell.setCellValue("Vasocontricción");
                }
            }else{
                cell.setCellValue("Sin valor exacto");
            }
        }

        // Se coge la fecha actual
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String strDate = sdf.format(c.getTime());

        // Se crea el archivo excel
        File file= new File(getExternalFilesDir(null), nombrePaciente+"_"+strDate+".xlsx");
        FileOutputStream outputStream= null;

        try {
            // Se guarda el archivo
            outputStream =  new FileOutputStream(file.getAbsolutePath());
            workbook.write(outputStream);
            outputStream.flush();
            outputStream.close();
            Toast.makeText(this, "Los datos se han guardado correctamente", Toast.LENGTH_SHORT).show();

        }catch(IOException e) {
            // Se recoge cualquier error que se pueda dar guardando el archivo
            e.printStackTrace();
            Toast.makeText(this, "Error guardando el archivo.", Toast.LENGTH_SHORT).show();
        }
    }

    // función para controlar el boton de volver atrás de los móviles Android
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(DownloadExcel.this, MainActivity.class);
        startActivity(intent);
    }
}
