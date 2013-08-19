/*
 * AMP - Azureus Monitoring Plugin
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.pmm.monitoringplugin.graphic;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.pmm.monitoringplugin.graphic.providers.HeapUsageProvider;
import org.pmm.monitoringplugin.graphic.providers.MemoryBeanProvider;
import org.pmm.monitoringplugin.graphic.providers.NonHeapUsageProvider;

import static org.eclipse.swt.SWT.NONE;
import static org.eclipse.swt.SWT.SHADOW_OUT;
import static org.eclipse.swt.SWT.VERTICAL;
import static org.eclipse.swt.layout.GridData.FILL_BOTH;
import static org.eclipse.swt.layout.GridData.FILL_HORIZONTAL;

import org.gudy.azureus2.ui.swt.plugins.*;

/**
 * 
 * @author kustos
 *
 * The graphical view of the AMP Plugin.
 * TODO add config for update time
 */
public class GraphicView implements UISWTViewEventListener {
    
    private static final String TIMER_NAME = "AMP_GRAPHIC_TIMER";
    
    private List<ChartImage> images;
    private Composite composite;
    private List<Canvas>canvae;
    
    protected PluginInterface pluginInterface;
    protected UISWTInstance swt_ui;
    
    private UTTimer timer;
    
    /**
     * Creates a new GraphicView
     * @param pluginInterface the puginInterface
     */
    public GraphicView(PluginInterface pluginInterface, UISWTInstance swt_ui) {
        this.pluginInterface = pluginInterface;
        this.swt_ui = swt_ui;
        this.startTimer();
    }
    
    /**
     * This method is called when the view is instanciated, it initializes all
     * GUI components.
     * 
     * @param parent the parent composite
     */
    private void initialize(final Composite parent) {
        this.images = new LinkedList<ChartImage>();
        this.canvae = new LinkedList<Canvas>();
        parent.setLayout(new FillLayout(VERTICAL));
        this.composite = new Composite(parent, NONE);
        this.composite.setLayout(new FillLayout(VERTICAL));
        
        
        this.createGroup(this.composite, "Heap:", new HeapUsageProvider());
        this.createGroup(this.composite, "Non-Heap:", new NonHeapUsageProvider());
    }
    
    private Composite createGroup(Composite parent, String title, MemoryBeanProvider provider) {
        Group group = new Group(parent, SHADOW_OUT);
        group.setText(title);
        group.setLayout(new GridLayout(1, true));                
        
        ChartImage image = new ChartImage(parent.getDisplay(), provider, this.pluginInterface);
        
        Composite legend = image.createLegendComposite(group);
        legend.setLayoutData(new GridData(FILL_HORIZONTAL));
        
        Canvas canvas = new Canvas(group, NONE);
        canvas.addPaintListener(new ImagePainter(image));
        canvas.setLayoutData(new GridData(FILL_BOTH));        
        
        this.images.add(image);
        this.canvae.add(canvas);
        
        return group;
    }
    
    protected void updateImages() {
        if (this.composite != null && !this.composite.isDisposed()) {
            for (ChartImage each : this.images) {
                each.refresh();
            }
            for (Canvas each : this.canvae) {
                each.redraw();
            }
        }
    }
    
    private void delete() {
        this.timer.destroy();
    }
    
    private void startTimer() { 
        this.timer = this.pluginInterface.getUtilities().createTimer(TIMER_NAME);         
        this.timer.addPeriodicEvent(this.getUpdateMillis(), new UTTimerEventPerformer()  {             
            public void  perform(UTTimerEvent ev2 )  {       
                Display display = swt_ui.getDisplay();
                if (!display.isDisposed()) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            GraphicView.this.updateImages();
                        }
                    });
                }
            }             
        });
    } 
    
    private int getUpdateMillis() {
        this.pluginInterface.getPluginconfig().getPluginIntParameter("MemoryCheckTime");
        return 1 * 1000;
    }
    
    boolean isCreated = false;
    public boolean eventOccurred(UISWTViewEvent event) {
      switch (event.getType()) {
        case UISWTViewEvent.TYPE_CREATE:
          if (isCreated) // Comment this out if you want to allow multiple views!
            return false;

          isCreated = true;
          break;

        case UISWTViewEvent.TYPE_DESTROY:
          delete(); // Remove if not defined
          isCreated = false;
          break;

        case UISWTViewEvent.TYPE_INITIALIZE:
          initialize((Composite)event.getData());
          break;

      }

      return true;
    }
    
}
