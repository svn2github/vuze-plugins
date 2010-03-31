package com.aelitis.azbuddy.ui;

import org.eclipse.swt.custom.StyledText;

public class UILogger
{
	private StyledText	logView		= null;
	private boolean		logChanged	= true;
	private String		logDelta	= "";
	
	private static UILogger singleton = new UILogger();
	
	static UILogger getSingleton(StyledText logView)
	{
		singleton.logView = logView;
		return singleton;
	}
	
	
	public static void log(String toLog)
	{
		singleton.logSupport(toLog);
	}
	
	private void logSupport(String toLog)
	{
    	logDelta += toLog+"\n";
    	logChanged = true;
	}
	
	void refresh()
	{
		if(logChanged && logView != null)
		{
			logChanged = false;
			logView.append(logDelta);
			logDelta = "";
			logView.setSelection(logView.getText().length());
		}
	}
	
	void destroy()
	{
		logView	= null;
		logDelta = "";
	}
	
}