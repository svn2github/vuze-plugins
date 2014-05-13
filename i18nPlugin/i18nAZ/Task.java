package i18nAZ;

import java.util.concurrent.atomic.AtomicBoolean;

import org.gudy.azureus2.core3.util.AEThread2;

public class Task
{
    private int checkCount = 0;
    private Boolean stopping = false;
    private AEThread2 checkThread = null;
    final private AtomicBoolean  started = new AtomicBoolean(false);
    int time = 60;
    private iTask listener = null;
    private String name = null;

    public Task(String name, int time, iTask listener)
    {
        this.name = name;
        this.time = time;
        this.listener = listener;
    }

    public void signal()
    {
        this.checkCount++;
    }

    public void start()
    {
        synchronized (this.started)
        {
            if (this.started.get() == true)
            {
                return;
            }
            i18nAZ.log(this.name + " starting...");
            this.listener.onStart();
            this.checkThread = new AEThread2("i18nAZ." + Task.this.name + ".checkThread")
            {
                @Override
                public void run()
                {
                    i18nAZ.log(Task.this.name + " started !");
                    while (Task.this.stopping == false)
                    {
                        try
                        {
                            try
                            {
                                while (Task.this.stopping == false)
                                {
                                    Task.this.listener.check();
                                    int count = 0;
                                    while (count++ < Task.this.time && Task.this.checkCount == 0 && Task.this.stopping == false)
                                    {
                                        Thread.sleep(1000);
                                    }
                                    Task.this.checkCount = 0;
                                }
                            }
                            catch (InterruptedException e)
                            {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            };
            this.stopping = false;
            this.checkThread.start();
            this.started.set(true);
        }
    }

    public void stop()
    {
        synchronized (this.started)
        {
            if (this.started.get() == false)
            {
                return;
            }
            i18nAZ.log(this.name + " stopping...");
            StopEvent stopEvent = new StopEvent();
            this.listener.onStop(stopEvent);
            this.stopping = true;
            if (this.checkThread != null && this.checkThread.isAlive())
            {
                if(stopEvent.interupt == true)
                {
                    this.checkThread.interrupt();                    
                }
                this.checkThread.join();
                this.checkThread = null;
            }
            this.started.set(false);
            i18nAZ.log(this.name + " stopped !");
        }
    }

    public boolean isStarted()
    {        
        return this.started.get();
    }

    /*private boolean isStopping()
    {
        return this.stopping;
    }*/
}
class StopEvent
{
   boolean interupt = true;   
}
interface iTask
{
    void check();

    void onStart();

    void onStop(StopEvent e);
}
