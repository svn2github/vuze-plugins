package com.aelitis.azbuddy.utils;

import java.util.LinkedList;

import org.gudy.azureus2.core3.util.AEThread;

public class AsyncTaskRunner {
	
	private static final LinkedList<Runnable> toDo = new LinkedList<Runnable>();	
	private static final AEThread runner; 
	
	static {
		runner = new AEThread("BuddyPlugin:AsyncRunner",true) {
			public void runSupport()
			{
				while(true)
				{
					synchronized (toDo)
					{
						while(toDo.size() > 0)
							toDo.poll().run();
					}
					
					synchronized (runner)
					{
						try
						{
							runner.wait();
						} catch (InterruptedException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		};
		runner.start();
		
	}
	
	public static void addTask(Runnable toAdd)
	{
		synchronized (toDo)
		{
			toDo.add(toAdd);
		}
		
		synchronized (runner)
		{
			runner.notify();
		}
	}
}
