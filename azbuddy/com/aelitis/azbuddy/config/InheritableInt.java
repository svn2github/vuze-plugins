package com.aelitis.azbuddy.config;

public class InheritableInt extends InheritableConfigNode {
	
	static final String PREFIX = "i";
	
	private int value;
		
	/**
	 * Use this constructor with caution, getInstance should be used to obtain monitors or avoid exceptions
	 * @throws IllegalArgumentException if the parent already contains an identical key
	 */	
	public InheritableInt(InheritableConfigContainer parent, String name, boolean startAsMonitor, int initialValue)
	{
		super(parent,name,startAsMonitor);
		value = initialValue;
	}
	
	public static InheritableInt getInstance(InheritableConfigContainer parent, String name, boolean startAsMonitor)
	{
		InheritableInt newInstance = null;
		
		// fetch existing value to preserve singleton status
		if(parent != null)
			newInstance = parent.containsValue(InheritableInt.class, name);
		
		// use new one otherwise
		if(newInstance == null)
			newInstance = new InheritableInt(parent,name,startAsMonitor,0);
		
		return newInstance;
	}

	public int getInt()
	{
		if(isMonitor())
			return ((InheritableInt)nearestAncestor).getInt();
		return value;
	}
	
	public void setInt(int setTo)
	{
		if(isMonitor())
			throw new IllegalStateException("Value "+name+" currently is in a monitoring state");
		value = setTo;
	}

	

	String getPrefix() { return PREFIX; }
	Object getContentForSerialization() { return new Integer(value); }

	
}
