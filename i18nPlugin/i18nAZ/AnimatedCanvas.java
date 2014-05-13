package i18nAZ;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.ui.swt.Utils;

public class AnimatedCanvas extends Canvas
{
    private Image[] images = null;
    private int currentAnimationIndex = 0;
    private TimerEventPeriodic timerEvent = null;
    private TimerEventPerformer performer = null;
    private int delay = 80;
    private String fullImageID = null;

    public AnimatedCanvas(Composite parent, int style)
    {
        super(parent, style);

        this.addPaintListener(new PaintListener()
        {
            
            public void paintControl(PaintEvent e)
            {
                if (AnimatedCanvas.this.images == null || AnimatedCanvas.this.currentAnimationIndex >= AnimatedCanvas.this.images.length)
                {
                    return;
                }
                e.gc.drawImage(AnimatedCanvas.this.images[AnimatedCanvas.this.currentAnimationIndex], AnimatedCanvas.this.images[AnimatedCanvas.this.currentAnimationIndex].getBounds().x, AnimatedCanvas.this.images[AnimatedCanvas.this.currentAnimationIndex].getBounds().y);
            }
        });
        this.addDisposeListener(new DisposeListener()
        {
            
            public void widgetDisposed(DisposeEvent e)
            {
                if (AnimatedCanvas.this.timerEvent != null)
                {
                    AnimatedCanvas.this.timerEvent.cancel();
                    AnimatedCanvas.this.timerEvent = null;
                }
            }
        });

        this.performer = new TimerEventPerformer()
        {
            private boolean exec_pending = false;
            final private Object lock = this;

            
            public void perform(TimerEvent event)
            {
                synchronized (this.lock)
                {
                    if (this.exec_pending)
                    {
                        return;
                    }
                    this.exec_pending = true;
                }
                Utils.execSWTThread(new AERunnable()
                {
                    
                    public void runSupport()
                    {
                        synchronized (lock)
                        {

                            exec_pending = false;
                        }
                        AnimatedCanvas.this.currentAnimationIndex++;
                        if (AnimatedCanvas.this.currentAnimationIndex >= AnimatedCanvas.this.images.length)
                        {
                            AnimatedCanvas.this.currentAnimationIndex = 0;
                        }
                        if (AnimatedCanvas.this.isDisposed())
                        {
                            return;
                        }
                        AnimatedCanvas.this.redraw();
                    }
                });
            }
        };

    }

    public void setImageID(String imageId)
    {
        String newFullImageID = imageId == null ? "" : imageId;
        if (newFullImageID.equals(this.fullImageID))
        {
            return;
        }
        if (this.fullImageID != null)
        {
            i18nAZ.viewInstance.getImageLoader().releaseImage(this.fullImageID);
        }
        this.images = i18nAZ.viewInstance.getImageLoader().getImages(newFullImageID);
        if (this.images == null || this.images.length == 0)
        {
            i18nAZ.viewInstance.getImageLoader().releaseImage(newFullImageID);
        }
        this.fullImageID = newFullImageID;
        Utils.execSWTThread(new AERunnable()
        {
            
            public void runSupport()
            {   
                if (AnimatedCanvas.this.images != null && AnimatedCanvas.this.images.length > 0)
                {                
                    AnimatedCanvas.this.setSize(AnimatedCanvas.this.images[0].getBounds().width, AnimatedCanvas.this.images[0].getBounds().height);
                }
                AnimatedCanvas.this.currentAnimationIndex = 0;
             }
        });
        this.updateTimerEvent();     
    }

    
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        this.updateTimerEvent();
    }

    private void updateTimerEvent()
    {
        if (this.timerEvent != null)
        {
            this.timerEvent.cancel();
            this.timerEvent = null;
        }
        if (this.getVisible() == true)
        {
            this.timerEvent = SimpleTimer.addPeriodicEvent("Animate loading", this.delay, this.performer);
        }
        Utils.execSWTThread(new AERunnable()
        {
            
            public void runSupport()
            {
                if (AnimatedCanvas.this.isDisposed() == false)
                {
                    AnimatedCanvas.this.redraw();
                }
            }
        });
    }
}
