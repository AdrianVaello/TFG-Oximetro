package es.upv.oximetro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ActivityAyuda extends AppCompatActivity {

    // Variables de la interfaz
    ImageView bt_volver_atras;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ayuda);

        // Se añade la funcionalidad al botón de volver atrás
        bt_volver_atras= findViewById(R.id.bt_volver_atras2);
        bt_volver_atras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivityAyuda.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
    // Función para controlar el boton de volver atrás de los móviles Android
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ActivityAyuda.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
