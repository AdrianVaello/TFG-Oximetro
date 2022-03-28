package es.upv.oximetro;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListaUsuarios extends AppCompatActivity implements RecyclerViewAdapterPacientes.ItemClickListener{

    RecyclerViewAdapterPacientes adapter;
    public List<File> files= new ArrayList<>();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lista_usuarios);

        File directory = new File(String.valueOf(getExternalFilesDir(null)));
        files = Arrays.asList(directory.listFiles());
        for (int i =0; i<files.size(); i++){
            Log.d("Files", "3333Size: "+ files.get(i).getName());
        }
        Log.d("Files", "11Size: "+ files.size());

        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.reciclerViewPacientes);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerViewAdapterPacientes(this, files);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(View view, int position) {
        Toast.makeText(this, "You clicked " + adapter.getItem(position) + " on row number " + position, Toast.LENGTH_SHORT).show();
    }
}
