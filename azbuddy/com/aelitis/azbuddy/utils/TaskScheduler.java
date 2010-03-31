package com.aelitis.azbuddy.utils;

import java.util.Timer;
import java.util.TimerTask;


public class TaskScheduler {

	private static Timer t;

	static public void init() {	t = new Timer("BuddyScheduler",true); }
	static public void destroy() { t.cancel();t.purge();t=null; }
	static public TimedEvent timedAdd(Runnable toRun, long delay)
	{
		return new TimedEvent(toRun, delay);
	}

	static public LoopEvent loopedAdd(Runnable toRun, long interval)
	{
		return new LoopEvent(toRun, interval);
	}


	static abstract class ScheduledEvent {
		TimerTask task;
		Runnable toRun;
		
		public void stop()
		{
			if(task != null)
				task.cancel();
			task = null;
		}
	}

	public static class TimedEvent extends ScheduledEvent {
		long delay;

		TimedEvent(final Runnable toRun, long delay)
		{
			this.toRun = toRun;
			this.delay = delay;
			reset();
		}

		public void reset()
		{
			if(task != null)
				stop();
			task = new TimerTask() {
				@Override
				public void run()
				{
					toRun.run();					
				}
			};
			t.schedule(task, delay);
		}
	}

	public static class LoopEvent extends ScheduledEvent {
		long interval;

		LoopEvent(final Runnable toRun, long interval)
		{
			this.toRun = toRun;
			this.interval = interval;
			resume();
		}
		
		public void resume()
		{
			if(task != null)
				return;
			task = new TimerTask() {
				@Override
				public void run()
				{
					toRun.run();					
				}
			};
			t.schedule(task, 0L, interval);
		}
	}
}
