/*
 * Created on Sep 14, 2004
 */
package omschaub.azconfigbackup.utilities.imagerepository;

import java.io.InputStream;
import java.util.HashMap;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

/**
 * @author omschaub
 * 
 */
public class ImageRepository {

  private static HashMap images;

  static {
    images = new HashMap();
  }

  
  public static void loadImages(Display display) {
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/8x8_bullet_new.gif","bullet",255);
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/background.png","backgroundImage",255);
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/bar.png","barImage",255);
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/button_cancel.gif","delete",255);
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/download_manager.gif","manual_download",255);
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/folder_new.gif","open_folder",255);
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/folder_sent_mail.gif","create_backup",255);
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/folder_yellow.gif","folder",255);
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/gg_ignored.gif","cancel_download",255);
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/info.gif","help",255);
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/new_cvs.png","new_cvs",255);
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/new_plugin.png","azconfigbackup",255);
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/reload3.gif","manual_update",255);
	    loadImage(display,"omschaub/azconfigbackup/utilities/imagerepository/reload_page.gif","refresh",255);
    }

  
  /*public static Image loadImage(Display display, String res, String name){
    return loadImage(display,res,name,255);
  }*/
  
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
  
  public static Image getImage(String name) {
    return (Image) images.get(name);
  }

}
