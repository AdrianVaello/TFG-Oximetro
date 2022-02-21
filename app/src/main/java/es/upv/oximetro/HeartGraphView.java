package es.upv.oximetro;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

// NO UTILIZADA
//TODO: Poner el dibujo de la gráfica en esta clase

public class HeartGraphView extends View {

   private int ancho, alto;

   public HeartGraphView(Context context, AttributeSet attrs) {
      super(context, attrs);

      //Inicializa la vista
      //OjO: Aún no se conocen sus dimensiones
   }

   @Override protected void onSizeChanged(int ancho, int alto,
                                          int ancho_anterior, int alto_anterior){
      this.ancho = ancho;
      this.alto = alto;
      //Te informan del ancho y la altura
   }

   @Override protected void onDraw(Canvas canvas) {
      //Dibuja aquí la vista
   }

}
