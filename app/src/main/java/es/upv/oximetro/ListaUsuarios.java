package es.upv.oximetro;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
<<<<<<< Updated upstream

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
=======
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
>>>>>>> Stashed changes

public class ListaUsuarios extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lista_usuarios);
<<<<<<< Updated upstream
=======

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

        File file = new File(adapter.getItem(position));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID+".provider",file);
        //intent.putExtra(Intent.EXTRA_TEXT, uri);
        intent.setDataAndType(uri,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        //intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Open file...",null));

>>>>>>> Stashed changes
    }
}
