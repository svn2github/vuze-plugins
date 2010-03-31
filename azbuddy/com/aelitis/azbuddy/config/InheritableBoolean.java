package com.aelitis.azbuddy.config;

public class InheritableBoolean extends InheritableConfigNode {
	
	static final String PREFIX = "b";
	
	private boolean value;
	
	/**
	 * Use this constructor with caution, getInstance should be used to obtain monitors or avoid exceptions
	 * @throws IllegalArgumentException if the parent already contains an identical key
	 */	
	public InheritableBoolean(InheritableConfigContainer parent, String name, boolean startAsMonitor, boolean initialValue)
	{
		super(parent,name,startAsMonitor);
		value = initialValue;
	}
	
	public static InheritableBoolean getInstance(InheritableConfigContainer parent, String name, boolean startAsMonitor)
	{
		InheritableBoolean newInstance = null;
		
		// fetch existing value to preserve singleton status
		if(parent != null)
			newInstance = parent.containsValue(InheritableBoolean.class, name);
		
		// use new one otherwise
		if(newInstance == null)
			newInstance = new InheritableBoolean(parent,name,startAsMonitor,false);
		
		return newInstance;
	}

	public boolean getBoolean()
	{
		if(isMonitor())
			return ((InheritableBoolean)nearestAncestor).getBoolean();
		return value;
	}
	
	public void setBoolean(boolean setTo)
	{
		if(isMonitor())
			throw new IllegalStateException("Value "+name+" currently is in a monitoring state");
		value = setTo;
	}


	

	String getPrefix() { return PREFIX; }
	Object getContentForSerialization() { return new Integer(value?1:0); }
}
