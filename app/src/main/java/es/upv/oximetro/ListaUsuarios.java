package es.upv.oximetro;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ListaUsuarios extends AppCompatActivity implements RecyclerViewAdapterPacientes.ItemClickListener{

    RecyclerViewAdapterPacientes adapter;
    public ArrayList<File> files= new ArrayList<>();

    ConstraintLayout relativeLayoutFiltro,relativeLayoutFiltroContenido ;
    Boolean filtroAbierto;
    ImageView flechaLeft, flechaRight, calendarFiltro;
    RotateAnimation animation;
    EditText editTextFiltroNombre, editTextFechaFiltro;
    TextView tv_filtro_texto, tv_no_pacientes;

    ImageView bt_volver_atras;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lista_usuarios);

        bt_volver_atras= findViewById(R.id.bt_volver_atras2);
        bt_volver_atras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ListaUsuarios.this, MainActivity.class);
                startActivity(intent);
            }
        });

        tv_no_pacientes=findViewById(R.id.tv_no_hay_pacientes);
        final DatePickerDialog[] picker = new DatePickerDialog[1];

        flechaLeft=findViewById(R.id.imageViewFlechaDesplegableLeft);
        flechaRight=findViewById(R.id.imageViewFlechaDesplegableRigth);
        calendarFiltro=findViewById(R.id.imageViewFechaFiltro);
        editTextFiltroNombre=findViewById(R.id.editTextNombrePersona);
        editTextFechaFiltro=findViewById(R.id.editTextFechaFiltro);
        tv_filtro_texto=findViewById(R.id.tv_texto_filtro);
        tv_filtro_texto.setText("Filtrando por Nombre: "+editTextFiltroNombre.getText().toString() + " y fecha: "+editTextFechaFiltro.getText().toString());

        relativeLayoutFiltro=findViewById(R.id.relativeLayoutFiltro);
        relativeLayoutFiltroContenido=findViewById(R.id.relativeLayoutFIltroContenido);
        relativeLayoutFiltroContenido.setVisibility(View.GONE);
        filtroAbierto=false;
        relativeLayoutFiltro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!filtroAbierto){
                    relativeLayoutFiltroContenido.setVisibility(View.VISIBLE);
                    filtroAbierto=true;
                    rotarImagenAbajo(flechaLeft);
                    rotarImagenAbajo(flechaRight);
                    animation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            flechaLeft.setRotation(180.0f);
                            flechaRight.setRotation(180.0f);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });


                }else{
                    relativeLayoutFiltroContenido.setVisibility(View.GONE);
                    filtroAbierto=false;
                    flechaLeft.setRotation(0.0f);
                    flechaRight.setRotation(0.0f);

                }

            }
        });


        //MOSTRAR DATOS RECYCLERVIEW
        File directory = new File(String.valueOf(getExternalFilesDir(null)));
        File[] filesAux = directory.listFiles();

        if(filesAux!=null){
            Arrays.sort(filesAux);
            files.addAll(Arrays.asList(filesAux));

        }

        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.reciclerViewPacientes);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerViewAdapterPacientes(this, files);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        comprobarNumeroPacientes(recyclerView);


        editTextFiltroNombre.addTextChangedListener(new TextWatcher() {
           @Override
           public void beforeTextChanged(CharSequence s, int start, int count, int after) {
               adapter.filtrar(editTextFiltroNombre.getText().toString().toLowerCase(Locale.ROOT),editTextFechaFiltro.getText().toString());

               tv_filtro_texto.setText("Filtrando por Nombre: "+editTextFiltroNombre.getText().toString() + " y fecha: "+editTextFechaFiltro.getText().toString());
               comprobarNumeroPacientes(recyclerView);
           }

           @Override
           public void onTextChanged(CharSequence s, int start, int before, int count) {
               adapter.filtrar(editTextFiltroNombre.getText().toString().toLowerCase(Locale.ROOT),editTextFechaFiltro.getText().toString());

               tv_filtro_texto.setText("Filtrando por Nombre: "+editTextFiltroNombre.getText().toString() + " y fecha: "+editTextFechaFiltro.getText().toString());
               comprobarNumeroPacientes(recyclerView);
           }

           @Override
           public void afterTextChanged(Editable s) {
               adapter.filtrar(editTextFiltroNombre.getText().toString().toLowerCase(Locale.ROOT),editTextFechaFiltro.getText().toString());

               tv_filtro_texto.setText("Filtrando por Nombre: "+editTextFiltroNombre.getText().toString() + " y fecha: "+editTextFechaFiltro.getText().toString());
               comprobarNumeroPacientes(recyclerView);
           }
       });

        calendarFiltro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar cldr = Calendar.getInstance();
                int day = cldr.get(Calendar.DAY_OF_MONTH);
                int month = cldr.get(Calendar.MONTH);
                int year = cldr.get(Calendar.YEAR);
                // date picker dialog
                picker[0] = new DatePickerDialog(ListaUsuarios.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                if(monthOfYear<10){
                                    editTextFechaFiltro.setText(dayOfMonth + "-0" + (monthOfYear + 1) + "-" + year);
                                }else{
                                    editTextFechaFiltro.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);
                                }

                            }
                        }, year, month, day);
                picker[0].show();
            }
        });
        editTextFechaFiltro.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                adapter.filtrar(editTextFiltroNombre.getText().toString().toLowerCase(Locale.ROOT),editTextFechaFiltro.getText().toString());
                tv_filtro_texto.setText("Filtrando por Nombre: "+editTextFiltroNombre.getText().toString() + " y fecha: "+editTextFechaFiltro.getText().toString());
                comprobarNumeroPacientes(recyclerView);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filtrar(editTextFiltroNombre.getText().toString().toLowerCase(Locale.ROOT),editTextFechaFiltro.getText().toString());

                tv_filtro_texto.setText("Filtrando por Nombre: "+editTextFiltroNombre.getText().toString() + " y fecha: "+editTextFechaFiltro.getText().toString());
                comprobarNumeroPacientes(recyclerView);
            }

            @Override
            public void afterTextChanged(Editable s) {
                adapter.filtrar(editTextFiltroNombre.getText().toString().toLowerCase(Locale.ROOT),editTextFechaFiltro.getText().toString());

                tv_filtro_texto.setText("Filtrando por Nombre: "+editTextFiltroNombre.getText().toString() + " y fecha: "+editTextFechaFiltro.getText().toString());
                comprobarNumeroPacientes(recyclerView);
            }
        });


    }

    @Override
    public void onItemClick(View view, int position) {

        File file = new File(adapter.getItem(position));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID+".provider",file);
        intent.setDataAndType(uri,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.abrir_datos_con),null));

    }

    private void rotarImagenAbajo(View view){
        animation = new RotateAnimation(0, 180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);

        animation.setDuration(1000);
        animation.setRepeatCount(0);
        view.startAnimation(animation);

    }

    public void comprobarNumeroPacientes(RecyclerView recyclerView){
        if(recyclerView.getAdapter()!=null){
            if(recyclerView.getAdapter().getItemCount()==0){
                tv_no_pacientes.setVisibility(View.VISIBLE);
            }else{
                tv_no_pacientes.setVisibility(View.GONE);
            }
        }
    }


}
