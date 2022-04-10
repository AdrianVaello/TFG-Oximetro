package es.upv.oximetro;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class RecyclerViewAdapterPacientes extends RecyclerView.Adapter<RecyclerViewAdapterPacientes.ViewHolder> {

    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private static final String TAG = "1";

    private List<File> mfilesAdapter;

    // data is passed into the constructor
    RecyclerViewAdapterPacientes(Context context, List<File> filesAdapter) {
        this.mInflater = LayoutInflater.from(context);
        this.mfilesAdapter=filesAdapter;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.adapter_pacientes, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        //Log.d("Files", "11FileName:" + mfilesAdapter.get(position).getName());
        //Log.d("Files", "11FileName:" + mfilesAdapter.size());

        String[] nombreFichero=mfilesAdapter.get(position).getName().split("_");
        String[] fechaFichero=nombreFichero[1].split("\\.");
        //Log.d("Files", "5555" + nombreFichero[0]+fechaFichero[0]);
        holder.tx_nombre_pacientes.setText( nombreFichero[0]);
        holder.tx_fecha_pacientes.setText( fechaFichero[0]);

    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mfilesAdapter.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tx_nombre_pacientes;
        TextView tx_fecha_pacientes;
        RelativeLayout paciente;

        ViewHolder(View itemView) {
            super(itemView);
            //myTextView = itemView.findViewById(R.id.tvAnimalName);
            paciente= itemView.findViewById(R.id.Paciente);
            tx_fecha_pacientes= itemView.findViewById(R.id.txt_fecha_paciente);
            tx_nombre_pacientes= itemView.findViewById(R.id.txt_nombre_paciente);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mfilesAdapter.get(id).toString();
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
