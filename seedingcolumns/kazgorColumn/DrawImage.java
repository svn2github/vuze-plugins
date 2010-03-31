package kazgorColumn;





import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;



public class DrawImage {
    public static Image labelColor(String variable_name, int ired, int igreen, int iblue){

        //First, we need to set the sizes of the label we are going to color.. 
             
        int iWidth = 23;
        int iHeight = 23;
        
        //We first will initialize an image to draw on
        Image img = new Image(View.getDisplay(),iWidth,iHeight);
      
        //This GC is the 'drawing board' on top of the image -- it does the drawing
        GC gc = new GC(img);
      
        //Set the Foreground color as white
        gc.setForeground(View.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        
        //Draw the Background color based on the variable_name + color attributes
        gc.setBackground( new Color(View.getDisplay(),
                View.getPluginInterface().getPluginconfig().getPluginIntParameter(variable_name + "_RED", ired),
                View.getPluginInterface().getPluginconfig().getPluginIntParameter(variable_name + "_GREEN", igreen),
                View.getPluginInterface().getPluginconfig().getPluginIntParameter(variable_name + "_BLUE", iblue)));  
        
        //Fill the space with the Rectagle
        gc.fillRectangle(0,0,iWidth -1, iHeight -1);
        gc.drawRectangle(0,0, iWidth -1, iHeight -1);
       
      
      //you must ALWAYS dispose of the gc (also images, colors, pics, but not Labels, Tables, etc.)
       gc.dispose(); 
        return img;
    }
    
}
