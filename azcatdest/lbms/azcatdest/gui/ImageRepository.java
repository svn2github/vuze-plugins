/*
 * Created on Nov 16, 2005
 */
package lbms.azcatdest.gui;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

/**
 * @author omschaub
 * 
 */
public class ImageRepository {

  private static HashMap<String,Image> images = new HashMap<String,Image>();
  
  public static void loadImages(Display display) {
	
    //put any images here to load on startup
    loadImage(display,"lbms/azcatdest/gui/cancel.png","cancel",255);
    loadImage(display,"lbms/azcatdest/gui/edit_add.png","add",255);
    loadImage(display,"lbms/azcatdest/gui/reload.png","reload",255);
    

    }

  
  public static Image loadImage(Display display, String res, String name){
    return loadImage(display,res,name,255);
  }
  
  public static Image loadImage(Display display, String res, String name,int alpha) {
    Image im = getImage(name);
    if(null == im) {
      InputStream is = ImageRepository.class.getClassLoader().getResourceAsStream(res);
      if(null != is) {
        if(alpha == 255) {
          im = new Image(display, is);          
        } else {
          ImageData icone = new ImageData(is);
          icone.alpha = alpha;
          im = new Image(display,icone);
        }
        images.put(name, im);
      } else {
        System.out.println("ImageRepository:loadImage:: Resource not found: " + res);
      }
    }
    return im;
  }
  
  public static void unLoadImages() {
    Iterator iter = images.values().iterator();
    while (iter.hasNext()) {
      Image im = (Image) iter.next();
      im.dispose();
    }
  }

  public static Image getImage(String name) {
    return (Image) images.get(name);
  }

}
