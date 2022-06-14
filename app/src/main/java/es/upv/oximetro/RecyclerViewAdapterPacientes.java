package es.upv.oximetro;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class RecyclerViewAdapterPacientes extends RecyclerView.Adapter<RecyclerViewAdapterPacientes.ViewHolder> {

    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private static final String TAG = "1";

    private final ArrayList<File> mfilesAdapter;
    private final ArrayList<File> mfilesAdapterCopy= new ArrayList<File>();

    // Los datos son pasados al contructor
    RecyclerViewAdapterPacientes(Context context, ArrayList<File> filesAdapter) {
        this.mInflater = LayoutInflater.from(context);
        this.mfilesAdapter=filesAdapter;
        this.mfilesAdapterCopy.addAll(mfilesAdapter);
    }

    // Añade el adaptador de pacientes
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.adapter_pacientes, parent, false);
        return new ViewHolder(view);
    }

    // Añade la informacion a cada campo del paciente
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String[] nombreFichero=mfilesAdapter.get(position).getName().split("_");
        String[] fechaFichero=nombreFichero[1].split("\\.");

        holder.tx_nombre_pacientes.setText( nombreFichero[0]);
        holder.tx_fecha_pacientes.setText( fechaFichero[0]);

    }

    // Numero total de filas
    @Override
    public int getItemCount() {
        return mfilesAdapter.size();
    }



    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tx_nombre_pacientes;
        TextView tx_fecha_pacientes;
        RelativeLayout paciente;

        ViewHolder(View itemView) {
            super(itemView);
            paciente= itemView.findViewById(R.id.Paciente);
            tx_fecha_pacientes= itemView.findViewById(R.id.txt_fecha_paciente);
            tx_nombre_pacientes= itemView.findViewById(R.id.txt_nombre_paciente);

            itemView.setOnClickListener(this);
        }

        // Funcion para cuando se hace click en un paciente
        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // Se obtiene los datos del paciente en la posicion en la que se ha clickado
    String getItem(int id) {
        return mfilesAdapter.get(id).toString();
    }

    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    /* Filtra los datos del adaptador */
    public void filtrar(String texto, String fecha) {

        // Elimina todos los datos del ArrayList que se cargan en los
        // elementos del adaptador
        mfilesAdapter.clear();

        // Si no hay texto: agrega de nuevo los datos del ArrayList copiado
        // al ArrayList que se carga en los elementos del adaptador
        if (texto.length() == 0 && fecha.length()==0) {
            mfilesAdapter.addAll(mfilesAdapterCopy);
        } else if(texto.length() != 0 && fecha.length()==0) {
            // Recorre todos los elementos que contiene el ArrayList copiado
            // y dependiendo de si estos contienen el texto ingresado por el
            // usuario los agrega de nuevo al ArrayList que se carga en los
            // elementos del adaptador.

            for (int i=0; i<mfilesAdapterCopy.size();i++){
                String[] nombreFichero=mfilesAdapterCopy.get(i).getName().split("_");
                if(nombreFichero[0].toLowerCase(Locale.ROOT).contains(texto) ){
                    mfilesAdapter.add(mfilesAdapterCopy.get(i));
                }
            }

        }else if(texto.length() == 0 && fecha.length()!=0){
            for (int i=0; i<mfilesAdapterCopy.size();i++){
                String[] nombreFichero=mfilesAdapterCopy.get(i).getName().split("_");
                String[] fechaFichero=nombreFichero[1].split("\\.");
                if(fechaFichero[0].contains(fecha)){
                    mfilesAdapter.add(mfilesAdapterCopy.get(i));
                }

            }
        }else {
            for (int i=0; i<mfilesAdapterCopy.size();i++){
                String[] nombreFichero=mfilesAdapterCopy.get(i).getName().split("_");
                String[] fechaFichero=nombreFichero[1].split("\\.");
                if(fechaFichero[0].contains(fecha) && nombreFichero[0].toLowerCase(Locale.ROOT).contains(texto)){
                    mfilesAdapter.add(mfilesAdapterCopy.get(i));
                }
            }
        }

        // Actualiza el adaptador para aplicar los cambios
        notifyDataSetChanged();
    }
}
