/*
 * Created on Feb 24, 2005
 * Created by omschaub
 *
 */
package lbms.plugins.azcron.main;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorUtilities {
    private static Color BACKGROUND;
    private static Color DARK_BACKGROUND;

    public static Color getBackgroundColor(final Display display){
    	if(BACKGROUND != null) return BACKGROUND;

            if(display==null && display.isDisposed()){
                BACKGROUND = null;
                return BACKGROUND;
            }
            try{
                BACKGROUND = new Color(display ,
                        new RGB(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRed()-10,
                                display.getSystemColor(SWT.COLOR_LIST_BACKGROUND).getGreen()-10,
                                display.getSystemColor(SWT.COLOR_LIST_BACKGROUND).getBlue()-10));

            }catch(Exception e){
                BACKGROUND = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
            }



        return BACKGROUND;
    }

    public static Color getDark_BackgroundColor(final Display display){
    	if(DARK_BACKGROUND != null) return DARK_BACKGROUND;

            if(display==null && display.isDisposed()){
                DARK_BACKGROUND = null;
                return DARK_BACKGROUND;
            }
            try{
                DARK_BACKGROUND = new Color(display ,
                        new RGB(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRed()-10,
                        		display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getGreen()-10,
                        		display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getBlue()-10));
            }catch(Exception e){
                DARK_BACKGROUND = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
            }




        return DARK_BACKGROUND;
    }

    public static void unloadColors(){
        if (BACKGROUND != null && !BACKGROUND.isDisposed())
            BACKGROUND.dispose();
        if (DARK_BACKGROUND != null && !DARK_BACKGROUND.isDisposed())
            DARK_BACKGROUND.dispose();
    }
}
